package com.aura.p2p.messaging

import com.aura.p2p.relay.EpochClock
import com.aura.p2p.relay.FakeCacheEncryption
import com.aura.p2p.transport.wire.MessageEnvelope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxHolderTest {

    private class MutableClock(var t: Long = 0L) : EpochClock {
        override fun nowMs(): Long = t
    }

    private fun holder(
        dao: FakeMailboxDao = FakeMailboxDao(),
        clock: EpochClock = MutableClock(0),
        ttlMs: Long = MailboxHolder.DEFAULT_TTL_MS,
        maxPerSender: Int = MailboxHolder.DEFAULT_MAX_PER_SENDER,
        maxPerRecipient: Int = MailboxHolder.DEFAULT_MAX_PER_RECIPIENT,
    ) = MailboxHolder(
        dao = dao,
        cache = FakeCacheEncryption(),
        clock = clock,
        ttlMs = ttlMs,
        maxPerSenderPerRecipient = maxPerSender,
        maxEnvelopesPerRecipient = maxPerRecipient,
    )

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
        val h = holder()
        assertTrue(h.deposit(env("m1")))
        val pulled = h.pull("did:r")
        assertEquals(1, pulled.size)
        assertEquals("m1", pulled[0].msgId)
    }

    @Test fun deposit_dedupsByMsgId() = runTest {
        val h = holder()
        assertTrue(h.deposit(env("m1")))
        assertTrue(h.deposit(env("m1"))) // dup -> accepted, not stored twice
        assertEquals(1, h.count("did:r"))
    }

    @Test fun deposit_enforcesPerSenderCap() = runTest {
        val h = holder(maxPerSender = 2)
        assertTrue(h.deposit(env("m1")))
        assertTrue(h.deposit(env("m2")))
        assertFalse(h.deposit(env("m3")), "third from same sender exceeds the cap")
        assertEquals(2, h.count("did:r"))
    }

    @Test fun deposit_enforcesPerRecipientCap() = runTest {
        val h = holder(maxPerRecipient = 2, maxPerSender = 100)
        assertTrue(h.deposit(env("m1", sender = "did:s1")))
        assertTrue(h.deposit(env("m2", sender = "did:s2")))
        assertFalse(h.deposit(env("m3", sender = "did:s3")))
        assertEquals(2, h.count("did:r"))
    }

    @Test fun pull_dropsExpiredEnvelopes() = runTest {
        val clock = MutableClock(0)
        val h = holder(clock = clock, ttlMs = 1_000)
        h.deposit(env("m1"))
        clock.t = 1_001
        assertTrue(h.pull("did:r").isEmpty(), "expired mail is filtered on pull")
        assertEquals(0, h.count("did:r"))
    }

    @Test fun pull_isolatesRecipients() = runTest {
        val h = holder()
        h.deposit(env("m1", recipient = "did:a"))
        h.deposit(env("m2", recipient = "did:b"))
        assertEquals(listOf("m1"), h.pull("did:a").map { it.msgId })
        assertEquals(listOf("m2"), h.pull("did:b").map { it.msgId })
    }

    @Test fun storedRowsAreSealed_andTamperIsSkippedOnPull() = runTest {
        val dao = FakeMailboxDao()
        val h = holder(dao = dao)
        h.deposit(env("m1"))

        // At rest the row holds ciphertext, never the plaintext envelope bytes.
        val row = dao.rows.single()
        assertFalse(row.ciphertext.isEmpty())

        // Corrupt the sealed blob: open() returns null -> the row is silently skipped.
        dao.rows[0] = row.copy(ciphertext = byteArrayOf(0, 0, 0, 0))
        assertTrue(h.pull("did:r").isEmpty(), "a tampered sealed row is not served")
    }
}
