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
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(applicationContext)
            val profileId = account?.email ?: "guest"
            val database = AppDatabase.getInstance(applicationContext, profileId)
            val dispatchers = DefaultDispatcherProvider()
            val authRepository = AuthRepositoryImpl(dispatchers)

            val plannerRepository = PlannerRepositoryImpl(
                plannerDao = database.dailyPlannerDao(),
                todoDao = database.todoDao(),
                priorityDao = database.priorityDao(),
                scheduleDao = database.scheduleDao(),
                selfCareDao = database.selfCareDao(),
                reminderDao = database.reminderDao(),
                gratitudeDao = database.gratitudeDao(),
                dispatchers = dispatchers,
                context = applicationContext
            )
            plannerRepository.switchProfile(profileId)

            val driveDataSource = com.dialy.app.data.remote.drive.GoogleDriveDataSourceImpl(
                context = applicationContext,
                dispatchers = dispatchers
            )

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

            // Step 1: Upload full backup snapshot to Google Drive
            Log.d(TAG, "Uploading full backup snapshot to Drive...")
            syncRepository.backupToCloud()

            // Step 2: Sync today's planner and all pending days to Google Drive
            Log.d(TAG, "Syncing today ($todayStr) to Drive...")
            syncRepository.syncPlanner(todayStr)
            syncRepository.syncAll()

            Log.d(TAG, "Daily backup routine completed successfully. All local data retained permanently.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DailySyncWorker execution failed: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}