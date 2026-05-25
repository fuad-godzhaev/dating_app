package com.aura.database.appView.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single sealed envelope a cacheHolder is storing-and-forwarding for an offline
 * recipient (ADR-0001 / M5). Persistent so queued mail survives a holder restart -
 * the in-memory v1 dropped everything on process death, which gutted the mailbox's
 * practical value.
 *
 * The row is sealed **at rest**: [ciphertext]/[nonce] are the cache-AEAD output over
 * the message envelope's wire bytes ([com.aura.p2p.relay.CacheEncryption],
 * associated data = `recipientDid|msgId`). A filesystem dump of a holder therefore
 * reveals neither message content (already ECIES-sealed end-to-end) nor the
 * sender/recipient/timestamp routing metadata of the mail it forwards. [recipientDid]
 * and [senderDid] are kept in the clear only as query/cap keys; they are never the
 * sole protection (the sealed blob is the record of truth).
 *
 * [msgId] is the primary key so an at-least-once deposit (online retry + mailbox
 * replay) dedups naturally on insert.
 */
@Entity(
    tableName = "mailbox",
    indices = [
        Index(value = ["recipientDid", "depositedAt"]),
        Index(value = ["recipientDid", "senderDid"]),
    ],
)
data class MailboxEntity(
    @PrimaryKey val msgId: String,
    val recipientDid: String,
    val senderDid: String,
    val depositedAt: Long,
    val ciphertext: ByteArray,
    val nonce: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MailboxEntity) return false
        return msgId == other.msgId &&
            recipientDid == other.recipientDid &&
            senderDid == other.senderDid &&
            depositedAt == other.depositedAt &&
            ciphertext.contentEquals(other.ciphertext) &&
            nonce.contentEquals(other.nonce)
    }

    override fun hashCode(): Int {
        var result = msgId.hashCode()
        result = 31 * result + recipientDid.hashCode()
        result = 31 * result + senderDid.hashCode()
        result = 31 * result + depositedAt.hashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        return result
    }
}
