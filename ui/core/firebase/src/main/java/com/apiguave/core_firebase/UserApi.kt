package com.apiguave.core_firebase

// Firebase imports (commented out - using AT Protocol now)
// import com.apiguave.core_firebase.extensions.getTaskResult
// import com.google.firebase.firestore.FieldValue
// import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.Query
// import com.google.firebase.firestore.ktx.toObject

// AT Protocol imports
import com.apiguave.core_firebase.model.FirestoreUser
import com.apiguave.tinderclonedata.source.firebase.model.FirestoreOrientation
import com.fypapp.lexicons.*
import com.fypapp.lexicons.repo.AtProtoClient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object UserApi {

    private const val USERS = "users"
    private const val MATCHES = "matches"

    // AT Protocol client instance
    private val atProtoClient: AtProtoClient = AtProtoClient.default

    suspend fun createUser(
        userId: String,
        name: String,
        birthdate: LocalDate,
        bio: String,
        isMale: Boolean,
        orientation: FirestoreOrientation
    ) {
        // Old Firebase implementation
        // val user = FirestoreUser(
        //     name = name,
        //     birthDate = birthdate.toDate(),
        //     bio = bio,
        //     male = isMale,
        //     orientation = orientation,
        //     pictures = emptyList(),
        //     liked = emptyList(),
        //     passed = emptyList()
        // )
        // FirebaseFirestore.getInstance().collection(USERS).document(userId).set(user).getTaskResult()

        // New AT Protocol implementation
        val profile = ComFypappProfile.Record(
            displayName = name,
            bio = bio,
            birthdate = birthdate.format(DateTimeFormatter.ISO_DATE) + "T00:00:00Z",
            gender = if (isMale) "man" else "woman",
            lookingFor = when (orientation) {
                FirestoreOrientation.men -> listOf("man")
                FirestoreOrientation.women -> listOf("woman")
                FirestoreOrientation.both -> listOf("man", "woman")
            },
            photos = emptyList(),
            interests = emptyList(),
            isActive = true,
            isPremium = false,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )

        atProtoClient.createProfile(userId, profile)
    }

    suspend fun userExists(): Boolean {
        // Old Firebase implementation (commented out)
        // val snapshot = FirebaseFirestore.getInstance().collection(USERS).document(AuthApi.userId!!).get().getTaskResult()
        // return snapshot.exists()

        // New AT Protocol implementation
        return atProtoClient.profileExists(AuthApi.userId!!)
    }

    suspend fun getUser(userId: String): FirestoreUser? {
        // Old Firebase implementation (commented out)
        // val snapshot = FirebaseFirestore.getInstance().collection(USERS).document(userId).get().getTaskResult()
        // return snapshot.toObject<FirestoreUser>()

        // New AT Protocol implementation
        val input = ComFypappGetProfile.Input(actor = userId)
        val output = atProtoClient.getProfile(input) ?: return null

        return output.profile?.toFirestoreUser(userId)
    }

    suspend fun getCompatibleUsers(currentUser: FirestoreUser): List<FirestoreUser> {
        // Old Firebase implementation (commented out)
        // val excludedUserIds = currentUser.liked + currentUser.passed + currentUser.id
        //
        // //Build query
        // val searchQuery: Query = kotlin.run {
        //     var query: Query = FirebaseFirestore.getInstance().collection(USERS)
        //     if(currentUser.male != null) {
        //         query = query.whereNotEqualTo(
        //             FirestoreUserProperties.orientation,
        //             when (currentUser.male) {
        //                 true -> FirestoreOrientation.women.name
        //                 false -> FirestoreOrientation.men.name
        //             }
        //         )
        //     }
        //
        //     if (currentUser.orientation != FirestoreOrientation.both) {
        //         query = query.whereEqualTo(
        //             FirestoreUserProperties.isMale,
        //             currentUser.orientation == FirestoreOrientation.men
        //         )
        //     }
        //
        //     query
        // }
        //
        // val result = searchQuery.get().getTaskResult()
        // //Filter documents
        // return result.filter { !excludedUserIds.contains(it.id) }.mapNotNull { it.toObject<FirestoreUser>() }

        // New AT Protocol implementation using discover endpoint
        val input = ComFypappDiscover.Input(limit = 50)
        val output = atProtoClient.discover(input) ?: return emptyList()

        return output.profiles?.mapNotNull { profileCard ->
            profileCard.profile?.toFirestoreUser(profileCard.did)
        } ?: emptyList()
    }

    suspend fun swipeUser(swipedUserId: String, isLike: Boolean): String? {
        // Old Firebase implementation (commented out)
        // FirebaseFirestore.getInstance()
        //     .collection(USERS)
        //     .document(AuthApi.userId!!)
        //     .update(mapOf((if (isLike) FirestoreUserProperties.liked else FirestoreUserProperties.passed) to FieldValue.arrayUnion(swipedUserId)))
        //     .getTaskResult()
        // FirebaseFirestore.getInstance()
        //     .collection(USERS)
        //     .document(AuthApi.userId!!)
        //     .collection(FirestoreUserProperties.liked)
        //     .document(swipedUserId)
        //     .set(mapOf("exists" to true))
        //     .getTaskResult()
        //
        // val hasUserLikedBack = hasUserLikedBack(swipedUserId)
        // if(hasUserLikedBack){
        //     val matchId = getMatchId(AuthApi.userId!!, swipedUserId)
        //     FieldValue.serverTimestamp()
        //     val data = FirestoreMatchProperties.toData(swipedUserId, AuthApi.userId!!)
        //     FirebaseFirestore.getInstance()
        //         .collection(MATCHES)
        //         .document(matchId)
        //         .set(data)
        //         .getTaskResult()
        //
        //
        //     return matchId
        // }
        // return null

        // New AT Protocol implementation
        val currentUserId = AuthApi.userId!!
        val currentTime = Instant.now().toString()

        if (isLike) {
            // Create like record
            val likeRecord = ComFypappLike.Record(
                subject = swipedUserId,
                message = null,
                createdAt = currentTime,
                superLike = false
            )
            atProtoClient.like(currentUserId, likeRecord)

            // Check if the other user has liked back
            val hasUserLikedBack = hasUserLikedBack(swipedUserId)
            if (hasUserLikedBack) {
                // Create match
                val matchRecord = ComFypappMatch.Record(
                    user1 = currentUserId,
                    user2 = swipedUserId,
                    createdAt = currentTime,
                    isActive = true,
                    lastMessageAt = null
                )
                val matchUri = atProtoClient.createMatch(matchRecord)
                return matchUri?.toString()
            }
        } else {
            // Create pass record
            val passRecord = ComFypappPass.Record(
                subject = swipedUserId,
                createdAt = currentTime
            )
            atProtoClient.pass(currentUserId, passRecord)
        }

        return null
    }


    private fun getMatchId(userId1: String, userId2: String): String{
        return if(userId1 > userId2){
            userId1 + userId2
        } else userId2 + userId1
    }

    private suspend fun hasUserLikedBack(swipedUserId: String): Boolean{
        // Old Firebase implementation (commented out)
        // val currentUserId = AuthApi.userId!!
        // val result = FirebaseFirestore.getInstance()
        //     .collection(USERS)
        //     .document(swipedUserId)
        //     .collection(FirestoreUserProperties.liked)
        //     .document(currentUserId)
        //     .get()
        //     .getTaskResult()
        // return result.exists()

        // New AT Protocol implementation
        val currentUserId = AuthApi.userId!!
        val likes = atProtoClient.listLikes(swipedUserId) ?: return false

        return likes.any { it.record.subject == currentUserId }
    }

    suspend fun updateUser(
        bio: String,
        gender: Boolean,
        orientation: FirestoreOrientation) {
        // Old Firebase implementation (commented out)
        // val data = mapOf(
        //     FirestoreUserProperties.bio to bio,
        //     FirestoreUserProperties.isMale to gender,
        //     FirestoreUserProperties.orientation to orientation
        // )
        // FirebaseFirestore.getInstance().collection(USERS).document(AuthApi.userId!!).update(data).getTaskResult()

        // New AT Protocol implementation
        val currentUserId = AuthApi.userId!!

        // Get current profile first
        val currentProfile = atProtoClient.getProfile(ComFypappGetProfile.Input(actor = currentUserId))?.profile
            ?: return

        // Update with new values
        val updatedProfile = currentProfile.copy(
            bio = bio,
            gender = if (gender) "man" else "woman",
            lookingFor = when (orientation) {
                FirestoreOrientation.men -> listOf("man")
                FirestoreOrientation.women -> listOf("woman")
                FirestoreOrientation.both -> listOf("man", "woman")
            },
            updatedAt = Instant.now().toString()
        )

        val input = ComFypappUpdateProfile.Input(profile = updatedProfile)
        atProtoClient.updateProfile(input)
    }


    suspend fun updateUserPictures(pictures: List<String>) {
        // Old Firebase implementation (commented out)
        // val data = mapOf(
        //     FirestoreUserProperties.pictures to pictures
        // )
        // FirebaseFirestore.getInstance().collection(USERS).document(AuthApi.userId!!).update(data).getTaskResult()

        // New AT Protocol implementation
        val currentUserId = AuthApi.userId!!

        // Get current profile first
        val currentProfile = atProtoClient.getProfile(ComFypappGetProfile.Input(actor = currentUserId))?.profile
            ?: return

        // Update with new photo references
        val updatedProfile = currentProfile.copy(
            photos = pictures.map { ComFypappProfile.BlobRef(it) },
            updatedAt = Instant.now().toString()
        )

        val input = ComFypappUpdateProfile.Input(profile = updatedProfile)
        atProtoClient.updateProfile(input)
    }

    // Helper function to parse ISO 8601 datetime strings to java.util.Date
    private fun parseIsoDateTime(isoString: String?): Date {
        if (isoString.isNullOrEmpty()) return Date()

        return try {
            // Try parsing as ISO instant (e.g., "2024-01-15T10:30:00Z")
            val instant = Instant.parse(isoString)
            Date.from(instant)
        } catch (e: Exception) {
            try {
                // Try parsing as date only (e.g., "2024-01-15")
                val localDate = LocalDate.parse(isoString.substring(0, 10))
                Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            } catch (e2: Exception) {
                // Fallback to current date if parsing fails
                Date()
            }
        }
    }

    // Extension function to convert AT Protocol profile to FirestoreUser
    private fun ComFypappProfile.Record.toFirestoreUser(userId: String): FirestoreUser {
        return FirestoreUser(
            id = userId,
            name = this.displayName ?: "",
            birthDate = parseIsoDateTime(this.birthdate),
            bio = this.bio ?: "",
            male = when (this.gender) {
                "man" -> true
                "woman" -> false
                else -> null
            },
            orientation = when {
                this.lookingFor.isNullOrEmpty() -> FirestoreOrientation.both
                this.lookingFor.contains("man") && this.lookingFor.contains("woman") -> FirestoreOrientation.both
                this.lookingFor.contains("man") -> FirestoreOrientation.men
                this.lookingFor.contains("woman") -> FirestoreOrientation.women
                else -> FirestoreOrientation.both
            },
            pictures = this.photos?.map { it.ref } ?: emptyList(),
            liked = emptyList(), // These would need to be fetched separately if needed
            passed = emptyList()
        )
    }
}