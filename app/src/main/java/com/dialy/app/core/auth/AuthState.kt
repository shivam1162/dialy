package com.dialy.app.core.auth

/**
 * State representing current user authentication status.
 */
sealed interface AuthState {
    data object Initial : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data object Unauthenticated : AuthState
    data class Error(val message: String, val cause: Throwable? = null) : AuthState
}
