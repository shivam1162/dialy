package com.dialy.app.data.remote.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.dialy.app.core.auth.AuthUser
import com.dialy.app.core.error.AppError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "GoogleAuthManager"

/**
 * Encapsulates Google Sign-In and Google Drive permission authorization.
 */
class GoogleAuthManager(
    private val context: Context
) {
    private val webClientId: String = try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "209256810606-9tmkauf1lotds835iog7ooq0jgt240dt.apps.googleusercontent.com"
    } catch (e: Exception) {
        "209256810606-9tmkauf1lotds835iog7ooq0jgt240dt.apps.googleusercontent.com"
    }

    // Standard Google Sign-In with Firebase Web Client ID
    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .requestProfile()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    val signInIntent: Intent
        get() = googleSignInClient.signInIntent

    /**
     * Gets the currently signed in account silently if available.
     */
    fun getLastSignedInUser(): AuthUser? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return mapToAuthUser(account)
    }

    /**
     * Checks if the user has granted Google Drive AppData permissions.
     */
    fun hasDrivePermission(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
    }

    /**
     * Requests Drive permissions if not already granted.
     */
    fun requestDrivePermissions(activity: Activity, requestCode: Int) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return
        GoogleSignIn.requestPermissions(
            activity,
            requestCode,
            account,
            Scope(DriveScopes.DRIVE_APPDATA)
        )
    }

    /**
     * Extracts AuthUser from completed Google Sign-In Intent result.
     */
    fun handleSignInResult(data: Intent?): Result<AuthUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d(TAG, "Google Sign-In success: ${account.email}")
                Result.success(mapToAuthUser(account))
            } else {
                Result.failure(AppError.AuthError("No Google account returned from intent."))
            }
        } catch (e: ApiException) {
            val statusMessage = CommonStatusCodes.getStatusCodeString(e.statusCode)
            Log.e(TAG, "Google Sign-In ApiException: code=${e.statusCode} ($statusMessage)", e)

            val userFriendlyMsg = when (e.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR ->
                    "Google Cloud OAuth client is not registered for debug SHA-1 (Code 10)."
                CommonStatusCodes.SIGN_IN_REQUIRED ->
                    "Sign-in cancelled or required."
                CommonStatusCodes.NETWORK_ERROR ->
                    "Network error connecting to Google Play Services."
                else ->
                    "Sign-In error (${e.statusCode}: $statusMessage)"
            }
            Result.failure(AppError.AuthError(userFriendlyMsg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-In exception: ${e.message}", e)
            Result.failure(AppError.AuthError("Sign-In failed: ${e.message}", e))
        }
    }

    /**
     * Signs out the current Google account and clears cached Google Sign-In state
     * so that the next sign-in prompts the user to select an account.
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            suspendCancellableCoroutine { continuation ->
                googleSignInClient.signOut()
                    .addOnCompleteListener {
                        try {
                            googleSignInClient.revokeAccess()
                        } catch (e: Exception) {
                            // ignore revoke failure
                        }
                        continuation.resume(Unit)
                    }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.AuthError("Sign-out failed: ${e.message}", e))
        }
    }

    private fun mapToAuthUser(account: GoogleSignInAccount): AuthUser {
        return AuthUser(
            id = account.id ?: account.email ?: "google_user",
            email = account.email ?: "user@google.com",
            displayName = account.displayName ?: account.givenName ?: "Diary User",
            photoUrl = account.photoUrl?.toString(),
            serverAuthCode = account.serverAuthCode
        )
    }
}
