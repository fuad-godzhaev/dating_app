package fyp.project.datingapp.p2p.messaging

/**
 * AES-256-GCM with a caller-supplied key (unlike `CacheEncryption`, which is bound to
 * a non-exportable Keystore key). Used for ECIES message bodies (Part 5), keyed by an
 * HKDF-derived ECDH secret. Android = JCA `SecretKeySpec` (works on plain JVM too, so
 * the crypto round-trip is unit-testable); iOS = stub.
 */
expect object Aead {
    /** Fresh 12-byte GCM nonce. */
    fun randomNonce(): ByteArray

    /** Encrypt: AES-256-GCM([key]=32 bytes, [nonce]=12 bytes, [plaintext], [aad]). */
    fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray

    /** Decrypt; null on tag/AAD mismatch (treated as a failure by the caller). */
    fun open(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray?
}
