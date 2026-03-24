package fyp.project.datingapp.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName($$"$type")
    val type: String = "fyp.project.datingapp.records.message",

    val recipient: String,          // DID of the recipient
    val cipherText: ByteArray,      //TODO: Encrypted payload
    val createdAt: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is Message) return false
        return type == other.type &&
                recipient == other.recipient &&
                cipherText.contentEquals(other.cipherText) &&
                createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + recipient.hashCode()
        result = 31 * result + cipherText.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}