package fyp.project.datingapp.database.appView.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incoming_likes",
    indices = [
        Index(value = ["fromDid"]),
        Index(value = ["isMatched"])
    ]
)
data class IncomingLikeEntity(
    @PrimaryKey
    val id: String,

    val fromDid: String,
    val createdAt: String,
    val receivedAt: Long,

    val verifiedCborBytes: ByteArray,
    val commitSignature: ByteArray,

    val isMatched: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IncomingLikeEntity) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}