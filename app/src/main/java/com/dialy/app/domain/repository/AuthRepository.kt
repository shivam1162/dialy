package com.dialy.app.domain.repository

import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.auth.AuthUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication abstraction hiding Google Sign-In SDK implementation details.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: AuthUser?

    suspend fun checkAuthStatus(): AuthState
    suspend fun signIn(authUser: AuthUser): Result<AuthUser>
    suspend fun signOut(): Result<Unit>
    fun setError(message: String)
}
