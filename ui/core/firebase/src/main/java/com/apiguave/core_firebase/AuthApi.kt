package com.apiguave.core_firebase

import com.apiguave.core_firebase.exception.AuthException
//import com.apiguave.core_firebase.extensions.getTaskResult
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.GoogleAuthProvider

/**
 * Simple in-memory authentication API.
 * Replaces Firebase authentication with email/password.
 */
object AuthApi {
    // In-memory storage for registered users
    private val registeredUsers = mutableMapOf<String, String>() // email -> password
    private var currentUserId: String? = null

    val userId: String?
        get() = currentUserId

    /**
     * Sign in with email and password.
     * @throws AuthException if credentials are invalid
     */
    suspend fun signIn(email: String, password: String) {
        val storedPassword = registeredUsers[email]
            ?: throw AuthException("Account not found. Please sign up first.")

        if (storedPassword != password) {
            throw AuthException("Invalid password")
        }

        currentUserId = email.hashCode().toString()
    }

    /**
     * Sign up with email and password.
     * @throws AuthException if account already exists
     */
    suspend fun signUp(email: String, password: String) {
        if (registeredUsers.containsKey(email)) {
            throw AuthException("Account already exists. Please sign in.")
        }

        if (password.length < 6) {
            throw AuthException("Password must be at least 6 characters")
        }

        registeredUsers[email] = password
        currentUserId = email.hashCode().toString()
    }

    fun signOut(){
        currentUserId = null
    }

    suspend fun isNewAccount(email: String): Boolean {
        return !registeredUsers.containsKey(email)
    }

    // Legacy Firebase methods - COMMENTED OUT
    /*
    val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential).getTaskResult()
    }

    fun signOut(){
        FirebaseAuth.getInstance().signOut()
    }

    suspend fun isNewAccount(email: String): Boolean {
        val methods = FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email).getTaskResult()
        val signInMethods = methods.signInMethods ?: throw AuthException("No sign in methods found")
        return signInMethods.isEmpty()
    }
    */
}
