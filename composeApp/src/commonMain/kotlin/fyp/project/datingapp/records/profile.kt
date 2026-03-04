package fyp.project.datingapp.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

val appJson = Json {
    ignoreUnknownKeys = true
    // TODO: Pretty print for debugging (disable in production for smaller payloads)
    prettyPrint = true
    encodeDefaults = true
    classDiscriminator = "type"
    isLenient = true
}