package com.dialy.app.data.remote.auth

import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.auth.AuthUser
import com.dialy.app.core.error.AppError
import com.dialy.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.dialy.app.core.util.DefaultDispatcherProvider
import com.dialy.app.core.util.DispatcherProvider

/**
 * Implementation of AuthRepository managing user authentication state.
 */
class AuthRepositoryImpl(
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val currentUser: AuthUser?
        get() = (authState.value as? AuthState.Authenticated)?.user

    override suspend fun checkAuthStatus(): AuthState {
        return _authState.value
    }

    override suspend fun signIn(authUser: AuthUser): Result<AuthUser> {
        return try {
            _authState.value = AuthState.Authenticated(authUser)
            Result.success(authUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Sign in failed", e)
            Result.failure(AppError.AuthError("Sign in failed", e))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.AuthError("Sign out failed", e))
        }
    }

    override fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }
}
