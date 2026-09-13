package com.dialy.app.domain.repository

import com.dialy.app.core.sync.SyncResult
import com.dialy.app.core.sync.SyncState
import com.dialy.app.domain.model.DailyPlanner
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for Google Drive cloud synchronization.
 */
interface SyncRepository {
    val syncState: StateFlow<SyncState>

    /**
     * Synchronizes a single daily planner entry with Google Drive.
     */
    suspend fun syncPlanner(date: String): SyncResult<DailyPlanner>

    /**
     * Full synchronization across all local and remote planners.
     */
    suspend fun syncAll(): SyncResult<List<DailyPlanner>>

    /**
     * Uploads full local backup JSON snapshot to Google Drive app-data storage.
     */
    suspend fun backupToCloud(): SyncResult<Unit>

    /**
     * Restores all planners from Google Drive backup snapshot into local database.
     */
    suspend fun restoreFromCloud(): SyncResult<List<DailyPlanner>>

    /**
     * Nightly retention clean-up: Ensures data older than retentionDays is backed up in Google Drive,
     * then purges local records so that only the rolling retention window lives on device.
     */
    suspend fun purgeOldLocalData(retentionDays: Int = 7): Result<Int>
}
