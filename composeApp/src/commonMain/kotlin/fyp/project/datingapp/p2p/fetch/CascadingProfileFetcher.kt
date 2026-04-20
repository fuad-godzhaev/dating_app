package fyp.project.datingapp.p2p.fetch

import fyp.project.datingapp.database.appView.dao.DiscoveryDao
import fyp.project.datingapp.database.appView.entities.PeerProfileEntity
import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.discovery.PeerDirectory
import fyp.project.datingapp.p2p.relay.EpochClock
import fyp.project.datingapp.p2p.relay.SystemClock
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchRequest
import fyp.project.datingapp.p2p.transport.wire.SignedEnvelope
import fyp.project.datingapp.records.UserProfile
import fyp.project.datingapp.records.canonical.decodeUserProfile

/**
 * The four-step fetch cascade (`p2p-subsystem-design.md` §7.1). First success
 * wins; every networked/cached hit passes [ProfileEnvelopeVerifier] before it is
 * returned, and a record that fails verification does **not** advance the cascade
 * (the next source is tried). Verified direct-P2P hits are written to the local
 * AppView cache so the next fetch is offline.
 *
 * Dependencies are narrow seams (lambdas + interfaces) rather than the concrete
 * `RepositoryManager` / `Libp2pTransport`, so the cascade is unit-testable with
 * fakes (the transport is an `expect class` and can't be faked directly).
 */
class CascadingProfileFetcher(
    private val selfDid: suspend () -> String?,
    private val ownEnvelope: suspend () -> SignedEnvelope?,
    private val cache: DiscoveryDao,
    private val peerDirectory: PeerDirectory,
    private val streamClient: ProfileStreamClient,
    private val verifier: SignatureVerifier,
    private val clock: EpochClock = SystemClock,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
) : ProfileFetcher {

    override suspend fun fetch(did: String, expectedCid: String?): UserProfile? =
        fetchSigned(did, expectedCid)?.let { decodeUserProfile(it.canonicalBytes) }

    override suspend fun fetchSigned(did: String, expectedCid: String?): SignedEnvelope? {
        // 1. Own PDS — it's ours, no verification needed; honour the CID hint.
        if (did == selfDid()) {
            val own = ownEnvelope()
            return own?.takeIf { expectedCid == null || it.cid == expectedCid }
        }

        // 2. Local AppView cache (non-expired + CID match, re-verified).
        cacheLookup(did, expectedCid)?.let { return it }

        // 3. Direct P2P to the owner (peer learned from discovery).
        val contact = peerDirectory.get(did)
        if (contact != null) {
            // ifNotCid = a CID we already hold and want to avoid re-downloading.
            // We reached step 3 only because the local cache missed, so we hold
            // nothing usable -> request the full body (null), not expectedCid.
            val response = runCatching {
                streamClient.request(contact, ProfileFetchRequest(targetDid = did, ifNotCid = null))
            }.getOrNull()
            val envelope = response?.record
            if (envelope != null && ProfileEnvelopeVerifier.verify(envelope, verifier, expectedCid)) {
                runCatching { cachePut(envelope) }
                return envelope
            }
        }

        // 4. cacheHolders fallback — TODO(Phase F): look up SignedCacheAttestation
        // holders on the DHT and ask one. Inert until attestations are published.
        return null
    }

    private suspend fun cacheLookup(did: String, expectedCid: String?): SignedEnvelope? {
        val row = cache.getProfileByDid(did) ?: return null
        if (row.verifiedCborBytes.isEmpty() || row.commitSignature.isEmpty()) return null
        if (expectedCid != null && row.profileCid != expectedCid) return null
        if (clock.nowMs() - row.lastUpdatedAt > cacheTtlMs) return null
        val envelope = SignedEnvelope(
            collection = COLLECTION_PROFILE,
            rkey = RKEY_SELF,
            ownerDid = row.ownerDid.ifEmpty { row.did },
            cid = row.profileCid,
            canonicalBytes = row.verifiedCborBytes,
            signature = row.commitSignature,
        )
        return if (ProfileEnvelopeVerifier.verify(envelope, verifier, expectedCid)) envelope else null
    }

    private suspend fun cachePut(envelope: SignedEnvelope) {
        val profile = decodeUserProfile(envelope.canonicalBytes)
        val now = clock.nowMs()
        val existing = cache.getProfileByDid(envelope.ownerDid)
        cache.upsertProfile(
            PeerProfileEntity(
                did = envelope.ownerDid,
                displayName = profile.displayName,
                bio = profile.bio,
                age = profile.age,
                city = existing?.city,
                latitude = profile.location?.latitude,
                longitude = profile.location?.longitude,
                geohash = profile.location?.geohash,
                interests = profile.interests.joinToString(","),
                profileCid = envelope.cid,
                commitCid = envelope.cid,
                commitSignature = envelope.signature,
                signingKey = profile.signingKey,
                verifiedCborBytes = envelope.canonicalBytes,
                ownerDid = envelope.ownerDid,
                receivedAt = existing?.receivedAt ?: now,
                lastUpdatedAt = now,
                lastSeenAt = now,
                isBlocked = existing?.isBlocked ?: false,
            ),
        )
    }

    companion object {
        /** Local fetch-cache freshness window (30 min). Beyond this, re-fetch. */
        const val DEFAULT_CACHE_TTL_MS: Long = 30L * 60 * 1000

        // Collection/rkey for a profile record. Value is metadata only — the
        // verification gate keys off ownerDid + canonicalBytes, not these.
        private const val COLLECTION_PROFILE = "fyp.project.datingapp.records.profile"
        private const val RKEY_SELF = "self"
    }
}
