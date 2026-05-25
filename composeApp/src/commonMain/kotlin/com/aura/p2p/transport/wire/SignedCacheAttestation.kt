package com.aura.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * When a peer starts caching another user's profile, it publishes one of
 * these to the DHT under the key
 *   sha256("com.aura.cache-holders.v1/" + targetDid)
 *
 * A fetcher looking up a profile whose owner is offline reads all
 * attestations for the target DID, picks a holder, and opens a
 * `/dateable/profile/1.0.0` stream to that [holderPeerId]. The attestation
 * itself proves nothing about record contents — it is only a
 * holder-self-promise that they will serve if asked. Any record returned is
 * still validated against the *owner's* signature at the fetcher.
 */
@Serializable
data class SignedCacheAttestation(
    @SerialName($$"$type") val type: String = "com.aura.p2p.cacheHolder",
    val holderDid: String,
    val targetDid: String,
    val holderPeerId: String,
    val expiresAt: Long,
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedCacheAttestation) return false
        return holderDid == other.holderDid &&
                targetDid == other.targetDid &&
                holderPeerId == other.holderPeerId &&
                expiresAt == other.expiresAt &&
                signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var h = holderDid.hashCode()
        h = 31 * h + targetDid.hashCode()
        h = 31 * h + holderPeerId.hashCode()
        h = 31 * h + expiresAt.hashCode()
        h = 31 * h + signature.contentHashCode()
        return h
    }
}
