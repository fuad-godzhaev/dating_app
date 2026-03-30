package fyp.project.datingapp.records.canonical

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CidTest {

    @Test fun cid_starts_with_bafyrei_prefix() {
        // Any CIDv1 / dag-cbor / sha2-256 will share the first three binary
        // bytes 0x01 0x71 0x12, which base32-encode to "afyrei" after the 'b'
        // multibase prefix. If this test fails, one of the constant bytes
        // in Cid.kt is wrong.
        val cid = Cid.cidV1DagCbor(byteArrayOf(0x00))
        assertTrue(cid.startsWith("bafyrei"), "cid was $cid")
    }

    @Test fun cid_is_deterministic() {
        val a = Cid.cidV1DagCbor(byteArrayOf(1, 2, 3))
        val b = Cid.cidV1DagCbor(byteArrayOf(1, 2, 3))
        assertEquals(a, b)
    }

    @Test fun cid_changes_with_input() {
        val a = Cid.cidV1DagCbor(byteArrayOf(1, 2, 3))
        val b = Cid.cidV1DagCbor(byteArrayOf(1, 2, 4))
        assertTrue(a != b, "different inputs must produce different CIDs")
    }

    @Test fun cid_is_base32_lower_no_padding() {
        // Characters outside the base32-lower alphabet (plus leading 'b') would
        // indicate a regression in the encoder.
        val alphabet = ('a'..'z').toSet() + ('2'..'7').toSet()
        val cid = Cid.cidV1DagCbor(byteArrayOf(42))
        assertEquals('b', cid[0])
        for (ch in cid.substring(1)) {
            assertTrue(ch in alphabet, "unexpected char '$ch' in cid $cid")
        }
    }

    @Test fun cid_fixed_length() {
        // 36 binary bytes → 58 base32 chars (ceil(36*8/5)) + 'b' = 59.
        val cid = Cid.cidV1DagCbor(ByteArray(10))
        assertEquals(59, cid.length)
    }
}
