package com.aura.p2p.messaging

import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.discovery.PeerContact
import com.aura.p2p.discovery.PeerDirectory
import com.aura.p2p.fetch.ProfileFetcher
import com.aura.p2p.relay.EpochClock
import com.aura.p2p.transport.wire.MessageEnvelope
import com.aura.p2p.transport.wire.SignedEnvelope
import com.aura.records.UserProfile
import com.aura.database.appView.entities.MessageEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises the M4 send/receive flow against fakes. The fake [MessageCrypto] is a
 * symmetric passthrough so a round-trip preserves the plaintext (the real ECIES
 * crypto is covered by EciesMessageCryptoTest).
 */
class MessageServiceTest {

    private val senderDid = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"
    private val recipientDid = "did:key:zDnaeRecipientPlaceholderForUnitTests00000000000"

    private val clock = EpochClock { 1_000L }

    private class FakeMessageCrypto : MessageCrypto {
        override suspend fun encrypt(recipientDid: String, plaintext: ByteArray) =
            SealedMessage(plaintext, EciesMessageCrypto.ECIES_TYPE) // passthrough
        override suspend fun decrypt(senderDid: String, ciphertext: ByteArray, messageType: Int) = ciphertext
    }

    private class AcceptingVerifier(private val result: Boolean = true) : SignatureVerifier {
        override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String) = result
    }

    private class FakeStreamClient(var ack: Boolean = true) : MessageStreamClient {
        var lastSent: ByteArray? = null
        var sendCount = 0
        override suspend fun send(contact: PeerContact, envelopeWireBytes: ByteArray): Boolean {
            sendCount++
            lastSent = envelopeWireBytes
            return ack
        }
    }

    private class FakeProfileFetcher(private val profile: UserProfile?) : ProfileFetcher {
        override suspend fun fetch(did: String, expectedCid: String?): UserProfile? = profile
        override suspend fun fetchSigned(did: String, expectedCid: String?): SignedEnvelope? = null
    }

    private fun profile(did: String) = UserProfile(
        did = did,
        displayName = "Peer",
        bio = "",
        age = 30,
        signingKey = byteArrayOf(1),
        interests = emptyList(),
        createdAt = "2026-05-23T00:00:00Z",
    )

    private fun service(
        selfDid: String,
        dao: FakeMessageDao,
        client: FakeStreamClient,
        directory: PeerDirectory,
        fetcherProfile: UserProfile? = profile(recipientDid),
        verifierResult: Boolean = true,
        offlineDeposit: (suspend (MessageEnvelope) -> Boolean)? = null,
    ) = MessageService(
        selfDid = { selfDid },
        crypto = FakeMessageCrypto(),
        sign = { byteArrayOf(7, 7, 7) },
        verifier = AcceptingVerifier(verifierResult),
        fetcher = FakeProfileFetcher(fetcherProfile),
        peerDirectory = directory,
        streamClient = client,
        messageDao = dao,
        clock = clock,
        newMsgId = { "msg-1" },
        nowIso = { "2026-05-23T12:00:00Z" },
        offlineDeposit = offlineDeposit,
    )

    @Test fun sendMessage_encryptsStoresAndDelivers() = runTest {
        val dao = FakeMessageDao()
        val client = FakeStreamClient(ack = true)
        val directory = PeerDirectory().apply { record(recipientDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001")) }
        val svc = service(senderDid, dao, client, directory)

        assertTrue(svc.sendMessage(recipientDid, "hi there"))
        assertEquals(1, client.sendCount)
        val stored = dao.messages["msg-1"]
        assertNotNull(stored)
        assertEquals(MessageEntity.DIRECTION_OUT, stored.direction)
        // Direct online ack => DELIVERED (recipient's handler stored it).
        assertEquals(MessageEntity.STATE_DELIVERED, stored.deliveryState)
        assertEquals("hi there", stored.plaintext)
        assertTrue(dao.hasConversation(recipientDid))
    }

    @Test fun sendMessage_parkedInMailbox_marksSent() = runTest {
        val dao = FakeMessageDao()
        val client = FakeStreamClient(ack = true)
        // No peer contact -> online delivery fails; offlineDeposit parks it at a holder.
        val svc = service(senderDid, dao, client, PeerDirectory(), offlineDeposit = { true })

        assertFalse(svc.sendMessage(recipientDid, "later"))
        assertEquals(0, client.sendCount)
        assertEquals(MessageEntity.STATE_SENT, dao.messages["msg-1"]?.deliveryState)
    }

    @Test fun handleReceipt_marksOutgoingDelivered() = runTest {
        val dao = FakeMessageDao()
        // Park an outgoing message (SENT) first.
        val svc = service(senderDid, dao, FakeStreamClient(ack = true), PeerDirectory(), offlineDeposit = { true })
        svc.sendMessage(recipientDid, "later")
        assertEquals(MessageEntity.STATE_SENT, dao.messages["msg-1"]?.deliveryState)

        // A reverse receipt for that msgId flips it to DELIVERED.
        assertTrue(svc.handleReceipt("msg-1".encodeToByteArray()))
        assertEquals(MessageEntity.STATE_DELIVERED, dao.messages["msg-1"]?.deliveryState)
    }

    @Test fun sendMessage_noPeer_staysQueuedAndReturnsFalse() = runTest {
        val dao = FakeMessageDao()
        val client = FakeStreamClient(ack = true)
        val svc = service(senderDid, dao, client, PeerDirectory()) // no contact

        assertFalse(svc.sendMessage(recipientDid, "hi"))
        assertEquals(0, client.sendCount)
        assertEquals(MessageEntity.STATE_QUEUED, dao.messages["msg-1"]?.deliveryState)
    }

    @Test fun roundTrip_recipientDecryptsAndStores() = runTest {
        // Sender builds + "sends" the wire envelope.
        val senderDao = FakeMessageDao()
        val client = FakeStreamClient(ack = true)
        val directory = PeerDirectory().apply { record(recipientDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001")) }
        service(senderDid, senderDao, client, directory).sendMessage(recipientDid, "hello")
        val wire = client.lastSent!!

        // Recipient processes it.
        val recipientDao = FakeMessageDao()
        val recipientSvc = service(recipientDid, recipientDao, FakeStreamClient(), PeerDirectory())
        assertTrue(recipientSvc.handleIncoming(wire))

        val incoming = recipientDao.messages["msg-1"]
        assertNotNull(incoming)
        assertEquals(MessageEntity.DIRECTION_IN, incoming.direction)
        assertEquals("hello", incoming.plaintext)
        assertEquals(1, recipientDao.conversations[senderDid]?.unreadCount)
    }

    @Test fun handleIncoming_dedupsByMsgId() = runTest {
        val senderDao = FakeMessageDao()
        val client = FakeStreamClient(ack = true)
        val directory = PeerDirectory().apply { record(recipientDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001")) }
        service(senderDid, senderDao, client, directory).sendMessage(recipientDid, "dup")
        val wire = client.lastSent!!

        val dao = FakeMessageDao()
        val svc = service(recipientDid, dao, FakeStreamClient(), PeerDirectory())
        assertTrue(svc.handleIncoming(wire))
        assertTrue(svc.handleIncoming(wire)) // duplicate accepted, not re-stored
        assertEquals(1, dao.messages.size)
        assertEquals(1, dao.conversations[senderDid]?.unreadCount)
    }

    @Test fun handleIncoming_wrongRecipient_rejected() = runTest {
        val senderDao = FakeMessageDao()
        val client = FakeStreamClient(ack = true)
        val directory = PeerDirectory().apply { record(recipientDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001")) }
        service(senderDid, senderDao, client, directory).sendMessage(recipientDid, "hi")
        val wire = client.lastSent!!

        val dao = FakeMessageDao()
        // selfDid is someone else -> not addressed to us.
        val svc = service("did:key:zDnaeSomeoneElse00000000000000000000000000000000", dao, FakeStreamClient(), PeerDirectory())
        assertFalse(svc.handleIncoming(wire))
        assertEquals(0, dao.messages.size)
    }

    @Test fun handleIncoming_unverifiedSignature_rejected() = runTest {
        val senderDao = FakeMessageDao()
        val client = FakeStreamClient(ack = true)
        val directory = PeerDirectory().apply { record(recipientDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001")) }
        service(senderDid, senderDao, client, directory).sendMessage(recipientDid, "hi")
        val wire = client.lastSent!!

        val dao = FakeMessageDao()
        val svc = service(recipientDid, dao, FakeStreamClient(), PeerDirectory(), verifierResult = false)
        assertFalse(svc.handleIncoming(wire))
        assertEquals(0, dao.messages.size)
    }
}
