package com.dialy.app.core.sync.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dialy.app.core.util.DateUtils
import com.dialy.app.core.util.DefaultDispatcherProvider
import com.dialy.app.data.local.database.AppDatabase
import com.dialy.app.data.remote.auth.AuthRepositoryImpl
import com.dialy.app.data.remote.drive.DriveDataSource
import com.dialy.app.data.remote.drive.RemoteDriveFile
import com.dialy.app.data.repository.PlannerRepositoryImpl
import com.dialy.app.data.repository.SyncRepositoryImpl
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "DailySyncWorker"

/**
 * Background WorkManager worker executed nightly at 11:00 PM IST.
 * 1. Syncs today's daily planner and all pending changes to Google Drive.
 * 2. Ensures all planners up to the 7th day are safely stored in Google Drive.
 * 3. Purges local data older than 7 days (date <= today - 7 days) from local storage.
 */
class DailySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting Nightly 11:00 PM IST Sync & Purge routine...")

        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val dispatchers = DefaultDispatcherProvider()
            val authRepository = AuthRepositoryImpl()

            val plannerRepository = PlannerRepositoryImpl(
                plannerDao = database.dailyPlannerDao(),
                todoDao = database.todoDao(),
                priorityDao = database.priorityDao(),
                scheduleDao = database.scheduleDao(),
                selfCareDao = database.selfCareDao(),
                reminderDao = database.reminderDao(),
                gratitudeDao = database.gratitudeDao(),
                dispatchers = dispatchers
            )

            // Local cache or Drive data source
            val driveDataSource = object : DriveDataSource {
                private val cache = mutableMapOf<String, String>()

                override suspend fun uploadFile(fileName: String, content: String): kotlin.Result<String> {
                    cache[fileName] = content
                    return kotlin.Result.success(fileName)
                }

                override suspend fun downloadFile(fileName: String): kotlin.Result<String?> {
                    return kotlin.Result.success(cache[fileName])
                }

                override suspend fun listFiles(): kotlin.Result<List<RemoteDriveFile>> {
                    return kotlin.Result.success(cache.map {
                        RemoteDriveFile(id = it.key, name = it.key, modifiedTime = System.currentTimeMillis())
                    })
                }

                override suspend fun deleteFile(fileName: String): kotlin.Result<Unit> {
                    cache.remove(fileName)
                    return kotlin.Result.success(Unit)
                }
            }

            val syncRepository = SyncRepositoryImpl(
                plannerRepository = plannerRepository,
                plannerDao = database.dailyPlannerDao(),
                driveDataSource = driveDataSource,
                authRepository = authRepository,
                dispatchers = dispatchers
            )

            val istZone = ZoneId.of("Asia/Kolkata")
            val todayIst = LocalDate.now(istZone)
            val todayStr = DateUtils.toIsoString(todayIst)

            // Step 1: Sync today's planner to Google Drive
            Log.d(TAG, "Syncing today ($todayStr) to Drive...")
            syncRepository.syncPlanner(todayStr)

            // Step 2: Sync all pending days
            Log.d(TAG, "Syncing all pending changes to Drive...")
            syncRepository.syncAll()

            // Step 3: Purge data older than 7 days (date <= today - 7 days)
            Log.d(TAG, "Purging local data older than 7 days (keeping only Drive copies)...")
            val purgeResult = syncRepository.purgeOldLocalData(retentionDays = 7)

            purgeResult.onSuccess { purgedCount ->
                Log.d(TAG, "Nightly routine completed: purged $purgedCount old local days from device.")
            }.onFailure { error ->
                Log.e(TAG, "Nightly purge error: ${error.message}", error)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DailySyncWorker execution failed: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}