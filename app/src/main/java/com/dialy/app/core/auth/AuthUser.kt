package com.dialy.app.core.auth

import kotlinx.serialization.Serializable

/**
 * Domain representation of an authenticated user.
 */
@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String? = null,
    val serverAuthCode: String? = null
)
