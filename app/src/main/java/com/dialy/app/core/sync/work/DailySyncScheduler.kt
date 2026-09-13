package com.dialy.app.core.sync.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val TAG = "DailySyncScheduler"
private const val UNIQUE_WORK_NAME = "Nightly11PMSyncAndPurgeWork"

/**
 * Schedules the recurring background job every night at 11:00 PM IST (23:00 Asia/Kolkata).
 */
object DailySyncScheduler {

    /**
     * Enqueues unique periodic work for 11:00 PM IST daily sync and 7-day retention purge.
     */
    fun scheduleNightlySync(context: Context) {
        val initialDelay = calculateInitialDelayTo11PmIst()
        Log.d(TAG, "Scheduling 11:00 PM IST nightly sync. Initial delay: ${initialDelay.toMinutes()} minutes")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<DailySyncWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Calculates the duration until the next 11:00 PM in Indian Standard Time (Asia/Kolkata).
     */
    fun calculateInitialDelayTo11PmIst(): Duration {
        val istZone = ZoneId.of("Asia/Kolkata")
        val nowIst = LocalDateTime.now(istZone)
        val targetTimeIst = LocalTime.of(23, 0) // 11:00 PM IST

        var nextTarget = nowIst.with(targetTimeIst)
        if (nowIst.isAfter(nextTarget)) {
            // If already past 11:00 PM today, target 11:00 PM tomorrow
            nextTarget = nextTarget.plusDays(1)
        }

        val duration = Duration.between(nowIst, nextTarget)
        return if (duration.isNegative) Duration.ZERO else duration
    }
}