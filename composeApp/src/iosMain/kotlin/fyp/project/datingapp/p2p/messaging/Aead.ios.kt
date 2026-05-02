package fyp.project.datingapp.p2p.messaging

// iOS stub - AES-GCM via CryptoKit/Security lands with the iOS crypto bring-up.
actual object Aead {
    actual fun randomNonce(): ByteArray = ByteArray(12)
    actual fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray =
        throw UnsupportedOperationException("AEAD is not implemented on iOS yet")
    actual fun open(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray? = null
}
