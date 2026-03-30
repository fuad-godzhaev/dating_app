package fyp.project.datingapp.records.canonical

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Golden vectors for [CanonicalEncoder]. Values below are hand-verified
 * against RFC 8949 §A (appendix) to pin the exact wire bytes for small,
 * canonical inputs. Any accidental regression that changes the encoding
 * flips one of these tests.
 */
class CanonicalEncoderTest {

    @Test fun integer_zero() {
        assertEnc(byteArrayOf(0x00), CborValue.CInt(0L))
    }

    @Test fun integer_small_positive() {
        // 23 fits in the 5-bit immediate field → single byte 0x17.
        assertEnc(byteArrayOf(0x17), CborValue.CInt(23L))
        // 24 crosses into the one-byte argument form → 0x18, 0x18.
        assertEnc(byteArrayOf(0x18, 0x18), CborValue.CInt(24L))
        // 100 → 0x18, 0x64.
        assertEnc(byteArrayOf(0x18, 0x64), CborValue.CInt(100L))
    }

    @Test fun integer_two_byte_boundary() {
        // 255 is the last value that fits in one argument byte.
        assertEnc(byteArrayOf(0x18, 0xFF.toByte()), CborValue.CInt(255L))
        // 256 pushes into the 2-byte form.
        assertEnc(byteArrayOf(0x19, 0x01, 0x00), CborValue.CInt(256L))
    }

    @Test fun integer_negative() {
        // -1 → major 1 arg 0 → 0x20.
        assertEnc(byteArrayOf(0x20), CborValue.CInt(-1L))
        // -100 → major 1 arg 99 → 0x38, 0x63.
        assertEnc(byteArrayOf(0x38, 0x63), CborValue.CInt(-100L))
    }

    @Test fun text_string() {
        // "a" → 0x61, 0x61.
        assertEnc(byteArrayOf(0x61, 'a'.code.toByte()), CborValue.CString("a"))
        // Empty string → 0x60.
        assertEnc(byteArrayOf(0x60), CborValue.CString(""))
    }

    @Test fun byte_string() {
        assertEnc(byteArrayOf(0x44, 0x01, 0x02, 0x03, 0x04), CborValue.CBytes(byteArrayOf(1, 2, 3, 4)))
    }

    @Test fun empty_array_and_map() {
        assertEnc(byteArrayOf(0x80.toByte()), CborValue.CArray(emptyList()))
        assertEnc(byteArrayOf(0xA0.toByte()), CborValue.CMap(emptyMap()))
    }

    @Test fun map_keys_are_length_first_sorted() {
        // "b" (length 1) must come before "aa" (length 2), regardless of source order.
        val m = CborValue.CMap(mapOf("aa" to CborValue.CInt(2L), "b" to CborValue.CInt(1L)))
        val expected = byteArrayOf(
            0xA2.toByte(),                // map(2)
            0x61, 'b'.code.toByte(), 0x01, // key "b" → 1
            0x62, 'a'.code.toByte(), 'a'.code.toByte(), 0x02, // key "aa" → 2
        )
        assertEnc(expected, m)
    }

    @Test fun map_keys_same_length_sort_lexicographically() {
        val m = CborValue.CMap(mapOf("b" to CborValue.CInt(2L), "a" to CborValue.CInt(1L)))
        val expected = byteArrayOf(
            0xA2.toByte(),
            0x61, 'a'.code.toByte(), 0x01,
            0x61, 'b'.code.toByte(), 0x02,
        )
        assertEnc(expected, m)
    }

    @Test fun nested_structures_are_deterministic() {
        // Building two maps with different insertion orders of the same content
        // must produce byte-identical encodings.
        val a = CborValue.CMap(
            linkedMapOf(
                "z" to CborValue.CArray(listOf(CborValue.CInt(1L), CborValue.CInt(2L))),
                "a" to CborValue.CString("hello"),
                "m" to CborValue.CMap(linkedMapOf("b" to CborValue.CBool(true), "a" to CborValue.CBool(false))),
            )
        )
        val b = CborValue.CMap(
            linkedMapOf(
                "m" to CborValue.CMap(linkedMapOf("a" to CborValue.CBool(false), "b" to CborValue.CBool(true))),
                "a" to CborValue.CString("hello"),
                "z" to CborValue.CArray(listOf(CborValue.CInt(1L), CborValue.CInt(2L))),
            )
        )
        assertContentEquals(CanonicalEncoder.encode(a), CanonicalEncoder.encode(b))
    }

