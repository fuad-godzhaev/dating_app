package fyp.project.datingapp.database.appView.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single chat message row (Part 5 / M3). Stored *decrypted* locally after the
 * E2EE layer has done its work; the ciphertext only ever exists in transit
 * ([fyp.project.datingapp.p2p.transport.wire.MessageEnvelope]). [msgId] is the
 * primary key so an at-least-once delivery (online + mailbox replay) dedups
 * naturally on insert.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationDid", "sentAt"])],
)
data class MessageEntity(
    @PrimaryKey val msgId: String,
    val conversationDid: String,   // peer DID this message belongs to
    val direction: String,         // DIRECTION_IN | DIRECTION_OUT
    val plaintext: String,
    val sentAt: Long,
    val receivedAt: Long,
    val deliveryState: String,     // STATE_*
) {
    companion object {
        const val DIRECTION_IN = "in"
        const val DIRECTION_OUT = "out"

        const val STATE_QUEUED = "queued"       // outgoing, awaiting delivery
        const val STATE_SENT = "sent"           // outgoing, handed to the recipient/holder
        const val STATE_DELIVERED = "delivered" // outgoing, recipient acknowledged
        const val STATE_RECEIVED = "received"   // incoming, decrypted + stored
    }
}
