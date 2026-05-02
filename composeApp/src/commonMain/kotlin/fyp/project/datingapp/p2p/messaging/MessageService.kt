package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.database.appView.dao.MessageDao
import fyp.project.datingapp.database.appView.entities.ConversationEntity
import fyp.project.datingapp.database.appView.entities.MessageEntity
import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.discovery.PeerDirectory
import fyp.project.datingapp.p2p.fetch.ProfileFetcher
import fyp.project.datingapp.p2p.relay.EpochClock
import fyp.project.datingapp.p2p.relay.SystemClock
import fyp.project.datingapp.p2p.transport.wire.MessageEnvelope
import fyp.project.datingapp.records.canonical.decodeMessageEnvelope
import fyp.project.datingapp.records.canonical.encodeCanonical
import fyp.project.datingapp.records.canonical.encodeMessageEnvelopeWire
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Orchestrates 1:1 E2EE messaging (ADR-0001 / M4): encrypt + sign + send outgoing,
 * and verify + decrypt + store incoming. Depends only on seams ([MessageCrypto],
 * [MessageStreamClient], [ProfileFetcher], DAO, signer), so the flow is unit-testable
 * with a fake [MessageCrypto].
 *
 * Inner [MessageCrypto] layer (P-256 ECIES) = confidentiality + integrity (E2E).
 * Outer [MessageEnvelope.signature] (sender's P-256 over the signable projection) =
 * transport-layer sender authentication, checked by [MessageEnvelopeVerifier].
 */
class MessageService(
    private val selfDid: suspend () -> String?,
    private val crypto: MessageCrypto,
    private val sign: suspend (ByteArray) -> ByteArray,
    private val verifier: SignatureVerifier,
    private val fetcher: ProfileFetcher,
    private val peerDirectory: PeerDirectory,
    private val streamClient: MessageStreamClient,
    private val messageDao: MessageDao,
    private val clock: EpochClock = SystemClock,
    private val newMsgId: () -> String = { defaultMsgId() },
    private val nowIso: () -> String = { Clock.System.now().toString() },
    // M5: when online delivery fails, park the sealed envelope at the recipient's
    // mailbox holders (best-effort). Null in tests / when messaging is offline-less.
    private val offlineDeposit: (suspend (MessageEnvelope) -> Boolean)? = null,
) {

    /**
     * Encrypt [text] for [recipientDid], persist it locally (queued), and attempt
     * online delivery. Returns true iff the peer acked. A false result leaves the
     * message queued (offline mailbox delivery is M5).
     */
    suspend fun sendMessage(recipientDid: String, text: String): Boolean {
        val sender = selfDid() ?: return false

        // ECIES seals to the recipient's DID key directly - no bundle fetch needed.
        // (Still fetch the profile for the conversation display name below.)
        val recipientProfile = runCatching { fetcher.fetch(recipientDid) }.getOrNull()

        val sealed = runCatching {
            crypto.encrypt(recipientDid, text.encodeToByteArray())
        }.getOrNull() ?: return false

        val msgId = newMsgId()
        val unsigned = MessageEnvelope(
            senderDid = sender,
            recipientDid = recipientDid,
            msgId = msgId,
            ciphertext = sealed.ciphertext,
            messageType = sealed.messageType,
            sentAt = nowIso(),
            signature = ByteArray(0),
        )
        val envelope = unsigned.copy(signature = sign(encodeCanonical(unsigned)))

        val now = clock.nowMs()
        messageDao.insertMessage(
            MessageEntity(msgId, recipientDid, MessageEntity.DIRECTION_OUT, text, now, now, MessageEntity.STATE_QUEUED),
        )
        ensureConversation(recipientDid, recipientProfile?.displayName)

        val contact = peerDirectory.get(recipientDid)
        val delivered = contact != null &&
            runCatching { streamClient.send(contact, encodeMessageEnvelopeWire(envelope)) }.getOrDefault(false)

        // Offline fallback (M5): recipient unreachable -> park the sealed envelope at
        // their mailbox holders for pickup on reconnect.
        if (!delivered) {
            runCatching { offlineDeposit?.invoke(envelope) }
        }

        messageDao.updateDeliveryState(msgId, if (delivered) MessageEntity.STATE_SENT else MessageEntity.STATE_QUEUED)
        messageDao.onOutgoingMessage(recipientDid, text, now)
        return delivered
    }

    /**
     * Handle a wire envelope received over [MESSAGE_PROTOCOL_ID]: verify the sender
     * signature, decrypt, dedup by msgId, store, and bump the conversation. Returns
     * true if the message is accepted (or was a known duplicate).
     */
    suspend fun handleIncoming(envelopeWireBytes: ByteArray): Boolean {
        val envelope = runCatching { decodeMessageEnvelope(envelopeWireBytes) }.getOrNull() ?: return false
        if (envelope.recipientDid != selfDid()) return false
        if (!MessageEnvelopeVerifier.verify(envelope, verifier)) return false
        if (messageDao.hasMessage(envelope.msgId)) return true // dedup (online + mailbox replay)

        val plaintext = runCatching {
            crypto.decrypt(envelope.senderDid, envelope.ciphertext, envelope.messageType)
        }.getOrNull() ?: return false

        val text = plaintext.decodeToString()
        val now = clock.nowMs()
        messageDao.insertMessage(
            MessageEntity(envelope.msgId, envelope.senderDid, MessageEntity.DIRECTION_IN, text, now, now, MessageEntity.STATE_RECEIVED),
        )
        ensureConversation(envelope.senderDid, null)
        messageDao.onNewMessage(envelope.senderDid, text, now)
        return true
    }

    private suspend fun ensureConversation(peerDid: String, displayName: String?) {
        if (messageDao.hasConversation(peerDid)) return
        val name = displayName
            ?: runCatching { fetcher.fetch(peerDid)?.displayName }.getOrNull()
            ?: peerDid
        messageDao.upsertConversation(
            ConversationEntity(peerDid = peerDid, peerDisplayName = name, matchedAt = clock.nowMs()),
        )
    }

    companion object {
        private fun defaultMsgId(): String {
            val bytes = ByteArray(16).also { Random.nextBytes(it) }
            return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        }
    }
}
