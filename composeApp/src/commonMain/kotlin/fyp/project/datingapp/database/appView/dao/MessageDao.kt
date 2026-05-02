package fyp.project.datingapp.database.appView.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fyp.project.datingapp.database.appView.entities.ConversationEntity
import fyp.project.datingapp.database.appView.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    //Get all active conversations for the Messages screen
    @Query("""
        SELECT * FROM conversations 
        WHERE isArchived = 0 
        ORDER BY lastMessageAt DESC
    """)
    fun getActiveConversations(): Flow<List<ConversationEntity>>

    //Get a specific conversation by peer DID
    @Query("SELECT * FROM conversations WHERE peerDid = :peerDid")
    suspend fun getConversation(peerDid: String): ConversationEntity?

    //Check if a conversation exists with this peer
    @Query("SELECT COUNT(*) > 0 FROM conversations WHERE peerDid = :peerDid")
    suspend fun hasConversation(peerDid: String): Boolean

    //Create or update a conversation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    //Update the conversation when a new (incoming) message arrives
    @Query("""
        UPDATE conversations
        SET lastMessagePreview = :preview,
            lastMessageAt = :timestamp,
            unreadCount = unreadCount + 1
        WHERE peerDid = :peerDid
    """)
    suspend fun onNewMessage(peerDid: String, preview: String, timestamp: Long)

    //Update the conversation for an outgoing message (no unread increment)
    @Query("""
        UPDATE conversations
        SET lastMessagePreview = :preview,
            lastMessageAt = :timestamp
        WHERE peerDid = :peerDid
    """)
    suspend fun onOutgoingMessage(peerDid: String, preview: String, timestamp: Long)

    //Mark conversation as read
    @Query("UPDATE conversations SET unreadCount = 0 WHERE peerDid = :peerDid")
    suspend fun markAsRead(peerDid: String)

    //Count total unread messages across all conversations
    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM conversations WHERE isArchived = 0")
    fun totalUnreadCount(): Flow<Int>

    // ---- message rows (Part 5 / M3) ---------------------------------------

    /**
     * Insert a message, ignoring duplicates by [MessageEntity.msgId] (an at-least-
     * once delivery from online + mailbox replay dedups here). Returns the inserted
     * rowId, or -1 if it was a duplicate.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE msgId = :msgId)")
    suspend fun hasMessage(msgId: String): Boolean

    //Messages in a conversation, oldest first (chat scroll order)
    @Query("SELECT * FROM messages WHERE conversationDid = :peerDid ORDER BY sentAt ASC")
    fun getMessages(peerDid: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET deliveryState = :state WHERE msgId = :msgId")
    suspend fun updateDeliveryState(msgId: String, state: String)
}