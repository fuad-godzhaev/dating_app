package com.aura.p2p.discovery

import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.transport.PeerIdentity
import com.aura.p2p.transport.wire.AgeRange
import com.aura.p2p.transport.wire.PresenceRecord
import com.aura.records.canonical.decodePresenceRecord
import com.aura.records.canonical.encodeCanonical
import com.aura.records.canonical.encodePresenceWire
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresenceCanonicalTest {

    // A real P-256 did:key (AT Protocol cryptography spec golden vector). Its
    // embedded point decodes via PeerIdentity.p256FromDidKey, so PresenceVerifier
    // can extract a valid public key without platform crypto.
    private val did = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"

    private fun sample(signature: ByteArray = byteArrayOf(1, 2, 3, 4)) = PresenceRecord(
        did = did,
        peerId = "12D3KooWSamplePeerIdForTestingOnly000000000000000000",
        multiaddrs = listOf("/ip4/10.0.2.16/tcp/4001", "/ip6/::1/tcp/4001"),
        profileCid = "bafyreieabc123",
        geohash = "gc7x3",
        interests = listOf("climbing", "books"),
        gender = "female",
        lookingFor = listOf("male", "nonbinary"),
        ageRange = AgeRange(min = 25, max = 35),
        announcedAt = "2026-05-23T12:00:00Z",
        ttl = 900_000L,
        signature = signature,
    )

    @Test fun wire_roundTrip_recoversAllFields() {
        val r = sample()
        val back = decodePresenceRecord(encodePresenceWire(r))
        assertEquals(r.type, back.type)
        assertEquals(r.did, back.did)
        assertEquals(r.peerId, back.peerId)
        assertEquals(r.multiaddrs, back.multiaddrs)
        assertEquals(r.profileCid, back.profileCid)
        assertEquals(r.geohash, back.geohash)
        assertEquals(r.interests, back.interests)
        assertEquals(r.gender, back.gender)
        assertEquals(r.lookingFor, back.lookingFor)
        assertEquals(r.ageRange, back.ageRange)
        assertEquals(r.announcedAt, back.announcedAt)
        assertEquals(r.ttl, back.ttl)
        assertContentEquals(r.signature, back.signature)
    }

    @Test fun encoding_isDeterministic() {
        val r = sample()
        assertContentEquals(encodePresenceWire(r), encodePresenceWire(r))
        assertContentEquals(encodeCanonical(r), encodeCanonical(r))
        // Re-encoding a decoded record reproduces the wire bytes (byte stability).
        assertContentEquals(encodePresenceWire(r), encodePresenceWire(decodePresenceRecord(encodePresenceWire(r))))
    }

    @Test fun signableBytes_omitSignature() {
        // The signed bytes must not depend on the signature field: changing only
        // the signature leaves encodeCanonical unchanged, but changes the wire form.
        val a = sample(signature = byteArrayOf(1, 2, 3))
        val b = sample(signature = byteArrayOf(9, 9, 9, 9))
        assertContentEquals(encodeCanonical(a), encodeCanonical(b))
        assertTrue(!encodePresenceWire(a).contentEquals(encodePresenceWire(b)))
    }

    @Test fun verifier_receivesDidKeyAndSignableBytes() {
        val r = sample()
        val fake = CapturingVerifier(result = true)
        assertTrue(PresenceVerifier.verify(r, fake))
        assertContentEquals(PeerIdentity.p256FromDidKey(did), fake.lastPublicKey)
        assertContentEquals(encodeCanonical(r), fake.lastData)
        assertContentEquals(r.signature, fake.lastSignature)
    }

    @Test fun verify_falseWhenVerifierRejects() {
        assertFalse(PresenceVerifier.verify(sample(), CapturingVerifier(result = false)))
    }

    @Test fun verify_falseAndShortCircuitsOnEmptySignature() {
        val fake = CapturingVerifier(result = true)
        assertFalse(PresenceVerifier.verify(sample(signature = ByteArray(0)), fake))
        assertEquals(0, fake.calls, "verifier must not be called for an empty signature")
    }

    @Test fun verify_falseOnMalformedDid() {
        val bad = sample().copy(did = "did:key:notvalid")
        assertFalse(PresenceVerifier.verify(bad, CapturingVerifier(result = true)))
    }

    @Test fun tampering_changesSignedBytes() {
        // A verifier that only accepts the original signed bytes rejects any
        // record whose signable content was altered after signing.
        val original = sample()
        val signedBytes = encodeCanonical(original)
        val pinned = object : SignatureVerifier {
            override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String) =
                data.contentEquals(signedBytes)
        }
        assertTrue(PresenceVerifier.verify(original, pinned))
        val tampered = original.copy(geohash = "zzzzz")
        assertFalse(PresenceVerifier.verify(tampered, pinned))
    }

    private class CapturingVerifier(private val result: Boolean) : SignatureVerifier {
        var lastPublicKey: ByteArray? = null
        var lastData: ByteArray? = null
        var lastSignature: ByteArray? = null
        var calls = 0
        override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String): Boolean {
            calls++
            lastPublicKey = publicKey
            lastData = data
            lastSignature = signature
            return result
        }
    }
}
