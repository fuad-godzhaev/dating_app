package com.aura.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Advertised via both the Kademlia DHT (as a value under a geohash-derived
 * key) and GossipSub (on the per-geohash topic). Carries just enough data to
 * decide "is this candidate worth fetching the full profile for?" without the
 * cost of shipping the whole [com.aura.records.UserProfile].
 *
 * Fields in this record map 1:1 to §10 of `p2p-subsystem-design.md`.
 * Re-ordering, renaming, or adding fields must go through a wire-format
 * version bump — peers on older versions will silently drop unknown fields
 * but may reject payload size overruns (see the 1 KB budget in §15.3).
 */
@Serializable
data class PresenceRecord(
    @SerialName($$"$type") val type: String = "com.aura.p2p.presence",
    val did: String,
    // libp2p reachability (B4): the Ed25519 transport PeerId from the go host and
    // its current multiaddrs, so a peer can be dialled after a presence sighting.
    // did:key (P-256) is the identity; peerId is the transport address.
    val peerId: String,
    val multiaddrs: List<String>,
    val profileCid: String,
    val geohash: String,
    val interests: List<String>,
    val gender: String,
    val lookingFor: List<String>,
    val ageRange: AgeRange,
    val announcedAt: String,
    val ttl: Long,
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PresenceRecord) return false
        return did == other.did &&
                profileCid == other.profileCid &&
                announcedAt == other.announcedAt &&
                signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var h = did.hashCode()
        h = 31 * h + profileCid.hashCode()
        h = 31 * h + announcedAt.hashCode()
        h = 31 * h + signature.contentHashCode()
        return h
    }
}

@Serializable
data class AgeRange(val min: Int, val max: Int) {
    init {
        require(min in 18..120) { "min age must be 18..120, got $min" }
        require(max in min..120) { "max age must be >= min and <= 120, got $max (min=$min)" }
    }
}
