package com.aura.records.canonical

import com.aura.database.sha256Digest

/**
 * CIDv1 with dag-cbor codec and sha2-256 multihash, encoded as multibase
 * base32-lower (no padding, leading 'b').
 *
 * Layout (binary, pre-multibase):
 *   0x01          — CID version 1
 *   0x71          — multicodec: dag-cbor
 *   0x12 0x20     — multihash: sha2-256, 32 bytes
 *   [32 bytes]    — sha256 digest of the canonical DAG-CBOR bytes
 *
 * The 36-byte binary form is base32-encoded with the RFC 4648 lower-case
 * alphabet without padding; the resulting string is prefixed with 'b'
 * (multibase tag for base32-lower). Every CID produced here starts with
 * "bafyrei" because the first three bytes 0x01 0x71 0x12 encode identically
 * across all inputs.
 *
 * Reference: https://github.com/multiformats/cid
 */
object Cid {
    private const val CID_VERSION_1: Byte = 0x01
    private const val MULTICODEC_DAG_CBOR: Byte = 0x71
    private const val MULTICODEC_RAW: Byte = 0x55
    private const val MULTIHASH_SHA2_256: Byte = 0x12
    private const val SHA256_LEN: Byte = 0x20

    /** Compute the CIDv1 string for already-canonical DAG-CBOR bytes. */
    fun cidV1DagCbor(canonicalCborBytes: ByteArray): String =
        cidV1(MULTICODEC_DAG_CBOR, canonicalCborBytes)

    /**
     * CIDv1 with the **raw** multicodec (0x55) for opaque binary (blobs / photos),
     * sha2-256 multihash, base32-lower. A fetched blob is verified by recomputing
     * this over its bytes and comparing to the [BlobRef.ref]. Raw CIDs render with
     * the "bafkrei" prefix (0x01 0x55 0x12), distinct from dag-cbor's "bafyrei".
     */
    fun cidV1Raw(bytes: ByteArray): String =
        cidV1(MULTICODEC_RAW, bytes)

    private fun cidV1(multicodec: Byte, bytes: ByteArray): String {
        val digest = sha256Digest(bytes)
        require(digest.size == 32) { "sha256 digest must be 32 bytes, got ${digest.size}" }
        val binary = ByteArray(4 + digest.size)
        binary[0] = CID_VERSION_1
        binary[1] = multicodec
        binary[2] = MULTIHASH_SHA2_256
        binary[3] = SHA256_LEN
        digest.copyInto(binary, destinationOffset = 4)
        return "b" + base32LowerNoPadding(binary)
    }

    // ---- base32 lower, RFC 4648, no padding --------------------------------

    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz234567"

    private fun base32LowerNoPadding(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bits = 0
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                val idx = (buffer ushr bits) and 0x1F
                sb.append(ALPHABET[idx])
            }
        }
        if (bits > 0) {
            val idx = (buffer shl (5 - bits)) and 0x1F
            sb.append(ALPHABET[idx])
        }
        return sb.toString()
    }
}
