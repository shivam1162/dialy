package com.dialy.app.data.local.database

import androidx.room.TypeConverter
import com.dialy.app.core.sync.SyncState

/**
 * Type converters for Room Database.
 */
class Converters {

    @TypeConverter
    fun fromSyncState(value: SyncState): String {
        return value.name
    }

    @TypeConverter
    fun toSyncState(value: String): SyncState {
        return try {
            SyncState.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SyncState.LOCAL_ONLY
        }
    }
}
