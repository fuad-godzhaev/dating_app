package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.p2p.relay.EpochClock
import fyp.project.datingapp.p2p.transport.wire.MessageEnvelope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxHolderTest {

    private class MutableClock(var t: Long = 0L) : EpochClock {
        override fun nowMs(): Long = t
    }

    private fun env(msgId: String, recipient: String = "did:r", sender: String = "did:s") =
        MessageEnvelope(
            senderDid = sender,
            recipientDid = recipient,
            msgId = msgId,
            ciphertext = byteArrayOf(1, 2, 3),
            messageType = EciesMessageCrypto.ECIES_TYPE,
            sentAt = "2026-05-23T12:00:00Z",
            signature = byteArrayOf(9),
        )

    @Test fun deposit_then_pull_returnsIt() = runTest {
        val holder = MailboxHolder()
        assertTrue(holder.deposit(env("m1")))
        val pulled = holder.pull("did:r")
        assertEquals(1, pulled.size)
        assertEquals("m1", pulled[0].msgId)
    }

    @Test fun deposit_dedupsByMsgId() = runTest {
        val holder = MailboxHolder()
        assertTrue(holder.deposit(env("m1")))
        assertTrue(holder.deposit(env("m1"))) // dup -> accepted, not stored twice
        assertEquals(1, holder.count("did:r"))
    }

    @Test fun deposit_enforcesPerSenderCap() = runTest {
        val holder = MailboxHolder(maxPerSenderPerRecipient = 2)
        assertTrue(holder.deposit(env("m1")))
        assertTrue(holder.deposit(env("m2")))
        assertFalse(holder.deposit(env("m3")), "third from same sender exceeds the cap")
        assertEquals(2, holder.count("did:r"))
    }

    @Test fun deposit_enforcesPerRecipientCap() = runTest {
        val holder = MailboxHolder(maxEnvelopesPerRecipient = 2, maxPerSenderPerRecipient = 100)
        assertTrue(holder.deposit(env("m1", sender = "did:s1")))
        assertTrue(holder.deposit(env("m2", sender = "did:s2")))
        assertFalse(holder.deposit(env("m3", sender = "did:s3")))
        assertEquals(2, holder.count("did:r"))
    }

    @Test fun pull_dropsExpiredEnvelopes() = runTest {
        val clock = MutableClock(0)
        val holder = MailboxHolder(clock = clock, ttlMs = 1_000)
        holder.deposit(env("m1"))
        clock.t = 1_001
        assertTrue(holder.pull("did:r").isEmpty(), "expired mail is swept on pull")
        assertEquals(0, holder.count("did:r"))
    }

    @Test fun pull_isolatesRecipients() = runTest {
        val holder = MailboxHolder()
        holder.deposit(env("m1", recipient = "did:a"))
        holder.deposit(env("m2", recipient = "did:b"))
        assertEquals(listOf("m1"), holder.pull("did:a").map { it.msgId })
        assertEquals(listOf("m2"), holder.pull("did:b").map { it.msgId })
    }
}
