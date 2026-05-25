package com.aura.p2p.discovery

import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.transport.PeerIdentity
import com.aura.p2p.transport.wire.PresenceRecord
import com.aura.records.canonical.encodeCanonical

/**
 * Verifies the ECDSA P-256 signature carried by a [PresenceRecord] against the
 * public key embedded in its own [PresenceRecord.did] (a did:key zDn... string).
 *
 * A presence record is self-asserting: the signer claims a DID and signs the
 * record with the matching key. Verification re-derives the signed bytes
 * ([encodeCanonical], which omits the signature) and checks them against the
 * P-256 point decoded from the DID. This proves the record was produced by the
 * holder of that DID's private key — it does **not** prove the DID maps to any
 * particular real person (that is out of scope for discovery).
 */
object PresenceVerifier {

    /**
     * @return true iff [record] carries a non-empty signature that verifies under
     * the key in its DID. A malformed DID or empty signature returns false
     * (drop the record) rather than throwing.
     */
    fun verify(record: PresenceRecord, verifier: SignatureVerifier): Boolean {
        if (record.signature.isEmpty()) return false
        val publicKey = try {
            PeerIdentity.p256FromDidKey(record.did)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return verifier.verify(publicKey, encodeCanonical(record), record.signature)
    }
}
