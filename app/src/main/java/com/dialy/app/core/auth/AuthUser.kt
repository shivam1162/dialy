package com.dialy.app.core.auth

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Domain representation of an authenticated user.
 */
@Immutable
@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String? = null,
    val serverAuthCode: String? = null
)
