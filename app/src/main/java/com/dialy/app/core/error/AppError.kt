package com.dialy.app.core.error

/**
 * Application-wide error types covering database, auth, sync, validation, and network.
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class DatabaseError(override val message: String, override val cause: Throwable? = null) :
        AppError(message, cause)

    data class NotFoundError(override val message: String) :
        AppError(message)

    data class ValidationError(override val message: String) :
        AppError(message)

    data class AuthError(override val message: String, override val cause: Throwable? = null) :
        AppError(message, cause)

    data class DriveError(override val message: String, override val cause: Throwable? = null) :
        AppError(message, cause)

    data class NetworkError(override val message: String, override val cause: Throwable? = null) :
        AppError(message, cause)

    data class SyncError(override val message: String, override val cause: Throwable? = null) :
        AppError(message, cause)

    data class UnknownError(override val message: String, override val cause: Throwable? = null) :
        AppError(message, cause)
}
