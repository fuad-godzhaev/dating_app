package fyp.project.datingapp.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pushed by the owner on the per-DID GossipSub topic
 * `fyp.project.datingapp.invalidate/v1/<did>` whenever they publish a new
 * version of a record. Cachers subscribed to that topic consume this message,
 * verify [signature] against the known owner public key, and atomically
 * swap the cached row for [canonicalBytes] — all without a pull round-trip.
 *
 * Logically an out-of-band [SignedEnvelope] plus a publish timestamp: the
 * extra timestamp lets receivers ignore late-arriving duplicates.
 */
@Serializable
data class ProfileInvalidation(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.invalidate",
    val ownerDid: String,
    val collection: String,
    val rkey: String,
    val cid: String,
    val canonicalBytes: ByteArray,
    val signature: ByteArray,
    val publishedAt: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfileInvalidation) return false
        return ownerDid == other.ownerDid &&
                cid == other.cid &&
                canonicalBytes.contentEquals(other.canonicalBytes) &&
                signature.contentEquals(other.signature) &&
                publishedAt == other.publishedAt
    }

    override fun hashCode(): Int {
        var h = ownerDid.hashCode()
        h = 31 * h + cid.hashCode()
        h = 31 * h + canonicalBytes.contentHashCode()
        h = 31 * h + signature.contentHashCode()
        h = 31 * h + publishedAt.hashCode()
        return h
    }
}