    @Test fun booleans_and_null() {
        assertEnc(byteArrayOf(0xF4.toByte()), CborValue.CBool(false))
        assertEnc(byteArrayOf(0xF5.toByte()), CborValue.CBool(true))
        assertEnc(byteArrayOf(0xF6.toByte()), CborValue.CNull)
    }

    @Test fun round_trip_through_decoder() {
        val original = CborValue.CMap(
            linkedMapOf(
                "id" to CborValue.CInt(42L),
                "tags" to CborValue.CArray(listOf(CborValue.CString("x"), CborValue.CString("y"))),
                "payload" to CborValue.CBytes(byteArrayOf(0x0A, 0x0B)),
                "nested" to CborValue.CMap(linkedMapOf("k" to CborValue.CBool(true))),
                "optional" to CborValue.CNull,
            )
        )
        val bytes = CanonicalEncoder.encode(original)
        val decoded = CanonicalDecoder.decode(bytes)
        // Re-encoding the decoded form must match the original bytes (idempotence).
        assertContentEquals(bytes, CanonicalEncoder.encode(decoded))
    }

    @Test fun decoder_rejects_indefinite_length() {
        // 0x9F is major-4 (array) with info 31 (indefinite) — not allowed.
        assertFailsWith<IllegalStateException> {
            CanonicalDecoder.decode(byteArrayOf(0x9F.toByte(), 0xFF.toByte()))
        }
    }

    @Test fun decoder_rejects_non_string_map_keys() {
        // A single-entry map with integer key 1 → 1 : 0xA1, 0x01, 0x01.
        assertFailsWith<IllegalArgumentException> {
            CanonicalDecoder.decode(byteArrayOf(0xA1.toByte(), 0x01, 0x01))
        }
    }

    @Test fun decoder_rejects_trailing_bytes() {
        // Valid 0 followed by stray byte.
        assertFailsWith<IllegalStateException> {
            CanonicalDecoder.decode(byteArrayOf(0x00, 0x00))
        }
    }

    // ---- helpers -----------------------------------------------------------

    private fun assertEnc(expected: ByteArray, value: CborValue) {
        val actual = CanonicalEncoder.encode(value)
        assertContentEquals(
            expected, actual,
            "expected ${expected.hex()} but got ${actual.hex()}",
        )
        // Every canonical encoding must round-trip through the decoder back to
        // the identical byte sequence — the determinism guarantee in code form.
        val reEncoded = CanonicalEncoder.encode(CanonicalDecoder.decode(actual))
        assertContentEquals(actual, reEncoded)
    }

    private fun ByteArray.hex(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    @Test fun profile_round_trip() {
        val p = fyp.project.datingapp.records.UserProfile(
            did = "did:key:zABC",
            displayName = "Ada",
            bio = "hello world",
            age = 31,
            avatar = null,
            photos = null,
            signingKey = byteArrayOf(1, 2, 3),
            signalPreKeyBundle = byteArrayOf(9, 8, 7),
            interests = listOf("books", "climbing"),
            location = null,
            createdAt = "2025-01-01T00:00:00Z",
        )
        val bytes = encodeCanonical(p)
        // Two encodes of the same record must be byte-identical.
        assertContentEquals(bytes, encodeCanonical(p))
        // Round-trip via decoder recovers every scalar field.
        val back = decodeUserProfile(bytes)
        assertEquals(p.did, back.did)
        assertEquals(p.displayName, back.displayName)
        assertEquals(p.age, back.age)
        assertContentEquals(p.signingKey, back.signingKey)
        assertEquals(p.interests, back.interests)
        // And re-encoding the decoded form produces the same bytes — true determinism.
        assertContentEquals(bytes, encodeCanonical(back))
        assertTrue(bytes.isNotEmpty())
    }
}
