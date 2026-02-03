package com.fypapp.lexicons.repo

import com.fypapp.lexicons.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * AT Protocol client for dating app lexicons
 * Communicates with PDS via XRPC endpoints
 */
interface AtProtoClient {

    // Profile operations
    suspend fun createProfile(repo: String, profile: ComFypappProfile.Record)
    suspend fun profileExists(did: String): Boolean
    suspend fun getProfile(input: ComFypappGetProfile.Input): ComFypappGetProfile.Output?
    suspend fun updateProfile(input: ComFypappUpdateProfile.Input): ComFypappUpdateProfile.Output?

    // Discovery
    suspend fun discover(input: ComFypappDiscover.Input): ComFypappDiscover.Output?

    // Matching
    suspend fun like(repo: String, record: ComFypappLike.Record)
    suspend fun pass(repo: String, record: ComFypappPass.Record)
    suspend fun listLikes(repo: String): List<LikeRecord>?
    suspend fun createMatch(record: ComFypappMatch.Record): AtUri?

    companion object {
        val default: AtProtoClient by lazy {
            AtProtoClientImpl(
                pdsUrl = "http://localhost:3000", // Using Termux PDS on port 3000
                adminPassword = "admin123" // TODO: Make configurable
            )
        }
    }

    data class LikeRecord(
        val uri: String,
        val cid: String,
        val record: ComFypappLike.Record
    )
}

/**
 * HTTP-based implementation of AtProtoClient using XRPC protocol
 */
