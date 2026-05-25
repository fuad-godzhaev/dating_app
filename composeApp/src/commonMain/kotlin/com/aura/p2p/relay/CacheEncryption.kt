package com.aura.p2p.relay

/**
 * Authenticated encryption wrapper for cached record bodies at rest.
 *
 * Actuals:
 *   - Android: AES-256-GCM via Android Keystore with a non-exportable key
 *     (alias `com.aura.cacheWrap`), 12-byte random nonce,
 *     AAD = `"${ownerDid}|${cid}"`. The key cannot be moved off-device; a
 *     filesystem dump attacker has ciphertext only.
 *   - JVM: per-process in-memory key for desktop preview / `jvmTest`.
 *   - iOS: Phase A stub that throws [UnsupportedOperationException].
 *     Replaced by a Secure-Enclave-backed implementation when iOS libp2p
 *     lands (see `p2p-subsystem-design.md` §13.2).
 *
 * The AAD ties each ciphertext to its owner DID and CID: if a mover tries
 * to swap the ciphertext under a different row, GCM tag verification fails
 * and the row is treated as a cache miss. See [RelayPolicy.get].
 *
 * `seal` fills both [Sealed.ciphertext] and [Sealed.nonce]; callers store
 * them verbatim. `open` requires the same nonce + AAD that `seal`
 * produced.
 */

/**
 * Cipher boundary that [RelayPolicy] depends on. The expect class
 * [CacheEncryption] implements this directly so production wiring can
 * pass a [CacheEncryption] instance without an adapter; commonTest doubles
 * implement [CacheCipher] to avoid pulling each platform's crypto stack
 * into the test classpath.
 */
interface CacheCipher {
    fun seal(plaintext: ByteArray, aad: ByteArray): Sealed
    fun open(ciphertext: ByteArray, nonce: ByteArray, aad: ByteArray): ByteArray?
}

expect class CacheEncryption() : CacheCipher {
    override fun seal(plaintext: ByteArray, aad: ByteArray): Sealed
    override fun open(ciphertext: ByteArray, nonce: ByteArray, aad: ByteArray): ByteArray?
}

data class Sealed(val ciphertext: ByteArray, val nonce: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sealed) return false
        return ciphertext.contentEquals(other.ciphertext) && nonce.contentEquals(other.nonce)
    }
    override fun hashCode(): Int = 31 * ciphertext.contentHashCode() + nonce.contentHashCode()
}

/** Compose the AAD used by the relay cache from an envelope's identifiers. */
fun relayAad(ownerDid: String, cid: String): ByteArray = "$ownerDid|$cid".encodeToByteArray()
