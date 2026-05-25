package com.aura.p2p.relay

import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.transport.PeerIdentity
import com.aura.p2p.transport.wire.ProfileInvalidation
import com.aura.records.canonical.Cid

/**
 * Verification gate for a [ProfileInvalidation] pushed over GossipSub
 * (`p2p-subsystem-design.md` §8.4). A holder must not act on an invalidation it
 * cannot prove the owner produced — otherwise any peer could evict or downgrade
 * other peers' caches. Mirrors [com.aura.p2p.fetch.ProfileEnvelopeVerifier]:
 * the invalidation is, on the wire, an out-of-band signed envelope.
 *
 * Three checks, all required:
 *  1. Non-empty signature.
 *  2. The claimed `cid` is the real content address of `canonicalBytes`
 *     (`Cid.cidV1DagCbor`) — so a peer can't bind a forged body to a known CID.
 *  3. The ECDSA P-256 signature verifies over `canonicalBytes` under the key
 *     embedded in `ownerDid` — proving the owner published this exact content.
 */
object ProfileInvalidationVerifier {

    fun verify(invalidation: ProfileInvalidation, verifier: SignatureVerifier): Boolean {
        if (invalidation.signature.isEmpty()) return false
        if (Cid.cidV1DagCbor(invalidation.canonicalBytes) != invalidation.cid) return false
        val publicKey = try {
            PeerIdentity.p256FromDidKey(invalidation.ownerDid)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return verifier.verify(publicKey, invalidation.canonicalBytes, invalidation.signature)
    }
}
