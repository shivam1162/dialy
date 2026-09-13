package com.dialy.app

import android.app.Application

/**
 * Main application class for Smart Diary.
 * Initializes application-level dependencies, database, and background services.
 */
class DialyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DialyApp
            private set
    }
}
