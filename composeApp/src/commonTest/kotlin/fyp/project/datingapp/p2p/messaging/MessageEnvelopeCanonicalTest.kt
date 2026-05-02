package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.transport.PeerIdentity
import fyp.project.datingapp.p2p.transport.wire.MessageEnvelope
import fyp.project.datingapp.records.canonical.decodeMessageEnvelope
import fyp.project.datingapp.records.canonical.encodeCanonical
import fyp.project.datingapp.records.canonical.encodeMessageEnvelopeWire
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Wire round-trip + sender-signature gate for [MessageEnvelope]. Mirrors
 * `PresenceCanonicalTest`; uses a real P-256 did:key golden vector.
 */
class MessageEnvelopeCanonicalTest {

    private val senderDid = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"

    private fun sample(signature: ByteArray = byteArrayOf(1, 2, 3, 4)) = MessageEnvelope(
        senderDid = senderDid,
        recipientDid = "did:key:zDnaerecipient000000000000000000000000000000000",
        msgId = "01J0ABCDEF",
        ciphertext = byteArrayOf(33, 7, 99, 12, 0, 5),
        messageType = EciesMessageCrypto.ECIES_TYPE,
        sentAt = "2026-05-23T12:00:00Z",
        signature = signature,
    )

    @Test fun wire_roundTrip_recoversAllFields() {
        val e = sample()
        val back = decodeMessageEnvelope(encodeMessageEnvelopeWire(e))
        assertEquals(e.type, back.type)
        assertEquals(e.senderDid, back.senderDid)
        assertEquals(e.recipientDid, back.recipientDid)
        assertEquals(e.msgId, back.msgId)
        assertContentEquals(e.ciphertext, back.ciphertext)
        assertEquals(e.messageType, back.messageType)
        assertEquals(e.sentAt, back.sentAt)
        assertContentEquals(e.signature, back.signature)
    }

    @Test fun encoding_isDeterministic() {
        val e = sample()
        assertContentEquals(encodeMessageEnvelopeWire(e), encodeMessageEnvelopeWire(e))
        assertContentEquals(encodeCanonical(e), encodeCanonical(e))
    }

    @Test fun signableBytes_omitSignature() {
        val a = sample(signature = byteArrayOf(1, 2, 3))
        val b = sample(signature = byteArrayOf(9, 9, 9, 9))
        assertContentEquals(encodeCanonical(a), encodeCanonical(b))
        assertTrue(!encodeMessageEnvelopeWire(a).contentEquals(encodeMessageEnvelopeWire(b)))
    }

    @Test fun verify_passesAndUsesSenderKeyAndSignableBytes() {
        val e = sample()
        val fake = CapturingVerifier(result = true)
        assertTrue(MessageEnvelopeVerifier.verify(e, fake))
        assertContentEquals(PeerIdentity.p256FromDidKey(senderDid), fake.lastPublicKey)
        assertContentEquals(encodeCanonical(e), fake.lastData)
    }

    @Test fun verify_falseOnEmptySignature() {
        val fake = CapturingVerifier(result = true)
        assertFalse(MessageEnvelopeVerifier.verify(sample(signature = ByteArray(0)), fake))
        assertEquals(0, fake.calls)
    }

    @Test fun verify_falseOnMalformedSenderDid() {
        val bad = sample().copy(senderDid = "did:key:notvalid")
        assertFalse(MessageEnvelopeVerifier.verify(bad, CapturingVerifier(result = true)))
    }

    @Test fun verify_falseWhenVerifierRejects() {
        assertFalse(MessageEnvelopeVerifier.verify(sample(), CapturingVerifier(result = false)))
    }

    private class CapturingVerifier(private val result: Boolean) : SignatureVerifier {
        var lastPublicKey: ByteArray? = null
        var lastData: ByteArray? = null
        var calls = 0
        override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String): Boolean {
            calls++
            lastPublicKey = publicKey
            lastData = data
            return result
        }
    }
}
