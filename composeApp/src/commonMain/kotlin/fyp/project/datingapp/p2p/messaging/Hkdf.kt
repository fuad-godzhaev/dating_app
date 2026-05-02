package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.database.sha256Digest

/**
 * HKDF-SHA256 (RFC 5869), pure Kotlin over the project's [sha256Digest]. Derives an
 * AES key from an ECDH shared secret for ECIES message encryption (Part 5).
 */
object Hkdf {
    private const val BLOCK_SIZE = 64 // SHA-256 block size
    private const val HASH_LEN = 32

    /** Derive [length] bytes from [ikm] with [salt] and [info]. */
    fun derive(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val prk = hmac(if (salt.isEmpty()) ByteArray(HASH_LEN) else salt, ikm) // extract
        return expand(prk, info, length)
    }

    private fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length <= 255 * HASH_LEN) { "HKDF length too large" }
        val out = ByteArray(length)
        var t = ByteArray(0)
        var pos = 0
        var counter = 1
        while (pos < length) {
            t = hmac(prk, t + info + byteArrayOf(counter.toByte()))
            val n = minOf(t.size, length - pos)
            t.copyInto(out, pos, 0, n)
            pos += n
            counter++
        }
        return out
    }

    private fun hmac(key: ByteArray, message: ByteArray): ByteArray {
        val k = if (key.size > BLOCK_SIZE) sha256Digest(key) else key
        val keyBlock = k.copyOf(BLOCK_SIZE)
        val ipad = ByteArray(BLOCK_SIZE) { (keyBlock[it].toInt() xor 0x36).toByte() }
        val opad = ByteArray(BLOCK_SIZE) { (keyBlock[it].toInt() xor 0x5c).toByte() }
        return sha256Digest(opad + sha256Digest(ipad + message))
    }
}
