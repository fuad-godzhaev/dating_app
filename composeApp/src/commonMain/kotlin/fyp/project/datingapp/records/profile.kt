package fyp.project.datingapp.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName($$"$type")
    val type: String = "fyp.project.datingapp.records.profile",

    //val did: String,
    val displayName: String,
    val bio: String,
    val age: Int,

    val avatar: BlobRef? = null,
    val photos: List<BlobRef>? = null,

    val interests: List<String>,
    val location: GeoLocation? = null,
    val createdAt: String,
)