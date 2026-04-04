package fyp.project.datingapp.p2p.relay

/**
 * Test double for [CacheEncryption] that avoids the commonTest / platform
 * actual split. Behaviour matches the real contract where it matters:
 *  - `seal` returns a fresh nonce each call (monotonic counter).
 *  - `open` with a mismatched AAD returns null.
 *
 * Ciphertext is the plaintext XOR'd with a nonce-derived byte so a ciphertext
 * swap across rows is detectable (decrypt-then-AAD-check path in tests).
 */
class FakeCacheEncryption : CacheCipher {
    private var counter: Long = 0

    override fun seal(plaintext: ByteArray, aad: ByteArray): Sealed {
        counter += 1
        val nonce = longToBytes(counter)
        val aadHash = aad.fold(0.toByte()) { acc, b -> (acc.toInt() xor b.toInt()).toByte() }
        val ct = ByteArray(plaintext.size) { i -> (plaintext[i].toInt() xor aadHash.toInt()).toByte() }
        // Prepend AAD tag so open can detect mismatches.
        val tagged = byteArrayOf(aadHash) + ct
        return Sealed(ciphertext = tagged, nonce = nonce)
    }

    override fun open(ciphertext: ByteArray, nonce: ByteArray, aad: ByteArray): ByteArray? {
        if (ciphertext.isEmpty()) return null
        val expected = aad.fold(0.toByte()) { acc, b -> (acc.toInt() xor b.toInt()).toByte() }
        if (ciphertext[0] != expected) return null
        val body = ciphertext.copyOfRange(1, ciphertext.size)
        return ByteArray(body.size) { i -> (body[i].toInt() xor expected.toInt()).toByte() }
    }

    private fun longToBytes(v: Long): ByteArray =
        ByteArray(12) { i -> ((v ushr (8 * i)) and 0xFF).toByte() }
}
