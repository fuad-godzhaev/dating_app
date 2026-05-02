package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.database.appView.dao.DiscoveryDao
import fyp.project.datingapp.database.appView.entities.PeerProfileEntity
import fyp.project.datingapp.p2p.transport.wire.ProfileInvalidation
import fyp.project.datingapp.p2p.transport.wire.SignedEnvelope
import fyp.project.datingapp.records.canonical.Cid

/**
 * Orchestrates the five defenses that gate relay-cache ingestion, decoding
 * and serving. Phase A lives in-process with no networking — libp2p wires in
 * later through [GossipSubInvalidator] without changing this class.
 *
 * The five defenses (`p2p-subsystem-design.md` §8.2, verbatim):
 *   1. **Non-self**: never cache envelopes we signed ourselves. Prevents the
 *      user's own device turning its PDS into a re-publication surface.
 *   2. **Rate limit**: dual token bucket (5/min, 500/day) silently drops
 *      machine-speed ingestion bursts.
 *   3. **Session interaction token**: one-shot 30-s tokens minted by the UI
 *      on `Msg.PictureLoaded`. A background process that never renders a
 *      card cannot mint tokens, so it cannot populate the cache.
 *   4. **Capacity cap**: reputation-gated (Phase A stubbed at 20) LRU. When
 *      the cache is full, the oldest-served row is evicted to make room.
 *   5. **AEAD at rest**: canonical bytes are sealed with AES-GCM bound via
 *      AAD to the owner DID + CID. A ciphertext swap across rows fails the
 *      GCM tag check, which [open] collapses to a cache miss.
 *
 * `put` never throws: every failure path drops the envelope silently and
 * returns `false`, mirroring the design-intent that the network side cannot
 * distinguish between rejection reasons (an attacker probing the reason
 * codes gains no information).
 */
