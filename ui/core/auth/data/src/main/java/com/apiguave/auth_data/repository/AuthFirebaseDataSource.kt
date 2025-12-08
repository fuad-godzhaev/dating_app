package com.apiguave.auth_data.repository

import com.apiguave.auth_domain.model.Account
import com.apiguave.core_firebase.AuthApi

/**
 * Authentication data source.
 * Now uses email/password instead of Firebase/Google Sign-In.
 */
class AuthFirebaseDataSource {
    val userId: String?
        get() = AuthApi.userId

    suspend fun isNewAccount(email: String): Boolean = AuthApi.isNewAccount(email)

    fun signOut() {
        AuthApi.signOut()
    }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String) {
        AuthApi.signIn(email, password)
    }

    /**
     * Sign up with email and password.
     */
    suspend fun signUp(email: String, password: String) {
        AuthApi.signUp(email, password)
    }

    // Legacy Google Sign-In - COMMENTED OUT
    /*
    val userId: String?
        get() = null // Firebase disabled

    suspend fun isNewAccount(account: Account): Boolean = false

    fun signOut() {
        // FirebaseAuth sign-out disabled
    }

    suspend fun signInWithGoogle(account: Account) {
        // FirebaseAuth sign-in disabled
    }
    */
}
