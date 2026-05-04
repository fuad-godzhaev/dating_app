package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.database.appView.dao.MailboxDao
import fyp.project.datingapp.database.appView.entities.MailboxEntity

/**
 * In-memory [MailboxDao] for tests. `rows` is public so a test can tamper with a
 * stored ciphertext to exercise the seal-at-rest integrity path.
 */
class FakeMailboxDao : MailboxDao {
    val rows = mutableListOf<MailboxEntity>()

    override suspend fun insert(row: MailboxEntity): Long {
        if (rows.any { it.msgId == row.msgId }) return -1L
        rows.add(row)
        return rows.size.toLong()
    }

    override suspend fun has(msgId: String): Boolean = rows.any { it.msgId == msgId }

    override suspend fun forRecipient(recipientDid: String, cutoff: Long): List<MailboxEntity> =
        rows.filter { it.recipientDid == recipientDid && it.depositedAt > cutoff }.sortedBy { it.depositedAt }

    override suspend fun countForRecipient(recipientDid: String, cutoff: Long): Int =
        rows.count { it.recipientDid == recipientDid && it.depositedAt > cutoff }

    override suspend fun countForSender(recipientDid: String, senderDid: String, cutoff: Long): Int =
        rows.count { it.recipientDid == recipientDid && it.senderDid == senderDid && it.depositedAt > cutoff }

    override suspend fun deleteExpired(cutoff: Long) {
        rows.removeAll { it.depositedAt <= cutoff }
    }
}
