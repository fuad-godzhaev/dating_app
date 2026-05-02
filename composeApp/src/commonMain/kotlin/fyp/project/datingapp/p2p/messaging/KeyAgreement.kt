package fyp.project.datingapp.p2p.messaging

/**
 * P-256 ECDH key agreement for ECIES message encryption (Part 5). Fakeable so
 * [EciesMessageCrypto] is unit-testable; the real impl is JCA on Android (iOS stub).
 */
interface KeyAgreement {
    /**
     * Sender side: generate an ephemeral keypair and ECDH it with [recipientPublicKey]
     * (a 33-byte compressed P-256 point). Returns the ephemeral public key (to ship in
     * the ciphertext) and the 32-byte shared secret.
     */
    fun ephemeralAgree(recipientPublicKey: ByteArray): EcdhResult

    /**
     * Recipient side: ECDH our stored static key with the sender's [ephemeralPublicKey].
     * Returns the same 32-byte shared secret.
     */
    suspend fun staticAgree(ephemeralPublicKey: ByteArray): ByteArray
}

class EcdhResult(val ephemeralPublicKey: ByteArray, val sharedSecret: ByteArray)
