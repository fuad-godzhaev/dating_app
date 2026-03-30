package fyp.project.datingapp.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Like (
    @SerialName("\$type")
    val type: String = "fyp.project.datingapp.records.like",
    val subject: String,
    val createdAt: String
    ) {
}