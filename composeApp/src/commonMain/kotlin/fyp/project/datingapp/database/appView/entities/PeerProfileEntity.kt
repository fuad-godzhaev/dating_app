package fyp.project.datingapp.database.appView.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Peer profile row. Serves two roles depending on how it was populated:
 *
 *  1. AppView discovery cache — lightweight copy of a peer's profile the
 *     Home feature renders while scrolling. Populated via the existing
 *     DiscoveryDao write paths.
 *  2. Relay cache — when [RelayPolicy.put] admits an owner-signed envelope
 *     from another peer, the row's `bodyCiphertext` / `bodyNonce` /
 *     `cachedAt` / `expiresAt` / `sessionInteractionToken` fields are
 *     populated. The row can then be served back to other peers as a
 *     [fyp.project.datingapp.p2p.transport.wire.SignedEnvelope].
 *
 * The two roles share the same row so that a profile we discovered and one
 * we cached for relay are never accidentally duplicated. A row is "relay
 * cached" iff `cachedAt IS NOT NULL`.
 *
 * Schema version: v3 (added in AppDatabase v2 → v3 migration).
 */
@Entity(
    tableName = "peer_profiles",
    indices = [
        Index(value = ["age"]),
        Index(value = ["city"]),
        Index(value = ["lastSeenAt"]),
        Index(value = ["isBlocked"]),
        // v3: sweep + LRU queries over the relay-cache subset.
        Index(value = ["expiresAt"]),
        Index(value = ["lastServedAt"]),
    ]
)
data class PeerProfileEntity(
    @PrimaryKey
    val did: String,

    val displayName: String,
    val bio: String? = null,
    val age: Int? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    val interests: String? = null,

    val profileCid: String,
    val commitCid: String,
    val commitSignature: ByteArray,
    val signingKey: ByteArray,

    /**
     * Legacy plaintext CBOR column from schema v2. Relay-cached rows keep this
     * at its migration default (empty byte array) and use `bodyCiphertext`
     * instead; discovery-only rows still write here. Scheduled for removal
     * in a later migration once no reader paths depend on it.
     */
    @ColumnInfo(defaultValue = "X''")
    val verifiedCborBytes: ByteArray = ByteArray(0),

    val receivedAt: Long,
    val lastUpdatedAt: Long,
    val lastSeenAt: Long,

    val isBlocked: Boolean = false,

    // v3 relay-cache columns (nullable for rows populated by the discovery
    // path only; required for rows written by RelayPolicy.put).
    /**
     * Explicit signer DID. For relay-cached rows this is the AAD input to
     * cache encryption; for discovery-only rows this is redundant with `did`.
     * Stored explicitly so AAD stays stable even if [did] is ever normalized.
     */
    @ColumnInfo(defaultValue = "")
    val ownerDid: String = "",

    /** AEAD-sealed canonical CBOR record body. See CacheEncryption. */
    @ColumnInfo(defaultValue = "X''")
    val bodyCiphertext: ByteArray = ByteArray(0),

    /** 12-byte AES-GCM nonce paired with [bodyCiphertext]. */
    @ColumnInfo(defaultValue = "X''")
    val bodyNonce: ByteArray = ByteArray(0),

    /** epoch-ms of first admission into the relay cache. Null ⇒ not relay-cached. */
    val cachedAt: Long? = null,

    /** epoch-ms of most recent successful serve (for LRU eviction). */
    val lastServedAt: Long? = null,

    /** epoch-ms when the relay cache must drop this row (TTL + sweep). */
    val expiresAt: Long? = null,

    /** UUID of the session interaction token that authorised the put. */
    val sessionInteractionToken: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PeerProfileEntity) return false
        return did == other.did
    }
    override fun hashCode(): Int = did.hashCode()
}
