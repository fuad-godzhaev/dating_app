package com.aura.database.appView.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.database.appView.entities.MailboxEntity

/**
 * Persistence for the cacheHolder mailbox (ADR-0001 / M5). All reads take a
 * `cutoff` (= now - TTL) so expiry is enforced in the query and a separate sweep
 * is only an optimisation, not a correctness requirement.
 */
@Dao
interface MailboxDao {

    /** Insert a sealed envelope, ignoring duplicates by [MailboxEntity.msgId]. Returns -1 if a dup. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: MailboxEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM mailbox WHERE msgId = :msgId)")
    suspend fun has(msgId: String): Boolean

    /** Non-expired rows for a recipient, oldest first. */
    @Query("SELECT * FROM mailbox WHERE recipientDid = :recipientDid AND depositedAt > :cutoff ORDER BY depositedAt ASC")
    suspend fun forRecipient(recipientDid: String, cutoff: Long): List<MailboxEntity>

    @Query("SELECT COUNT(*) FROM mailbox WHERE recipientDid = :recipientDid AND depositedAt > :cutoff")
    suspend fun countForRecipient(recipientDid: String, cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM mailbox WHERE recipientDid = :recipientDid AND senderDid = :senderDid AND depositedAt > :cutoff")
    suspend fun countForSender(recipientDid: String, senderDid: String, cutoff: Long): Int

    /** Drop rows past their TTL. Called opportunistically on deposit/pull. */
    @Query("DELETE FROM mailbox WHERE depositedAt <= :cutoff")
    suspend fun deleteExpired(cutoff: Long)
}
