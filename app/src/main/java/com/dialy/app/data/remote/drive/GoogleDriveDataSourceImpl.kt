package com.dialy.app.data.remote.drive

import android.content.Context
import android.util.Log
import com.dialy.app.core.util.DispatcherProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val TAG = "GoogleDriveDataSource"

/**
 * Production implementation of [DriveDataSource] connecting to Google Drive REST API v3.
 * Stores and manages files in the user's private Google Drive application folder (appDataFolder).
 */
class GoogleDriveDataSourceImpl(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) : DriveDataSource {

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: run {
            Log.w(TAG, "getDriveService: No signed-in Google account found on device.")
            return null
        }
        val googleAccount = account.account ?: run {
            Log.w(TAG, "getDriveService: GoogleSignInAccount account is null.")
            return null
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = googleAccount

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Dialy")
            .build()
    }

    override suspend fun uploadFile(fileName: String, content: String): Result<String> = withContext(dispatchers.io) {
        try {
            val drive = getDriveService()
                ?: return@withContext Result.failure(IllegalStateException("Google account not connected"))

            // 1. Search for existing file with fileName in appDataFolder
            val listQuery = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id, name)")
                .execute()

            val existingFile = listQuery.files?.firstOrNull()
            val contentStream = ByteArrayContent.fromString("application/json", content)

            val fileId = if (existingFile != null) {
                // Update existing file
                Log.d(TAG, "Updating existing cloud file '$fileName' (${existingFile.id}) in appDataFolder")
                val updated = drive.files().update(existingFile.id, null, contentStream)
                    .setFields("id, name, modifiedTime")
                    .execute()
                updated.id
            } else {
                // Create new file in appDataFolder
                Log.d(TAG, "Creating new cloud file '$fileName' in appDataFolder")
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
                val created = drive.files().create(fileMetadata, contentStream)
                    .setFields("id, name, modifiedTime")
                    .execute()
                created.id
            }

            Log.d(TAG, "Upload succeeded for '$fileName' (Drive fileId: $fileId)")
            Result.success(fileId)
        } catch (e: UserRecoverableAuthIOException) {
            Log.e(TAG, "UserRecoverableAuthIOException during upload: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Drive uploadFile failed for '$fileName': ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(fileName: String): Result<String?> = withContext(dispatchers.io) {
        try {
            val drive = getDriveService()
                ?: return@withContext Result.failure(IllegalStateException("Google account not connected"))

            val listQuery = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id, name)")
                .execute()

            val existingFile = listQuery.files?.firstOrNull()
            if (existingFile == null) {
                Log.d(TAG, "Cloud file '$fileName' not found in appDataFolder")
                return@withContext Result.success(null)
            }

            val outputStream = ByteArrayOutputStream()
            drive.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
            val fileContent = outputStream.toString("UTF-8")
            Log.d(TAG, "Downloaded '$fileName' (${fileContent.length} chars) from appDataFolder")
            Result.success(fileContent)
        } catch (e: Exception) {
            Log.e(TAG, "Drive downloadFile failed for '$fileName': ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun listFiles(): Result<List<RemoteDriveFile>> = withContext(dispatchers.io) {
        try {
            val drive = getDriveService()
                ?: return@withContext Result.failure(IllegalStateException("Google account not connected"))

            val list = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            val remoteFiles = (list.files ?: emptyList()).map { file ->
                RemoteDriveFile(
                    id = file.id,
                    name = file.name,
                    modifiedTime = file.modifiedTime?.value ?: System.currentTimeMillis()
                )
            }
            Log.d(TAG, "listFiles in appDataFolder found ${remoteFiles.size} files")
            Result.success(remoteFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Drive listFiles failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(fileName: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            val drive = getDriveService()
                ?: return@withContext Result.failure(IllegalStateException("Google account not connected"))

            val list = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id)")
                .execute()

            val existing = list.files?.firstOrNull()
            if (existing != null) {
                drive.files().delete(existing.id).execute()
                Log.d(TAG, "Deleted '$fileName' (${existing.id}) from appDataFolder")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Drive deleteFile failed for '$fileName': ${e.message}", e)
            Result.failure(e)
        }
    }
}
