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

    // ---- raw-leaf CID (Part 3, blobs) -------------------------------------

    @Test fun rawCid_starts_with_bafkrei_prefix() {
        // raw multicodec is 0x55; bytes 0x01 0x55 0x12 base32-encode such that a
        // raw CIDv1 begins "bafkrei" (distinct from dag-cbor's "bafyrei").
        val cid = Cid.cidV1Raw(byteArrayOf(0x00))
        assertTrue(cid.startsWith("bafkrei"), "cid was $cid")
    }

    @Test fun rawCid_is_deterministic_and_input_sensitive() {
        assertEquals(Cid.cidV1Raw(byteArrayOf(1, 2, 3)), Cid.cidV1Raw(byteArrayOf(1, 2, 3)))
        assertTrue(Cid.cidV1Raw(byteArrayOf(1, 2, 3)) != Cid.cidV1Raw(byteArrayOf(1, 2, 4)))
    }

    @Test fun rawCid_differs_from_dagCbor_forSameBytes() {
        // Same content, different multicodec -> different CID. Prevents a blob CID
        // from ever colliding with a dag-cbor record CID over the same bytes.
        val bytes = byteArrayOf(9, 8, 7, 6)
        assertTrue(Cid.cidV1Raw(bytes) != Cid.cidV1DagCbor(bytes))
    }
}
