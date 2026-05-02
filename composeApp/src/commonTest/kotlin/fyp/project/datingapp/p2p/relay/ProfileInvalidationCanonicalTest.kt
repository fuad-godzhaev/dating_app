package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.transport.PeerIdentity
import fyp.project.datingapp.p2p.transport.wire.ProfileInvalidation
import fyp.project.datingapp.records.canonical.Cid
import fyp.project.datingapp.records.canonical.decodeProfileInvalidation
import fyp.project.datingapp.records.canonical.encodeInvalidationWire
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Wire round-trip + verification gate for [ProfileInvalidation]. Mirrors
 * `PresenceCanonicalTest`. Uses a real P-256 did:key golden vector so
 * [PeerIdentity.p256FromDidKey] can extract a key without platform crypto.
 */
class ProfileInvalidationCanonicalTest {

    private val did = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"
    private val body = byteArrayOf(10, 20, 30, 40, 50)
    private val cid = Cid.cidV1DagCbor(body)

    private fun sample(
        signature: ByteArray = byteArrayOf(1, 2, 3, 4),
        canonicalBytes: ByteArray = body,
        c: String = cid,
    ) = ProfileInvalidation(
        ownerDid = did,
        collection = "fyp.project.datingapp.records.profile",
        rkey = "self",
        cid = c,
        canonicalBytes = canonicalBytes,
        signature = signature,
        publishedAt = "2026-05-23T12:00:00Z",
    )

    @Test fun wire_roundTrip_recoversAllFields() {
        val inv = sample()
        val back = decodeProfileInvalidation(encodeInvalidationWire(inv))
        assertEquals(inv.type, back.type)
        assertEquals(inv.ownerDid, back.ownerDid)
        assertEquals(inv.collection, back.collection)
        assertEquals(inv.rkey, back.rkey)
        assertEquals(inv.cid, back.cid)
        assertContentEquals(inv.canonicalBytes, back.canonicalBytes)
        assertContentEquals(inv.signature, back.signature)
        assertEquals(inv.publishedAt, back.publishedAt)
    }

    @Test fun encoding_isDeterministic() {
        val inv = sample()
        assertContentEquals(encodeInvalidationWire(inv), encodeInvalidationWire(inv))
        assertContentEquals(
            encodeInvalidationWire(inv),
            encodeInvalidationWire(decodeProfileInvalidation(encodeInvalidationWire(inv))),
        )
    }

    @Test fun verify_passesForWellFormedInvalidation() {
        val fake = CapturingVerifier(result = true)
        assertTrue(ProfileInvalidationVerifier.verify(sample(), fake))
        // The verifier is handed the owner DID's key and the carried bytes.
        assertContentEquals(PeerIdentity.p256FromDidKey(did), fake.lastPublicKey)
        assertContentEquals(body, fake.lastData)
    }

    @Test fun verify_falseAndShortCircuitsOnEmptySignature() {
        val fake = CapturingVerifier(result = true)
        assertFalse(ProfileInvalidationVerifier.verify(sample(signature = ByteArray(0)), fake))
        assertEquals(0, fake.calls, "verifier must not run for an empty signature")
    }

    @Test fun verify_falseWhenCidDoesNotMatchContent() {
        // Claimed CID is not the content address of canonicalBytes -> reject before
        // touching the signature verifier (a forged body can't borrow a real CID).
        val fake = CapturingVerifier(result = true)
        val bad = sample(c = "bafyreiaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        assertFalse(ProfileInvalidationVerifier.verify(bad, fake))
        assertEquals(0, fake.calls)
    }

    @Test fun verify_falseWhenVerifierRejects() {
        assertFalse(ProfileInvalidationVerifier.verify(sample(), CapturingVerifier(result = false)))
    }

    @Test fun verify_falseOnMalformedDid() {
        val bad = sample().copy(ownerDid = "did:key:notvalid")
        assertFalse(ProfileInvalidationVerifier.verify(bad, CapturingVerifier(result = true)))
    }

    private class CapturingVerifier(private val result: Boolean) : SignatureVerifier {
        var lastPublicKey: ByteArray? = null
        var lastData: ByteArray? = null
        var calls = 0
        override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String): Boolean {
            calls++
            lastPublicKey = publicKey
            lastData = data
            return result
        }
    }
}
