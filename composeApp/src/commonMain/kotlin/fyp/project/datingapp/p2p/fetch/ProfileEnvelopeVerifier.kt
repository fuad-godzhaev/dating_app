package fyp.project.datingapp.p2p.fetch

import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.transport.PeerIdentity
import fyp.project.datingapp.p2p.transport.wire.SignedEnvelope
import fyp.project.datingapp.records.canonical.Cid

/**
 * The fetch-side verification gate (`p2p-subsystem-design.md` §7). A
 * [SignedEnvelope] returned by any cascade source (owner, local cache, or a
 * cacheHolder) is only trusted after this passes — a holder can withhold a
 * record but cannot fabricate one.
 *
 * Three checks, all required:
 *  1. The claimed `cid` is the real content address of `canonicalBytes`
 *     (`Cid.cidV1DagCbor`) — so a relay can't swap the body under a known CID.
 *  2. The ECDSA P-256 signature verifies over `canonicalBytes` under the key
 *     embedded in `ownerDid` (same path as Phase C presence).
 *  3. If an [expectedCid] hint is supplied (e.g. from a `PresenceRecord`), the
 *     envelope's CID must equal it — so we get the version we asked for.
 */
object ProfileEnvelopeVerifier {

    fun verify(
        envelope: SignedEnvelope,
        verifier: SignatureVerifier,
        expectedCid: String? = null,
    ): Boolean {
        if (envelope.signature.isEmpty()) return false
        if (expectedCid != null && envelope.cid != expectedCid) return false
        if (Cid.cidV1DagCbor(envelope.canonicalBytes) != envelope.cid) return false
        val publicKey = try {
            PeerIdentity.p256FromDidKey(envelope.ownerDid)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return verifier.verify(publicKey, envelope.canonicalBytes, envelope.signature)
    }
}
