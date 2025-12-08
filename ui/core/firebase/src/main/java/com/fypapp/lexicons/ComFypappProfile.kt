package com.fypapp.lexicons

/**
 * com.fypapp.profile - Dating profile record
 */
object ComFypappProfile {
    data class Record(
        val displayName: String? = null,
        val bio: String? = null,
        val birthdate: String? = null,
        val gender: String? = null,
        val lookingFor: List<String>? = null,
        val location: Location? = null,
        val photos: List<BlobRef>? = null,
        val interests: List<String>? = null,
        val height: Int? = null,
        val occupation: String? = null,
        val education: String? = null,
        val relationshipType: String? = null,
        val smoking: String? = null,
        val drinking: String? = null,
        val children: String? = null,
        val pets: List<String>? = null,
        val languages: List<String>? = null,
        val isActive: Boolean? = null,
        val isPremium: Boolean? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null
    )

    data class Location(
        val city: String,
        val state: String? = null,
        val country: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    data class BlobRef(
        val ref: String
    )
}
