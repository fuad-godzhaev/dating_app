package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.p2p.relay.EpochClock
import fyp.project.datingapp.p2p.relay.SystemClock
import fyp.project.datingapp.p2p.transport.wire.MessageEnvelope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory store-and-forward mailbox a cacheHolder runs for offline recipients
 * (ADR-0001 / M5, relay-architecture §9). Holds sealed [MessageEnvelope]s with a
 * TTL; bounds abuse with a per-(sender,recipient) count cap and a per-recipient
 * total cap; dedups by msgId. Pull is non-destructive (kept until TTL) and the
 * recipient dedups, so a failed pull loses nothing.
 *
 * v1 is in-memory (a holder restart drops queued mail); since messages also arrive
 * online and the recipient dedups, loss is recoverable. Persistence is a follow-up.
 */
class MailboxHolder(
    private val clock: EpochClock = SystemClock,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val maxPerSenderPerRecipient: Int = DEFAULT_MAX_PER_SENDER,
    private val maxEnvelopesPerRecipient: Int = DEFAULT_MAX_PER_RECIPIENT,
) {
    private data class Stored(val envelope: MessageEnvelope, val depositedAt: Long)

    private val mutex = Mutex()
    private val byRecipient = mutableMapOf<String, MutableList<Stored>>()

    /** Queue [envelope] for its recipient. Returns false if a cap is hit; idempotent on msgId. */
    suspend fun deposit(envelope: MessageEnvelope): Boolean = mutex.withLock {
        val now = clock.nowMs()
        val list = byRecipient.getOrPut(envelope.recipientDid) { mutableListOf() }
        purge(list, now)
        if (list.any { it.envelope.msgId == envelope.msgId }) return@withLock true // dedup
        if (list.size >= maxEnvelopesPerRecipient) return@withLock false
        val fromSender = list.count { it.envelope.senderDid == envelope.senderDid }
        if (fromSender >= maxPerSenderPerRecipient) return@withLock false
        list.add(Stored(envelope, now))
        true
    }

    /** Non-expired envelopes queued for [recipientDid] (non-destructive). */
    suspend fun pull(recipientDid: String): List<MessageEnvelope> = mutex.withLock {
        val now = clock.nowMs()
        val list = byRecipient[recipientDid] ?: return@withLock emptyList()
        purge(list, now)
        list.map { it.envelope }
    }

    suspend fun count(recipientDid: String): Int = mutex.withLock {
        val list = byRecipient[recipientDid] ?: return@withLock 0
        purge(list, clock.nowMs())
        list.size
    }

    private fun purge(list: MutableList<Stored>, now: Long) {
        list.removeAll { now - it.depositedAt > ttlMs }
    }

    companion object {
        const val DEFAULT_TTL_MS: Long = 7L * 24 * 60 * 60 * 1000
        const val DEFAULT_MAX_PER_SENDER: Int = 50
        const val DEFAULT_MAX_PER_RECIPIENT: Int = 500
    }
}
