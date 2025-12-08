package com.apiguave.auth_domain.model

/**
 * Account model for email/password authentication.
 * Changed from idToken to password for email/password auth.
 */
data class Account(
    val email: String,
    val password: String,
    @Deprecated("Use password instead. Kept for backwards compatibility.")
    val idToken: String = password // Default to password for compatibility
)

// Legacy Google Sign-In version - COMMENTED OUT
/*
data class Account(val email: String, val idToken: String)
*/
