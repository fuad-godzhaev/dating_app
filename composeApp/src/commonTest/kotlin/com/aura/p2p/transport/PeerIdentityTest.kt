package com.aura.p2p.transport

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PeerIdentityTest {

    /** 33-byte compressed P-256 point: [prefix] (0x02/0x03) + 32-byte X. */
    private fun point(prefix: Int, x: (Int) -> Byte): ByteArray =
        byteArrayOf(prefix.toByte()) + ByteArray(32, x)

    @Test fun roundTrip_fixedKeys() {
        val keys = listOf(
            point(0x02) { 0 },                                  // even-y, X all zero
            point(0x03) { 0xFF.toByte() },                      // odd-y, X all ones
            point(0x02) { it.toByte() },                        // 0..31 ramp
            point(0x03) { ((it * 7 + 3) and 0xFF).toByte() },   // arbitrary pattern
        )
        for (k in keys) {
            val did = PeerIdentity.didKeyFromP256(k)
            assertContentEquals(k, PeerIdentity.p256FromDidKey(did))
        }
    }

    @Test fun p256DidKey_startsWith_zDn() {
        // Hard invariant: p256-pub multicodec (0x80 0x24) + base58btc always yields "zDn".
        val did = PeerIdentity.didKeyFromP256(point(0x02) { it.toByte() })
        assertTrue(did.startsWith("did:key:zDn"), "got $did")
    }

    @Test fun leadingZeroX_roundTrips() {
        val k = point(0x03) { 0 }.also { it[32] = 9 } // X = 0x00..00 09
        val did = PeerIdentity.didKeyFromP256(k)
        assertContentEquals(k, PeerIdentity.p256FromDidKey(did))
    }

    @Test fun rejects_wrongSizeInput() {
        assertFailsWith<IllegalArgumentException> { PeerIdentity.didKeyFromP256(ByteArray(32)) }
    }

    @Test fun rejects_uncompressedPrefix() {
        assertFailsWith<IllegalArgumentException> { PeerIdentity.didKeyFromP256(point(0x04) { 0 }) }
    }

    @Test fun rejects_missingPrefix() {
        assertFailsWith<IllegalArgumentException> { PeerIdentity.p256FromDidKey("did:key:Q3shabc") }
    }

    @Test fun rejects_malformedPayload() {
        // valid "did:key:z" prefix but the base58 body decodes to the wrong length.
        assertFailsWith<IllegalArgumentException> { PeerIdentity.p256FromDidKey("did:key:zABC") }
    }

    @Test fun goldenVectors_specStrings_decodeAndReEncode() {
        // Externally-published P-256 did:key / Multikey strings (the did:key body IS
        // the publicKeyMultibase value). Decoding then re-encoding must reproduce the
        // exact canonical string -> anchors base58btc + the p256-pub multicodec
        // against independent references. A 3rd distinct vector can be added later.
        val golden = listOf(
            // AT Protocol cryptography spec (atproto.com/specs/cryptography).
            "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo",
            // W3C did:key spec + W3C vc-di-ecdsa (same value, cross-corroborated).
            "did:key:zDnaerx9CtbPJ1q36T5Ln5wYt3MQYeGRG5ehnPAmxcf5mDZpv",
        )
        for (did in golden) {
            val point = PeerIdentity.p256FromDidKey(did)
            assertEquals(33, point.size, "compressed point length for $did")
            assertTrue(point[0] == 0x02.toByte() || point[0] == 0x03.toByte(), "compressed prefix for $did")
            assertEquals(did, PeerIdentity.didKeyFromP256(point), "re-encode round-trip for $did")
        }
    }
}
