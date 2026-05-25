package com.aura.p2p.messaging

import com.aura.p2p.transport.PeerIdentity

/**
 * Message-layer E2EE seam (ADR-0001). A pure-KMP P-256 **ECIES** scheme: ephemeral
 * ECDH to the recipient's DID key, HKDF-SHA256, AES-256-GCM. The recipient's
 * encryption key is its `did:key` P-256 point (already published in the profile), so
 * there is no prekey bundle to publish or fetch.
 *
 * This keeps messages confidential + integrity-protected through an untrusted mailbox
 * holder (offline store-and-forward, M5) - the holder only ever sees ciphertext.
 * Trade-off: forward secrecy is limited to the sender's ephemeral key; messages are
 * sealed to the recipient's static DID key, so a compromised long-term key exposes
 * past messages (acceptable v1; rationale + the choice of ECIES over a ratchet are in
 * ARCHITECTURE.md). [MessageService] depends only on this interface, so the crypto is
 * swappable.
 */
interface MessageCrypto {
    /** Encrypt [plaintext] for the holder of [recipientDid] (P-256 ECIES). */
    suspend fun encrypt(recipientDid: String, plaintext: ByteArray): SealedMessage

    /** Decrypt [ciphertext] addressed to us; [senderDid] is bound via the AEAD AAD. */
    suspend fun decrypt(senderDid: String, ciphertext: ByteArray, messageType: Int): ByteArray
}

/** Sealed payload + a type tag (constant for ECIES; carried in the MessageEnvelope). */
class SealedMessage(val ciphertext: ByteArray, val messageType: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SealedMessage) return false
        return messageType == other.messageType && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int = 31 * ciphertext.contentHashCode() + messageType
}

/**
 * ECIES over the [KeyAgreement] seam (Android = real P-256 JCA, iOS = stub). Wire
 * ciphertext = ephemeralPublicKey(33) || nonce(12) || aeadCiphertext. AAD binds the
 * message to sender|recipient so it can't be re-targeted.
 */
class EciesMessageCrypto(
    private val selfDid: suspend () -> String?,
    private val keyAgreement: KeyAgreement,
) : MessageCrypto {

    override suspend fun encrypt(recipientDid: String, plaintext: ByteArray): SealedMessage {
        val sender = selfDid() ?: error("no local identity")
        val recipientPublic = PeerIdentity.p256FromDidKey(recipientDid)
        val agreed = keyAgreement.ephemeralAgree(recipientPublic)
        val key = Hkdf.derive(agreed.sharedSecret, salt = ByteArray(0), info = INFO, length = AES_KEY_LEN)
        val nonce = Aead.randomNonce()
        val body = Aead.seal(key, nonce, plaintext, aad(sender, recipientDid))
        return SealedMessage(agreed.ephemeralPublicKey + nonce + body, ECIES_TYPE)
    }

    override suspend fun decrypt(senderDid: String, ciphertext: ByteArray, messageType: Int): ByteArray {
        require(ciphertext.size > EPH_LEN + NONCE_LEN) { "ciphertext too short" }
        val eph = ciphertext.copyOfRange(0, EPH_LEN)
        val nonce = ciphertext.copyOfRange(EPH_LEN, EPH_LEN + NONCE_LEN)
        val body = ciphertext.copyOfRange(EPH_LEN + NONCE_LEN, ciphertext.size)
        val me = selfDid() ?: error("no local identity")
        val shared = keyAgreement.staticAgree(eph)
        val key = Hkdf.derive(shared, salt = ByteArray(0), info = INFO, length = AES_KEY_LEN)
        return Aead.open(key, nonce, body, aad(senderDid, me)) ?: error("decryption failed")
    }

    private fun aad(sender: String, recipient: String): ByteArray = "$sender|$recipient".encodeToByteArray()

    companion object {
        const val ECIES_TYPE = 1
        private const val EPH_LEN = 33    // compressed P-256 point
        private const val NONCE_LEN = 12  // AES-GCM nonce
        private const val AES_KEY_LEN = 32
        private val INFO = "com.aura.ecies/v1".encodeToByteArray()
    }
}
