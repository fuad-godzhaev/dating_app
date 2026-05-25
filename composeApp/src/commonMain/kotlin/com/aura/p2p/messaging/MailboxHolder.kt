package com.aura.p2p.messaging

import com.aura.database.appView.dao.MailboxDao
import com.aura.database.appView.entities.MailboxEntity
import com.aura.p2p.relay.CacheCipher
import com.aura.p2p.relay.EpochClock
import com.aura.p2p.relay.SystemClock
import com.aura.p2p.transport.wire.MessageEnvelope
import com.aura.records.canonical.decodeMessageEnvelope
import com.aura.records.canonical.encodeMessageEnvelopeWire
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Durable store-and-forward mailbox a cacheHolder runs for offline recipients
 * (ADR-0001 / M5, relay-architecture §9). Backed by [MailboxDao] so queued mail
 * survives a holder restart - the earlier in-memory version dropped everything on
 * process death, which negated most of the mailbox's value.
 *
 * Each envelope is sealed **at rest** with the cache AEAD ([cache]); the holder's
 * on-device database therefore exposes neither message content (already
 * end-to-end ECIES-sealed) nor the sender/recipient routing metadata of the mail it
 * forwards. Abuse is bounded by a per-(sender,recipient) count cap and a per-recipient
 * total cap; deposits dedup by msgId. Pull is non-destructive (kept until TTL) and the
 * recipient dedups on insert, so a failed or repeated pull loses nothing.
 *
 * The associated data binds each ciphertext to its `recipientDid|msgId`, so a row
 * whose blob is swapped under a different key fails GCM verification and is silently
 * skipped on pull (treated as absent).
 */
class MailboxHolder(
    private val dao: MailboxDao,
    private val cache: CacheCipher,
    private val clock: EpochClock = SystemClock,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val maxPerSenderPerRecipient: Int = DEFAULT_MAX_PER_SENDER,
    private val maxEnvelopesPerRecipient: Int = DEFAULT_MAX_PER_RECIPIENT,
) {
    // Serialises the cap-check + insert so concurrent deposits can't overrun a cap.
    private val mutex = Mutex()

    /** Queue [envelope] for its recipient. Returns false if a cap is hit; idempotent on msgId. */
    suspend fun deposit(envelope: MessageEnvelope): Boolean = mutex.withLock {
        val now = clock.nowMs()
        val cutoff = now - ttlMs
        dao.deleteExpired(cutoff)
        if (dao.has(envelope.msgId)) return@withLock true // dedup
        if (dao.countForRecipient(envelope.recipientDid, cutoff) >= maxEnvelopesPerRecipient) return@withLock false
        if (dao.countForSender(envelope.recipientDid, envelope.senderDid, cutoff) >= maxPerSenderPerRecipient) {
            return@withLock false
        }
        val sealed = cache.seal(encodeMessageEnvelopeWire(envelope), aad(envelope.recipientDid, envelope.msgId))
        dao.insert(
            MailboxEntity(
                msgId = envelope.msgId,
                recipientDid = envelope.recipientDid,
                senderDid = envelope.senderDid,
                depositedAt = now,
                ciphertext = sealed.ciphertext,
                nonce = sealed.nonce,
            ),
        )
        true
    }

    /** Non-expired envelopes queued for [recipientDid] (non-destructive). Tampered rows are skipped. */
    suspend fun pull(recipientDid: String): List<MessageEnvelope> {
        val cutoff = clock.nowMs() - ttlMs
        return dao.forRecipient(recipientDid, cutoff).mapNotNull { row ->
            val wire = cache.open(row.ciphertext, row.nonce, aad(recipientDid, row.msgId)) ?: return@mapNotNull null
            runCatching { decodeMessageEnvelope(wire) }.getOrNull()
        }
    }

    suspend fun count(recipientDid: String): Int =
        dao.countForRecipient(recipientDid, clock.nowMs() - ttlMs)

    private fun aad(recipientDid: String, msgId: String): ByteArray = "$recipientDid|$msgId".encodeToByteArray()

    companion object {
        const val DEFAULT_TTL_MS: Long = 7L * 24 * 60 * 60 * 1000
        const val DEFAULT_MAX_PER_SENDER: Int = 50
        const val DEFAULT_MAX_PER_RECIPIENT: Int = 500
    }
}
