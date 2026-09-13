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
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "DailySyncScheduler"
private const val UNIQUE_WORK_NAME = "DailyDriveAutoBackupWork"
private const val PREFS_NAME = "diary_auto_backup_prefs"
private const val KEY_HOUR = "backup_hour"
private const val KEY_MINUTE = "backup_minute"

/**
 * Manages scheduling and timing for Google Drive automated daily backups.
 */
object DailySyncScheduler {

    const val DEFAULT_HOUR = 23
    const val DEFAULT_MINUTE = 0

    fun getScheduledHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HOUR, DEFAULT_HOUR)
    }

    fun getScheduledMinute(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE)
    }

    fun getFormattedScheduledTime(context: Context): String {
        val hour = getScheduledHour(context)
        val minute = getScheduledMinute(context)
        val time = LocalTime.of(hour, minute)
        return time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
    }

    /**
     * Updates user's preferred backup timing and reschedules background WorkManager accordingly.
     */
    fun scheduleDailySync(
        context: Context,
        hour: Int = getScheduledHour(context),
        minute: Int = getScheduledMinute(context)
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()

        val workManager = WorkManager.getInstance(context)

        val initialDelay = calculateInitialDelay(hour, minute)
        Log.d(TAG, "Rescheduling daily backup for $hour:${minute.toString().padStart(2, '0')}. Delay: ${initialDelay.toMinutes()} minutes")

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

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    /**
     * Initial startup scheduler call.
     */
    fun scheduleNightlySync(context: Context) {
        scheduleDailySync(context)
    }

    /**
     * Calculates the duration from now until the target hour and minute in local system time.
     */
    fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Duration {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val targetTime = LocalTime.of(targetHour, targetMinute)

        var nextTarget = now.with(targetTime)
        if (now.isAfter(nextTarget)) {
            // Already passed today, schedule for tomorrow
            nextTarget = nextTarget.plusDays(1)
        }

        val duration = Duration.between(now, nextTarget)
        return if (duration.isNegative) Duration.ZERO else duration
    }
}