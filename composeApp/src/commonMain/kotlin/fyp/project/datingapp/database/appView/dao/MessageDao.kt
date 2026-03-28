package fyp.project.datingapp.database.appView.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fyp.project.datingapp.database.appView.entities.ConversationEntity
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

    //Update the conversation when a new message arrives
    @Query("""
        UPDATE conversations 
        SET lastMessagePreview = :preview, 
            lastMessageAt = :timestamp,
            unreadCount = unreadCount + 1
        WHERE peerDid = :peerDid
    """)
    suspend fun onNewMessage(peerDid: String, preview: String, timestamp: Long)

    //Mark conversation as read
    @Query("UPDATE conversations SET unreadCount = 0 WHERE peerDid = :peerDid")
    suspend fun markAsRead(peerDid: String)

    //Count total unread messages across all conversations
    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM conversations WHERE isArchived = 0")
    fun totalUnreadCount(): Flow<Int>
}