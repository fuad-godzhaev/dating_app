package com.aura.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName($$"$type")
    val type: String = "com.aura.records.profile",

    val did: String,
    val displayName: String,
    val bio: String,
    val age: Int,

    val avatar: BlobRef? = null,
    val photos: List<BlobRef>? = null,

    val signingKey: ByteArray, // public key for repo verification (also the ECIES key)

    val interests: List<String>,
    val location: Geolocation? = null,
    val createdAt: String,
)