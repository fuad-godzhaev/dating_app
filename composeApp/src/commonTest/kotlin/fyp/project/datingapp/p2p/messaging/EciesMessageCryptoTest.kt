package fyp.project.datingapp.p2p.messaging

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Round-trip for the ECIES message crypto. The real HKDF + AES-GCM ([Aead]) run on
 * the JVM (Android actual = JCA), so this exercises the actual KDF + AEAD; only the
 * P-256 ECDH is faked (a symmetric shared secret) - that part is JCA on-device.
 */
class EciesMessageCryptoTest {

    // A valid P-256 did:key (so PeerIdentity.p256FromDidKey succeeds in encrypt()).
    private val did = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"

    /** Carries the shared secret inside the 33-byte "ephemeral public key" so the
     *  recipient recovers the identical secret - a stand-in for real ECDH. */
    private class FakeKeyAgreement : KeyAgreement {
        private var counter = 0
        override fun ephemeralAgree(recipientPublicKey: ByteArray): EcdhResult {
            val s = ByteArray(32) { (counter + it + 1).toByte() }
            counter++
            return EcdhResult(byteArrayOf(0x02) + s, s)
        }
        override suspend fun staticAgree(ephemeralPublicKey: ByteArray): ByteArray =
            ephemeralPublicKey.copyOfRange(1, 33)
    }

    @Test fun roundTrip_recoversPlaintext() = runTest {
        val crypto = EciesMessageCrypto(selfDid = { did }, keyAgreement = FakeKeyAgreement())
        val sealed = crypto.encrypt(did, "hello world".encodeToByteArray())
        assertEquals(EciesMessageCrypto.ECIES_TYPE, sealed.messageType)
        // ciphertext = eph(33) + nonce(12) + body; must be longer than the headers.
        assertTrue(sealed.ciphertext.size > 45)
        val plain = crypto.decrypt(did, sealed.ciphertext, sealed.messageType)
        assertEquals("hello world", plain.decodeToString())
    }

    @Test fun tamperedCiphertext_fails() = runTest {
        val crypto = EciesMessageCrypto({ did }, FakeKeyAgreement())
        val sealed = crypto.encrypt(did, "secret".encodeToByteArray())
        val tampered = sealed.ciphertext.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertFailsWith<IllegalStateException> { crypto.decrypt(did, tampered, sealed.messageType) }
    }

    @Test fun wrongSenderInAad_fails() = runTest {
        // AAD binds sender|recipient. Decrypting as if from a different sender breaks the tag.
        val crypto = EciesMessageCrypto({ did }, FakeKeyAgreement())
        val sealed = crypto.encrypt(did, "hi".encodeToByteArray())
        assertFailsWith<IllegalStateException> {
            crypto.decrypt("did:key:zDnaeOtherSender0000000000000000000000000000000", sealed.ciphertext, sealed.messageType)
        }
    }
}
