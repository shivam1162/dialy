package com.dialy.app.core.sync

import kotlinx.serialization.Serializable

/**
 * Represents synchronization state for local and cloud data.
 */
@Serializable
enum class SyncState {
    LOCAL_ONLY,
    SYNC_PENDING,
    SYNCED,
    SYNC_ERROR
}
