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
    private val plannerDao: DailyPlannerDao,
    private val driveDataSource: DriveDataSource,
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider
) : SyncRepository {

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
                    plannerDao.updateSyncState(date, SyncState.SYNCED.name, synced.lastSyncedAt)
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
                        plannerDao.updateSyncState(date, SyncState.SYNCED.name, synced.lastSyncedAt)
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
            val localEntities = plannerDao.getAllPlannersOnce()
            val syncedPlanners = mutableListOf<DailyPlanner>()

            for (entity in localEntities) {
                val syncRes = syncPlanner(entity.date)
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
            val entities = plannerDao.getAllPlannersOnce()
            val allPlanners = entities.mapNotNull { plannerRepository.getPlanner(it.date) }
            val backupJson = json.encodeToString(allPlanners)

            driveDataSource.uploadFile("smart_diary_full_backup.json", backupJson)
            SyncResult.Success(Unit, "Full backup uploaded to Google Drive.")
        } catch (e: Exception) {
            SyncResult.Error("Cloud backup failed: ${e.message}", e)
        }
    }

    override suspend fun restoreFromCloud(): SyncResult<List<DailyPlanner>> = withContext(dispatchers.io) {
        val auth = authRepository.checkAuthStatus()
        if (auth !is AuthState.Authenticated) {
            return@withContext SyncResult.NotAuthenticated
        }

        try {
            val backupResult = driveDataSource.downloadFile("smart_diary_full_backup.json")
            val content = backupResult.getOrNull()
                ?: return@withContext SyncResult.Error("No cloud backup found on Google Drive.")

            val planners = json.decodeFromString<List<DailyPlanner>>(content)
            for (planner in planners) {
                plannerRepository.savePlanner(planner.copy(syncState = SyncState.SYNCED))
            }

            SyncResult.Success(planners, "Restored ${planners.size} planners from Google Drive.")
        } catch (e: Exception) {
            SyncResult.Error("Restore from cloud failed: ${e.message}", e)
        }
    }

    override suspend fun purgeOldLocalData(retentionDays: Int): Result<Int> = withContext(dispatchers.io) {
        try {
            val istZone = java.time.ZoneId.of("Asia/Kolkata")
            val today = java.time.LocalDate.now(istZone)
            val cutoffDate = today.minusDays(retentionDays.toLong())
            val cutoffDateStr = DateUtils.toIsoString(cutoffDate)

            // 1. Get all local planners older than the retention window
            val allPlanners = plannerDao.getAllPlannersOnce()
            val oldPlanners = allPlanners.filter { it.date <= cutoffDateStr }

            var purgedCount = 0
            for (oldPlanner in oldPlanners) {
                val date = oldPlanner.date
                val fullPlanner = plannerRepository.getPlanner(date) ?: continue

                // 2. Verify that this planner exists in Google Drive
                val fileName = "planner_$date.json"
                val remoteCheck = driveDataSource.downloadFile(fileName)
                val existsOnDrive = remoteCheck.getOrNull() != null

                if (!existsOnDrive) {
                    // Upload to Drive first before purging
                    val payload = json.encodeToString(fullPlanner.copy(syncState = SyncState.SYNCED))
                    driveDataSource.uploadFile(fileName, payload)
                }

                // 3. Purge from local Room database
                plannerRepository.deletePlanner(date)
                purgedCount++
            }

            Result.success(purgedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
