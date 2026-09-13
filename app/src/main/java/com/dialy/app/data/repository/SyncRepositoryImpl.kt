package com.dialy.app.data.repository

import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.sync.SyncResult
import com.dialy.app.core.sync.SyncState
import com.dialy.app.core.util.DateUtils
import com.dialy.app.core.util.DispatcherProvider
import com.dialy.app.data.local.dao.DailyPlannerDao
import com.dialy.app.data.remote.drive.DriveDataSource
import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.repository.AuthRepository
import com.dialy.app.domain.repository.PlannerRepository
import com.dialy.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of SyncRepository for user-owned cloud synchronization via Google Drive.
 */
class SyncRepositoryImpl(
    private val plannerRepository: PlannerRepository,
    private val driveDataSource: DriveDataSource,
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider,
    private val plannerDao: DailyPlannerDao? = null
) : SyncRepository {

    // Secondary constructor for compatibility with existing tests and callers
    constructor(
        plannerRepository: PlannerRepository,
        plannerDao: DailyPlannerDao,
        driveDataSource: DriveDataSource,
        authRepository: AuthRepository,
        dispatchers: DispatcherProvider
    ) : this(plannerRepository, driveDataSource, authRepository, dispatchers, plannerDao)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.LOCAL_ONLY)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override suspend fun syncPlanner(date: String): SyncResult<DailyPlanner> = withContext(dispatchers.io) {
        val auth = authRepository.checkAuthStatus()
        if (auth !is AuthState.Authenticated) {
            return@withContext SyncResult.NotAuthenticated
        }

        _syncState.value = SyncState.SYNC_PENDING

        try {
            val localPlanner = plannerRepository.getPlanner(date)
            val fileName = "planner_$date.json"
            val remoteContentResult = driveDataSource.downloadFile(fileName)

            val remotePlanner = remoteContentResult.getOrNull()?.let { content ->
                try {
                    json.decodeFromString<DailyPlanner>(content)
                } catch (e: Exception) {
                    null
                }
            }

            val resolvedPlanner: DailyPlanner = when {
                localPlanner == null && remotePlanner == null -> {
                    _syncState.value = SyncState.SYNCED
                    return@withContext SyncResult.Error("No planner exists locally or remotely for $date")
                }
                localPlanner != null && remotePlanner == null -> {
                    // Upload local to remote
                    val payload = json.encodeToString(localPlanner.copy(syncState = SyncState.SYNCED))
                    driveDataSource.uploadFile(fileName, payload)
                    val synced = localPlanner.copy(
                        syncState = SyncState.SYNCED,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    plannerRepository.updateSyncState(date, SyncState.SYNCED.name, synced.lastSyncedAt)
                    synced
                }
                localPlanner == null && remotePlanner != null -> {
                    // Save remote to local
                    val synced = remotePlanner.copy(
                        syncState = SyncState.SYNCED,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    plannerRepository.savePlanner(synced)
                    synced
                }
                else -> {
                    // Both exist: Deterministic conflict resolution based on latest updatedAt
                    val local = localPlanner!!
                    val remote = remotePlanner!!

                    if (remote.updatedAt > local.updatedAt) {
                        // Remote is newer: accept remote
                        val synced = remote.copy(
                            syncState = SyncState.SYNCED,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        plannerRepository.savePlanner(synced)
                        synced
                    } else {
                        // Local is newer or equal: upload local to remote
                        val payload = json.encodeToString(local.copy(syncState = SyncState.SYNCED))
                        driveDataSource.uploadFile(fileName, payload)
                        val synced = local.copy(
                            syncState = SyncState.SYNCED,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        plannerRepository.updateSyncState(date, SyncState.SYNCED.name, synced.lastSyncedAt)
                        synced
                    }
                }
            }

            _syncState.value = SyncState.SYNCED
            SyncResult.Success(resolvedPlanner, "Planner for $date synchronized successfully.")
        } catch (e: Exception) {
            _syncState.value = SyncState.SYNC_ERROR
            SyncResult.Error("Failed to sync planner for $date: ${e.message}", e)
        }
    }

    override suspend fun syncAll(): SyncResult<List<DailyPlanner>> = withContext(dispatchers.io) {
        val auth = authRepository.checkAuthStatus()
        if (auth !is AuthState.Authenticated) {
            return@withContext SyncResult.NotAuthenticated
        }

        _syncState.value = SyncState.SYNC_PENDING

        try {
            val localPlanners = plannerRepository.getAllPlannersOnce()
            val syncedPlanners = mutableListOf<DailyPlanner>()

            for (planner in localPlanners) {
                val syncRes = syncPlanner(planner.date)
                if (syncRes is SyncResult.Success) {
                    syncedPlanners.add(syncRes.data)
                }
            }

            _syncState.value = SyncState.SYNCED
            SyncResult.Success(syncedPlanners, "All planners synchronized successfully.")
        } catch (e: Exception) {
            _syncState.value = SyncState.SYNC_ERROR
            SyncResult.Error("Full synchronization failed: ${e.message}", e)
        }
    }

    override suspend fun backupToCloud(): SyncResult<Unit> = withContext(dispatchers.io) {
        val auth = authRepository.checkAuthStatus()
        if (auth !is AuthState.Authenticated) {
            return@withContext SyncResult.NotAuthenticated
        }

        try {
            val allPlanners = plannerRepository.getAllPlannersOnce()
            val backupJson = json.encodeToString(allPlanners)

            val uploadResult = driveDataSource.uploadFile("smart_diary_full_backup.json", backupJson)
            uploadResult.fold(
                onSuccess = {
                    SyncResult.Success(Unit, "Full backup uploaded to Google Drive.")
                },
                onFailure = { error ->
                    val cleanMsg = cleanErrorMessage(error)
                    SyncResult.Error("Cloud backup failed: $cleanMsg", error)
                }
            )
        } catch (e: Exception) {
            val cleanMsg = cleanErrorMessage(e)
            SyncResult.Error("Cloud backup failed: $cleanMsg", e)
        }
    }

    override suspend fun restoreFromCloud(): SyncResult<List<DailyPlanner>> = withContext(dispatchers.io) {
        val auth = authRepository.checkAuthStatus()
        if (auth !is AuthState.Authenticated) {
            return@withContext SyncResult.NotAuthenticated
        }

        try {
            val backupResult = driveDataSource.downloadFile("smart_diary_full_backup.json")
            if (backupResult.isFailure) {
                val error = backupResult.exceptionOrNull()
                val cleanMsg = cleanErrorMessage(error)
                return@withContext SyncResult.Error("Cloud restore failed: $cleanMsg", error)
            }

            val content = backupResult.getOrNull()
                ?: return@withContext SyncResult.Error("No cloud backup found on Google Drive.")

            val planners = json.decodeFromString<List<DailyPlanner>>(content)

            // 1. Wipe all local data for this account before restoring
            plannerRepository.clearAllLocalData()

            // 2. Insert all planners from Google Drive backup
            for (planner in planners) {
                plannerRepository.savePlanner(planner.copy(syncState = SyncState.SYNCED))
            }

            SyncResult.Success(planners, "Restored ${planners.size} planners from Google Drive.")
        } catch (e: Exception) {
            val cleanMsg = cleanErrorMessage(e)
            SyncResult.Error("Restore from cloud failed: $cleanMsg", e)
        }
    }

    override suspend fun getLastBackupMetadata(): com.dialy.app.domain.repository.BackupMetadata? = withContext(dispatchers.io) {
        val auth = authRepository.checkAuthStatus()
        if (auth !is AuthState.Authenticated) return@withContext null
        try {
            val filesResult = driveDataSource.listFiles()
            val files = filesResult.getOrNull() ?: return@withContext null
            val backupFile = files.find { it.name == "smart_diary_full_backup.json" } ?: return@withContext null
            val content = driveDataSource.downloadFile("smart_diary_full_backup.json").getOrNull()
            val count = if (content != null) {
                try { json.decodeFromString<List<DailyPlanner>>(content).size } catch (e: Exception) { 0 }
            } else 0
            com.dialy.app.domain.repository.BackupMetadata(
                lastBackupTime = backupFile.modifiedTime,
                plannerCount = count
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanErrorMessage(e: Throwable?): String {
        val raw = e?.message ?: "Unknown error"
        return when {
            raw.contains("SERVICE_DISABLED", ignoreCase = true) ->
                "Google Drive API is disabled in your Google Cloud Console project. Enable it at: console.developers.google.com/apis/api/drive.googleapis.com"
            raw.contains("ACCESS_TOKEN_SCOPE_INSUFFICIENT", ignoreCase = true) ->
                "Drive permission not granted. Please sign out and sign in again to grant Drive access."
            raw.contains("UserRecoverableAuthIOException", ignoreCase = true) ->
                "Drive authorization required. Please sign in again."
            else -> raw
        }
    }

    override suspend fun purgeOldLocalData(retentionDays: Int): Result<Int> = withContext(dispatchers.io) {
        // Permanent retention policy: local data is kept permanently on device and never purged
        Result.success(0)
    }
}
