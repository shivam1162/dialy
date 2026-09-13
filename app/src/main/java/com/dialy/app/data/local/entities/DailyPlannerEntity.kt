package com.dialy.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dialy.app.core.sync.SyncState

/**
 * Room entity representing the core daily planner entry.
 */
@Entity(tableName = "daily_planners")
data class DailyPlannerEntity(
    @PrimaryKey
    val date: String, // ISO "yyyy-MM-dd"
    val focus: String = "",
    val notes: String = "",
    val dailyReminder: String = "",
    val moodJson: String? = null,
    val reflectionJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val localVersion: Long = 1L,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val lastSyncedAt: Long? = null
)
