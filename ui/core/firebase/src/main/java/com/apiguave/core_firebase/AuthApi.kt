package com.apiguave.core_firebase

import com.apiguave.core_firebase.exception.AuthException
import com.fypapp.lexicons.repo.AtProtoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
//import com.apiguave.core_firebase.extensions.getTaskResult
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.GoogleAuthProvider

/**
 * AT Protocol authentication API.
 * Handles account creation and session management with PDS.
 */
object AuthApi {
    private const val PDS_URL = "http://localhost:3000"

    private var currentUserId: String? = null
    private var currentHandle: String? = null
    private var accessToken: String? = null

    val userId: String?
        get() = currentUserId

    /**
     * Sign in with email and password.
     * Creates a session with the PDS and stores the access token.
     */
    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        try {
            // Must match the handle format used in signUp (with .test suffix)
            val handle = email.replace("@", "-").replace(".", "-") + ".test"

            val endpoint = "$PDS_URL/xrpc/com.atproto.server.createSession"
            val payload = JSONObject().apply {
                put("identifier", handle)
                put("password", password)
            }

            android.util.Log.d("AuthApi", "Signing in as: $handle")
            val response = postRequest(endpoint, payload)
                ?: throw AuthException("Failed to create session - invalid credentials")

            currentUserId = response.getString("did")
            currentHandle = response.getString("handle")
            accessToken = response.getString("accessJwt")

            // Set the token in AtProtoClient
            if (accessToken != null) {
                (AtProtoClient.default as? com.fypapp.lexicons.repo.AtProtoClientImpl)?.setSession(accessToken!!)
                android.util.Log.d("AuthApi", "Signed in successfully - DID: $currentUserId, Token: ${accessToken!!.take(20)}...")
            } else {
                android.util.Log.e("AuthApi", "Sign in successful but no access token received")
                throw AuthException("Sign in successful but no access token received from server")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthApi", "Sign in failed", e)
            throw AuthException(e.message ?: "Sign in failed")
        }
    }

    /**
     * Sign up with email and password.
     * Creates a new account on the PDS.
     */
    suspend fun signUp(email: String, password: String) = withContext(Dispatchers.IO) {
        try {
            if (password.length < 8) {
                throw AuthException("Password must be at least 8 characters")
            }

            val handle = email.replace("@", "-").replace(".", "-") + ".test"

            val endpoint = "$PDS_URL/xrpc/com.atproto.server.createAccount"
            val payload = JSONObject().apply {
                put("handle", handle)
                put("password", password)
                put("email", email)
                // Use a default invite code for development
                // TODO: Make this configurable or remove if PDS has invites disabled
                //put("inviteCode", "dev-invite-code")
            }

            android.util.Log.d("AuthApi", "Creating account: $handle")
            val response = postRequest(endpoint, payload)
                ?: throw AuthException("Failed to create account - email may already be in use")

            currentUserId = response.getString("did")
            currentHandle = response.optString("handle", handle)
            accessToken = response.getString("accessJwt")

            // Set the token in AtProtoClient
            if (accessToken != null) {
                (AtProtoClient.default as? com.fypapp.lexicons.repo.AtProtoClientImpl)?.setSession(accessToken!!)
                android.util.Log.d("AuthApi", "Account created successfully - DID: $currentUserId, Token: ${accessToken!!.take(20)}...")
            } else {
                android.util.Log.e("AuthApi", "Account created but no access token received")
                throw AuthException("Account created but no access token received from server")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthApi", "Sign up failed", e)
            throw AuthException(e.message ?: "Sign up failed")
        }
    }

    fun signOut(){
        currentUserId = null
        currentHandle = null
        accessToken = null
        (AtProtoClient.default as? com.fypapp.lexicons.repo.AtProtoClientImpl)?.setSession("")
    }

    suspend fun isNewAccount(email: String): Boolean {
        // For now, always return true and let the server decide
        // In a real implementation, you could check if account exists
        return true
    }

    private fun postRequest(url: String, payload: JSONObject): JSONObject? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            android.util.Log.d("AuthApi", "Response code: $responseCode")

            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                JSONObject(response)
            } else {
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorBody = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    android.util.Log.e("AuthApi", "Error $responseCode: $errorBody")
                }
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthApi", "Request failed", e)
            null
        }
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
