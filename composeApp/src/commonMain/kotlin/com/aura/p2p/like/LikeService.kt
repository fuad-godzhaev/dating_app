package com.aura.p2p.like

import com.aura.database.RepositoryManager
import com.aura.database.appView.dao.IncomingLikesDao
import com.aura.database.appView.dao.MessageDao
import com.aura.database.appView.entities.ConversationEntity
import com.aura.database.appView.entities.IncomingLikeEntity
import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.discovery.PeerDirectory
import com.aura.p2p.fetch.ProfileEnvelopeVerifier
import com.aura.p2p.fetch.ProfileFetcher
import com.aura.p2p.relay.EpochClock
import com.aura.p2p.relay.SystemClock
import com.aura.records.canonical.decodeLike
import com.aura.records.canonical.decodeSignedEnvelope
import com.aura.records.canonical.encodeCanonical
import kotlin.time.Clock

/**
 * Drives the core match loop (E). Sending a like signs + stores it locally and delivers
 * it to the target over [LIKE_PROTOCOL_ID]; receiving one verifies + stores it. A mutual
 * like (either direction discovering the reciprocal) creates a [Match] record, marks the
 * incoming like matched, seeds a conversation, and signals the UI.
 *
 * Depends only on seams (DAOs, [RepositoryManager], [PeerDirectory], [LikeStreamClient],
 * [ProfileFetcher]), so the match logic is unit-testable without the transport.
 */
class LikeService(
    private val selfDid: suspend () -> String?,
    private val repo: RepositoryManager,
    private val incomingLikes: IncomingLikesDao,
    private val messageDao: MessageDao,
    private val fetcher: ProfileFetcher,
    private val verifier: SignatureVerifier,
    private val peerDirectory: PeerDirectory,
    private val streamClient: LikeStreamClient,
    private val clock: EpochClock = SystemClock,
    private val nowIso: () -> String = { Clock.System.now().toString() },
    // Signalled when a match is established (e.g. to raise the It's-a-match overlay).
    private val onMatch: suspend (peerDid: String, peerName: String?) -> Unit = { _, _ -> },
) {
    /**
     * Like [targetDid]: store + sign the like, deliver it (best-effort), and if the
     * target had already liked us, establish the match now. Returns true if it became a
     * match. [targetName] (known from the swipe card) names the conversation/overlay.
     */
    suspend fun sendLike(targetDid: String, targetName: String? = null): Boolean {
        val envelope = repo.putLike(targetDid).getOrNull() ?: return false
        peerDirectory.get(targetDid)?.let { contact ->
            runCatching { streamClient.send(contact, encodeCanonical(envelope)) }
        }
        if (incomingLikes.hasLikeFrom(targetDid)) {
            createMatch(targetDid, targetName)
            return true
        }
        return false
    }

    /**
     * Handle a signed like received over [LIKE_PROTOCOL_ID]: verify it is addressed to us
     * and signed by its claimed owner, store it, and if we had already liked the sender,
     * establish the match. Returns true if accepted.
     */
    suspend fun handleIncomingLike(envelopeBytes: ByteArray): Boolean {
        val envelope = runCatching { decodeSignedEnvelope(envelopeBytes) }.getOrNull() ?: return false
        if (envelope.collection != RepositoryManager.Collections.LIKE) return false
        val me = selfDid() ?: return false
        if (envelope.rkey != me) return false // not addressed to us
        if (!ProfileEnvelopeVerifier.verify(envelope, verifier)) return false

        val from = envelope.ownerDid
        val createdAt = runCatching { decodeLike(envelope.canonicalBytes).createdAt }.getOrNull() ?: nowIso()
        incomingLikes.insertLike(
            IncomingLikeEntity(
                id = envelope.cid,
                fromDid = from,
                createdAt = createdAt,
                receivedAt = clock.nowMs(),
                verifiedCborBytes = envelope.canonicalBytes,
                commitSignature = envelope.signature,
            ),
        )
        if (repo.hasOutgoingLike(from)) {
            createMatch(from, null)
        }
        return true
    }

    private suspend fun createMatch(peerDid: String, peerName: String?) {
        runCatching { repo.putMatch(peerDid) }
        runCatching { incomingLikes.markAsMatched(peerDid) }
        if (!messageDao.hasConversation(peerDid)) {
            val name = peerName
                ?: runCatching { fetcher.fetch(peerDid)?.displayName }.getOrNull()
                ?: peerDid
            messageDao.upsertConversation(
                ConversationEntity(peerDid = peerDid, peerDisplayName = name, matchedAt = clock.nowMs()),
            )
        }
        runCatching { onMatch(peerDid, peerName) }
    }
}