class RelayPolicy(
    private val selfDid: suspend () -> String?,
    private val discoveryDao: DiscoveryDao,
    private val rateLimiter: IngestRateLimiter,
    private val sessionTokens: SessionInteractionTokens,
    private val reputation: ReputationScorer,
    private val encryption: CacheCipher,
    private val clock: EpochClock = SystemClock,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    // Phase E: a lazy provider (not a direct reference) breaks the RelayPolicy
    // <-> DefaultGossipSubInvalidator construction cycle in the DI graph. Default
    // returns null so tests / early-phase builds construct a policy with no
    // transport stack.
    private val invalidator: () -> GossipSubInvalidator? = { null },
    // Phase F: lazy provider for the cacheHolder advertiser (DHT provider record on
    // cache). Lazy keeps RelayPolicy transport-free at construction; null in tests.
    private val advertiser: () -> CacheHolderAdvertiser? = { null },
) {

    /**
     * Ingest an envelope received from a peer. Returns `true` only if the
     * row was actually written — all five defenses passed. Otherwise returns
     * `false` without leaking which check failed.
     */
    suspend fun put(envelope: SignedEnvelope, sessionToken: String?): Boolean {
        // 1. Non-self.
        if (envelope.ownerDid == selfDid()) return false

        // 2. Rate limit. Checked before the session-token consume so replayed
        // tokens under a rate storm don't each burn a token-map entry.
        if (!rateLimiter.tryConsume()) return false

        // 3. Session interaction token. One-shot: consume even on later
        // failure paths is intentional — a token represents "the user saw
        // this card once", which doesn't entitle multiple writes.
        if (!sessionTokens.consume(sessionToken)) return false

        // 4. Capacity cap via LRU eviction.
        val capacity = reputation.currentCapacity()
        val currentCount = discoveryDao.countCached()
        if (currentCount >= capacity) {
            val toEvict = (currentCount - capacity + 1).coerceAtLeast(1)
            discoveryDao.pickOldestByLastServed(toEvict).forEach { victimDid ->
                discoveryDao.deleteByDid(victimDid)
                // Stop tracking invalidations for a row we no longer hold.
                invalidator()?.unsubscribe(victimDid)
            }
        }

        // 5. AEAD at rest, AAD-bound to owner+CID.
        val aad = relayAad(envelope.ownerDid, envelope.cid)
        val sealed = encryption.seal(envelope.canonicalBytes, aad)
        val now = clock.nowMs()

        // Preserve a row's existing discovery metadata if one already exists;
        // RelayPolicy only populates relay-cache columns. A merge at the
        // application layer decides how to update discovery fields.
        val existing = discoveryDao.getProfileByDid(envelope.ownerDid)
        val row = (existing ?: skeletonRow(envelope, now)).copy(
            ownerDid = envelope.ownerDid,
            profileCid = envelope.cid,
            commitSignature = envelope.signature,
            bodyCiphertext = sealed.ciphertext,
            bodyNonce = sealed.nonce,
            cachedAt = existing?.cachedAt ?: now,
            lastServedAt = now,
            expiresAt = now + ttlMs,
            sessionInteractionToken = sessionToken,
        )
        discoveryDao.upsertProfile(row)
        // Phase E: track this owner's invalidation topic so a later profile update
        // refreshes the cached row without a re-fetch. Idempotent in the invalidator.
        invalidator()?.subscribe(envelope.ownerDid)
        // Phase F: advertise (DHT provider record) that this device now serves this
        // owner's profile, so offline-owner fetches can fall back to us. Best-effort:
        // a not-yet-ready DHT just means no provider record this round.
        runCatching { advertiser()?.advertise(envelope.ownerDid) }
        return true
    }

    /**
     * Serve a relay-cached envelope back to a caller (i.e. this device
     * acting as a cacheHolder). Returns null for any of:
     *   - no row for [did]
     *   - row is not relay-cached (`cachedAt IS NULL`)
     *   - row is past [PeerProfileEntity.expiresAt]
     *   - AEAD tag fails (tampered ciphertext or AAD)
     *
     * On hit, bumps `lastServedAt` so the LRU eviction keeps hot rows alive.
     */
    suspend fun get(did: String): SignedEnvelope? {
        val row = discoveryDao.getProfileByDid(did) ?: return null
        // TODO: `cachedAt` is bound here only for the null guard, then never read
        // again in this method. Decide whether the binding documents anything
        // worth keeping or whether this collapses to `if (row.cachedAt == null)
        // return null`. The .also block at the end of this function exists only
        // to keep the binding "used"; resolving this comment lets it go too.
        val cachedAt = row.cachedAt ?: return null
        val expiresAt = row.expiresAt
        val now = clock.nowMs()
        if (expiresAt != null && expiresAt < now) return null

        // TODO: `PeerProfileEntity.ownerDid` should be non-empty whenever a
        // relay-cached row exists — `put` writes it from `envelope.ownerDid`,
        // and the AAD seal here uses it. An empty value indicates a schema
        // invariant violation (likely a pre-migration row that escaped a
        // backfill). Surfacing it as a `check` rather than papering over with
        // `.ifEmpty { row.did }` so the bug is loud.
        check(row.ownerDid.isNotEmpty()) {
            "PeerProfileEntity.ownerDid is empty for did=${row.did}; " +
                "schema invariant violated (relay-cached rows must carry a populated ownerDid)"
        }
        val ownerDid = row.ownerDid

        val aad = relayAad(ownerDid, row.profileCid)
        val plaintext = encryption.open(row.bodyCiphertext, row.bodyNonce, aad) ?: return null

        discoveryDao.bumpLastServed(did, now)
        return SignedEnvelope(
            collection = COLLECTION_PROFILE,
            rkey = RKEY_SELF,
            ownerDid = ownerDid,
            cid = row.profileCid,
            canonicalBytes = plaintext,
            signature = row.commitSignature,
        ).also { _ ->
            // Silence unused-variable warnings for cachedAt; kept as an
            // explicit binding to document the contract. See the TODO above.
            @Suppress("UNUSED_EXPRESSION") cachedAt
        }
    }

    /**
     * Drop hook: delete the cached row for [ownerDid] and stop tracking its
     * invalidation topic. Used when an invalidation says "this record is gone"
     * rather than "here is the new version" (see [onInvalidationReplace]).
     * Phase A tests call it directly.
     */
    suspend fun onInvalidation(ownerDid: String) {
        discoveryDao.deleteByDid(ownerDid)
        invalidator()?.unsubscribe(ownerDid)
    }

    /**
     * Replace hook (Phase E, `p2p-subsystem-design.md` §8.4). Called by
     * [GossipSubInvalidator] after it has verified the push against the owner's
     * key. Swaps the cached envelope for the new version *in place*, re-sealing
     * the carried [ProfileInvalidation.canonicalBytes] under a fresh AEAD nonce
     * and resetting the TTL — no pull round-trip.
     *
     * Returns true only when a row was actually replaced. No-ops (return false) if:
     *   - we don't relay-cache this owner (or the row isn't relay-cached),
     *   - the carried bytes don't hash to the claimed CID (integrity guard),
     *   - we already hold this exact version (same CID) — avoids reseal churn.
     *
     * The caller ([GossipSubInvalidator]) is responsible for signature
     * verification; this method trusts a verified invalidation, mirroring how
     * [put] trusts an already-verified envelope from the fetch gate.
     */
    suspend fun onInvalidationReplace(invalidation: ProfileInvalidation): Boolean {
        val existing = discoveryDao.getProfileByDid(invalidation.ownerDid) ?: return false
        if (existing.cachedAt == null) return false
        if (Cid.cidV1DagCbor(invalidation.canonicalBytes) != invalidation.cid) return false
        if (existing.profileCid == invalidation.cid) return false

        val aad = relayAad(invalidation.ownerDid, invalidation.cid)
        val sealed = encryption.seal(invalidation.canonicalBytes, aad)
        val now = clock.nowMs()
        discoveryDao.upsertProfile(
            existing.copy(
                profileCid = invalidation.cid,
                commitCid = invalidation.cid,
                commitSignature = invalidation.signature,
                bodyCiphertext = sealed.ciphertext,
                bodyNonce = sealed.nonce,
                lastUpdatedAt = now,
                lastServedAt = now,
                expiresAt = now + ttlMs,
            ),
        )
        return true
    }

    private fun skeletonRow(envelope: SignedEnvelope, now: Long): PeerProfileEntity =
        PeerProfileEntity(
            did = envelope.ownerDid,
            displayName = "",
            profileCid = envelope.cid,
            commitCid = envelope.cid,
            commitSignature = envelope.signature,
            signingKey = ByteArray(0),
            receivedAt = now,
            lastUpdatedAt = now,
            lastSeenAt = now,
        )

    companion object {
        /** 7 days — matches the "fresh peer" window in `p2p-subsystem-design.md` §8.2. */
        const val DEFAULT_TTL_MS: Long = 7L * 24 * 60 * 60 * 1000

        /** Collection / rkey for the single profile record each DID publishes. */
        const val COLLECTION_PROFILE = "fyp.project.datingapp.profile"
        const val RKEY_SELF = "self"
    }
}