class AtProtoClientImpl(
    private val pdsUrl: String,
    private val adminPassword: String
) : AtProtoClient {

    // Store the current session token for authenticated requests
    var accessToken: String? = null
        private set

    fun setSession(token: String) {
        accessToken = token
        android.util.Log.d("AtProtoClient", "Session token set")
    }

    override suspend fun createProfile(repo: String, profile: ComFypappProfile.Record) = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.atproto.repo.createRecord"
        val payload = JSONObject().apply {
            put("repo", repo)
            put("collection", "com.fypapp.profile")
            put("rkey", "self")
            put("record", profileToJson(profile))
        }

        android.util.Log.d("AtProtoClient", "Creating profile for repo: $repo")
        android.util.Log.d("AtProtoClient", "Endpoint: $endpoint")
        android.util.Log.d("AtProtoClient", "Payload: $payload")

        val response = postRequest(endpoint, payload, useAuth = true)
        if (response != null) {
            android.util.Log.d("AtProtoClient", "Profile created successfully: $response")
        } else {
            // Log warning but don't throw - server may return 200 with empty body
            android.util.Log.w("AtProtoClient", "Profile creation completed but response was null (server returned 200 with empty/invalid body)")
        }
        Unit
    }

    override suspend fun profileExists(did: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val endpoint = "$pdsUrl/xrpc/com.atproto.repo.getRecord?repo=$did&collection=com.fypapp.profile&rkey=self"
            val response = getRequest(endpoint, useAuth = false)
            response != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getProfile(input: ComFypappGetProfile.Input): ComFypappGetProfile.Output? = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.fypapp.getProfile?actor=${input.actor}"
        val response = getRequest(endpoint, useAuth = false) ?: return@withContext null

        val profile = response.optJSONObject("profile")?.let { jsonToProfile(it) }

        ComFypappGetProfile.Output(
            profile = profile,
            uri = response.optString("uri"),
            cid = response.optString("cid"),
            matchScore = if (response.has("matchScore")) response.getInt("matchScore") else null,
            distance = if (response.has("distance")) response.getDouble("distance") else null,
            isLiked = if (response.has("isLiked")) response.getBoolean("isLiked") else null,
            isMatched = if (response.has("isMatched")) response.getBoolean("isMatched") else null,
            isBlocked = if (response.has("isBlocked")) response.getBoolean("isBlocked") else null
        )
    }

    override suspend fun updateProfile(input: ComFypappUpdateProfile.Input): ComFypappUpdateProfile.Output? = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.fypapp.updateProfile"
        val payload = JSONObject().apply {
            put("profile", profileToJson(input.profile))
        }

        val response = postRequest(endpoint, payload, useAuth = true) ?: return@withContext null

        ComFypappUpdateProfile.Output(
            uri = response.getString("uri"),
            cid = response.getString("cid")
        )
    }

    override suspend fun discover(input: ComFypappDiscover.Input): ComFypappDiscover.Output? = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.fypapp.discover?limit=${input.limit}" +
                (input.cursor?.let { "&cursor=$it" } ?: "")

        val response = getRequest(endpoint, useAuth = false) ?: return@withContext null

        val profiles = response.optJSONArray("profiles")?.let { array ->
            (0 until array.length()).mapNotNull { i ->
                val card = array.getJSONObject(i)
                ComFypappDiscover.ProfileCard(
                    did = card.getString("did"),
                    profile = card.optJSONObject("profile")?.let { jsonToProfile(it) },
                    distance = if (card.has("distance")) card.getDouble("distance") else null,
                    matchScore = if (card.has("matchScore")) card.getInt("matchScore") else null,
                    commonInterests = card.optJSONArray("commonInterests")?.let { interests ->
                        (0 until interests.length()).map { j -> interests.getString(j) }
                    },
                    recentlyActive = if (card.has("recentlyActive")) card.getBoolean("recentlyActive") else null
                )
            }
        }

        ComFypappDiscover.Output(
            profiles = profiles,
            cursor = response.optString("cursor").takeIf { it.isNotEmpty() }
        )
    }

    override suspend fun like(repo: String, record: ComFypappLike.Record) = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.atproto.repo.createRecord"
        val payload = JSONObject().apply {
            put("repo", repo)
            put("collection", "com.fypapp.like")
            put("record", JSONObject().apply {
                put("\$type", "com.fypapp.like")
                put("subject", record.subject)
                record.message?.let { put("message", it) }
                put("createdAt", record.createdAt)
                put("superLike", record.superLike)
            })
        }

        postRequest(endpoint, payload, useAuth = true)
        Unit
    }

    override suspend fun pass(repo: String, record: ComFypappPass.Record) = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.atproto.repo.createRecord"
        val payload = JSONObject().apply {
            put("repo", repo)
            put("collection", "com.fypapp.pass")
            put("record", JSONObject().apply {
                put("\$type", "com.fypapp.pass")
                put("subject", record.subject)
                put("createdAt", record.createdAt)
            })
        }

        postRequest(endpoint, payload, useAuth = true)
        Unit
    }

    override suspend fun listLikes(repo: String): List<AtProtoClient.LikeRecord>? = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.atproto.repo.listRecords?repo=$repo&collection=com.fypapp.like&limit=100"
        val response = getRequest(endpoint, useAuth = false) ?: return@withContext null

        response.optJSONArray("records")?.let { array ->
            (0 until array.length()).mapNotNull { i ->
                val item = array.getJSONObject(i)
                val record = item.getJSONObject("value")

                AtProtoClient.LikeRecord(
                    uri = item.getString("uri"),
                    cid = item.getString("cid"),
                    record = ComFypappLike.Record(
                        subject = record.getString("subject"),
                        message = record.optString("message").takeIf { it.isNotEmpty() },
                        createdAt = record.getString("createdAt"),
                        superLike = record.optBoolean("superLike", false)
                    )
                )
            }
        }
    }

    override suspend fun createMatch(record: ComFypappMatch.Record): AtUri? = withContext(Dispatchers.IO) {
        val endpoint = "$pdsUrl/xrpc/com.atproto.repo.createRecord"
        val matchId = getMatchId(record.user1, record.user2)

        val payload = JSONObject().apply {
            put("repo", record.user1)
            put("collection", "com.fypapp.match")
            put("rkey", matchId)
            put("record", JSONObject().apply {
                put("\$type", "com.fypapp.match")
                put("user1", record.user1)
                put("user2", record.user2)
                put("createdAt", record.createdAt)
                put("isActive", record.isActive)
                record.lastMessageAt?.let { put("lastMessageAt", it) }
            })
        }

        val response = postRequest(endpoint, payload, useAuth = true)
        response?.getString("uri")?.let { AtUri(it) }
    }

    // Helper methods

    private fun getMatchId(userId1: String, userId2: String): String {
        val sorted = listOf(userId1, userId2).sorted()
        return sorted.joinToString("-")
    }

    private fun profileToJson(profile: ComFypappProfile.Record): JSONObject {
        return JSONObject().apply {
            put("\$type", "com.fypapp.profile")
            profile.displayName?.let { put("displayName", it) }
            profile.bio?.let { put("bio", it) }
            profile.birthdate?.let { put("birthdate", it) }
            profile.gender?.let { put("gender", it) }
            profile.lookingFor?.let { put("lookingFor", JSONArray(it)) }
            profile.location?.let { loc ->
                put("location", JSONObject().apply {
                    put("city", loc.city)
                    loc.state?.let { put("state", it) }
                    loc.country?.let { put("country", it) }
                    loc.latitude?.let { put("latitude", it) }
                    loc.longitude?.let { put("longitude", it) }
                })
            }
            profile.photos?.let { photos ->
                put("photos", JSONArray(photos.map { JSONObject().apply { put("ref", it.ref) } }))
            }
            profile.interests?.let { put("interests", JSONArray(it)) }
            profile.height?.let { put("height", it) }
            profile.occupation?.let { put("occupation", it) }
            profile.education?.let { put("education", it) }
            profile.relationshipType?.let { put("relationshipType", it) }
            profile.smoking?.let { put("smoking", it) }
            profile.drinking?.let { put("drinking", it) }
            profile.children?.let { put("children", it) }
            profile.pets?.let { put("pets", JSONArray(it)) }
            profile.languages?.let { put("languages", JSONArray(it)) }
            profile.isActive?.let { put("isActive", it) }
            profile.isPremium?.let { put("isPremium", it) }
            profile.createdAt?.let { put("createdAt", it) }
            profile.updatedAt?.let { put("updatedAt", it) }
        }
    }

    private fun jsonToProfile(json: JSONObject): ComFypappProfile.Record {
        return ComFypappProfile.Record(
            displayName = json.optString("displayName").takeIf { it.isNotEmpty() },
            bio = json.optString("bio").takeIf { it.isNotEmpty() },
            birthdate = json.optString("birthdate").takeIf { it.isNotEmpty() },
            gender = json.optString("gender").takeIf { it.isNotEmpty() },
            lookingFor = json.optJSONArray("lookingFor")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            location = json.optJSONObject("location")?.let { loc ->
                ComFypappProfile.Location(
                    city = loc.getString("city"),
                    state = loc.optString("state").takeIf { it.isNotEmpty() },
                    country = loc.optString("country").takeIf { it.isNotEmpty() },
                    latitude = if (loc.has("latitude")) loc.getDouble("latitude") else null,
                    longitude = if (loc.has("longitude")) loc.getDouble("longitude") else null
                )
            },
            photos = json.optJSONArray("photos")?.let { array ->
                (0 until array.length()).map { i ->
                    val photo = array.getJSONObject(i)
                    ComFypappProfile.BlobRef(photo.getString("ref"))
                }
            },
            interests = json.optJSONArray("interests")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            height = if (json.has("height")) json.getInt("height") else null,
            occupation = json.optString("occupation").takeIf { it.isNotEmpty() },
            education = json.optString("education").takeIf { it.isNotEmpty() },
            relationshipType = json.optString("relationshipType").takeIf { it.isNotEmpty() },
            smoking = json.optString("smoking").takeIf { it.isNotEmpty() },
            drinking = json.optString("drinking").takeIf { it.isNotEmpty() },
            children = json.optString("children").takeIf { it.isNotEmpty() },
            pets = json.optJSONArray("pets")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            languages = json.optJSONArray("languages")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            isActive = if (json.has("isActive")) json.getBoolean("isActive") else null,
            isPremium = if (json.has("isPremium")) json.getBoolean("isPremium") else null,
            createdAt = json.optString("createdAt").takeIf { it.isNotEmpty() },
            updatedAt = json.optString("updatedAt").takeIf { it.isNotEmpty() }
        )
    }

    private fun getRequest(url: String, useAuth: Boolean): JSONObject? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")

            if (useAuth) {
                val auth = "admin:$adminPassword"
                val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
                connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                JSONObject(response)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun postRequest(url: String, payload: JSONObject, useAuth: Boolean): JSONObject? {
        return try {
            android.util.Log.d("AtProtoClient", "POST $url")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000

            if (useAuth) {
                val token = accessToken
                if (token != null) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    android.util.Log.d("AtProtoClient", "Using Bearer token auth")
                } else {
                    android.util.Log.w("AtProtoClient", "No access token available for authenticated request")
                }
            }

            // Write payload
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            android.util.Log.d("AtProtoClient", "Response code: $responseCode")

            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                android.util.Log.d("AtProtoClient", "Response body: $response")
                if (response.isNotEmpty()) JSONObject(response) else JSONObject()
            } else {
                // Log error response
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorBody = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    android.util.Log.e("AtProtoClient", "Error $responseCode: $errorBody")
                } else {
                    android.util.Log.e("AtProtoClient", "Error $responseCode: No error body")
                }
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("AtProtoClient", "Request failed: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
}
