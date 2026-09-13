package com.dialy.app

import android.app.Application
import com.dialy.app.core.sync.work.DailySyncScheduler

/**
 * Main application class for Dialy.
 * Initializes application-level dependencies, database, and background WorkManager services.
 */
class DialyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Schedule 11:00 PM IST nightly Google Drive sync and 7-day retention purge
        DailySyncScheduler.scheduleNightlySync(this)
    }

    companion object {
        lateinit var instance: DialyApp
            private set
    }
}
