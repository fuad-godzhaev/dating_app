package fyp.project.datingapp.records

import kotlinx.serialization.Serializable

@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val geohash: String? = null
)