package com.dialy.app.core.sync

/**
 * Result wrapper for synchronization operations.
 */
sealed interface SyncResult<out T> {
    data class Success<T>(val data: T, val message: String? = null) : SyncResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult<Nothing>
    data class Conflict(val localData: Any?, val remoteData: Any?, val message: String) : SyncResult<Nothing>
    data object Offline : SyncResult<Nothing>
    data object NotAuthenticated : SyncResult<Nothing>
}
