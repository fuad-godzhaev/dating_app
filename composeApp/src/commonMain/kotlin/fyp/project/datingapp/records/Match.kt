package fyp.project.datingapp.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match (
    @SerialName("\$type")
    val type: String = "fyp.project.datingapp.records.match",

    val subject: String,
    val createdAt: String
)