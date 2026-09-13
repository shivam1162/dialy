package com.dialy.app.data.remote.drive

/**
 * Metadata for a file stored in Google Drive appDataFolder.
 */
data class RemoteDriveFile(
    val id: String,
    val name: String,
    val modifiedTime: Long,
    val content: String? = null
)

/**
 * Interface abstracting Google Drive operations for testability and isolation.
 */
interface DriveDataSource {
    suspend fun uploadFile(fileName: String, content: String): Result<String>
    suspend fun downloadFile(fileName: String): Result<String?>
    suspend fun listFiles(): Result<List<RemoteDriveFile>>
    suspend fun deleteFile(fileName: String): Result<Unit>
}
