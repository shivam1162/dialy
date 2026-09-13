package com.dialy.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.dialy.app.core.sync.work.DailySyncScheduler

/**
 * Main application class for Dialy.
 * Initializes application-level dependencies, database, WorkManager, and high-performance image caching.
 */
class DialyApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Schedule 11:00 PM IST nightly Google Drive sync and 7-day retention purge
        DailySyncScheduler.scheduleNightlySync(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available app memory for instant avatar caching
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("dialy_image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .allowHardware(true)
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: DialyApp
            private set
    }
}
