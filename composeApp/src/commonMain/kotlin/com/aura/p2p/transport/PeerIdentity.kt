package com.aura.p2p.transport

/**
 * did:key codec for the P-256 signing key (ADR-0003): multicodec p256-pub
 * (0x1200, unsigned varint 0x80 0x24) + the 33-byte compressed point, base58btc,
 * multibase 'z' -> "did:key:zDn...". The libp2p transport PeerId is derived
 * separately, by the go-libp2p host from the Ed25519 transport seed (ADR-0004).
 */
object PeerIdentity {
    private const val DID_KEY_PREFIX = "did:key:"
    private const val MULTIBASE_BASE58BTC = "z"
    private val P256_MULTICODEC = byteArrayOf(0x80.toByte(), 0x24) // unsigned varint of 0x1200 (p256-pub)
    private const val P256_COMPRESSED_LEN = 33

    fun didKeyFromP256(compressedPoint: ByteArray): String {
        require(compressedPoint.size == P256_COMPRESSED_LEN) {
            "P-256 compressed point must be 33 bytes, got ${compressedPoint.size}"
        }
        require(compressedPoint[0] == 0x02.toByte() || compressedPoint[0] == 0x03.toByte()) {
            "P-256 point must be compressed (0x02/0x03 prefix), got 0x${compressedPoint[0].toUByte().toString(16)}"
        }
        return DID_KEY_PREFIX + MULTIBASE_BASE58BTC + base58Encode(P256_MULTICODEC + compressedPoint)
    }

    fun p256FromDidKey(did: String): ByteArray {
        require(did.startsWith(DID_KEY_PREFIX + MULTIBASE_BASE58BTC)) {
            "not a base58btc did:key string"
        }
        val decoded = base58Decode(did.substring(DID_KEY_PREFIX.length + MULTIBASE_BASE58BTC.length))
        require(decoded.size == P256_MULTICODEC.size + P256_COMPRESSED_LEN) {
            "unexpected did:key payload length ${decoded.size}"
        }
        require(decoded[0] == P256_MULTICODEC[0] && decoded[1] == P256_MULTICODEC[1]) {
            "did:key multicodec is not p256-pub"
        }
        return decoded.copyOfRange(P256_MULTICODEC.size, decoded.size)
    }

    // ---- base58btc (Bitcoin alphabet), dependency-free port of the bitcoinj algorithm ----

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val ENCODED_ZERO = ALPHABET[0]
    private val INDEXES = IntArray(128) { -1 }.also { for (i in ALPHABET.indices) it[ALPHABET[i].code] = i }

    private fun base58Encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) zeros++
        val buf = input.copyOf()
        val encoded = CharArray(buf.size * 2)
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < buf.size) {
            encoded[--outputStart] = ALPHABET[divmod(buf, inputStart, 256, 58).toInt() and 0xFF]
            if (buf[inputStart].toInt() == 0) inputStart++
        }
        while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) outputStart++
        var z = zeros
        while (z-- > 0) encoded[--outputStart] = ENCODED_ZERO
        return encoded.concatToString(outputStart, encoded.size)
    }

    private fun base58Decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            require(digit >= 0) { "invalid base58 character '$c'" }
            input58[i] = digit.toByte()
        }
        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) zeros++
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256)
            if (input58[inputStart].toInt() == 0) inputStart++
        }
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) outputStart++
        return decoded.copyOfRange(outputStart - zeros, decoded.size)
    }

    /** Divide `number` (base `base`, big-endian) in place by `divisor`; return remainder. */
    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Byte {
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder.toByte()
    }
}
