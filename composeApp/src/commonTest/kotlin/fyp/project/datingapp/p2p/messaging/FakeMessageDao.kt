package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.database.appView.dao.MessageDao
import fyp.project.datingapp.database.appView.entities.ConversationEntity
import fyp.project.datingapp.database.appView.entities.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** In-memory [MessageDao] for commonTest (messages + conversation headers). */
class FakeMessageDao : MessageDao {
    val conversations = mutableMapOf<String, ConversationEntity>()
    val messages = mutableMapOf<String, MessageEntity>()

    override fun getActiveConversations(): Flow<List<ConversationEntity>> =
        flowOf(conversations.values.filterNot { it.isArchived }.sortedByDescending { it.lastMessageAt ?: 0 })

    override suspend fun getConversation(peerDid: String): ConversationEntity? = conversations[peerDid]

    override suspend fun hasConversation(peerDid: String): Boolean = conversations.containsKey(peerDid)

    override suspend fun upsertConversation(conversation: ConversationEntity) {
        conversations[conversation.peerDid] = conversation
    }

    override suspend fun onNewMessage(peerDid: String, preview: String, timestamp: Long) {
        conversations[peerDid]?.let {
            conversations[peerDid] = it.copy(
                lastMessagePreview = preview,
                lastMessageAt = timestamp,
                unreadCount = it.unreadCount + 1,
            )
        }
    }

    override suspend fun onOutgoingMessage(peerDid: String, preview: String, timestamp: Long) {
        conversations[peerDid]?.let {
            conversations[peerDid] = it.copy(lastMessagePreview = preview, lastMessageAt = timestamp)
        }
    }

    override suspend fun markAsRead(peerDid: String) {
        conversations[peerDid]?.let { conversations[peerDid] = it.copy(unreadCount = 0) }
    }

    override fun totalUnreadCount(): Flow<Int> =
        flowOf(conversations.values.filterNot { it.isArchived }.sumOf { it.unreadCount })

    override suspend fun insertMessage(message: MessageEntity): Long {
        if (messages.containsKey(message.msgId)) return -1L
        messages[message.msgId] = message
        return 1L
    }

    override suspend fun hasMessage(msgId: String): Boolean = messages.containsKey(msgId)

    override fun getMessages(peerDid: String): Flow<List<MessageEntity>> =
        flowOf(messages.values.filter { it.conversationDid == peerDid }.sortedBy { it.sentAt })

    override suspend fun updateDeliveryState(msgId: String, state: String) {
        messages[msgId]?.let { messages[msgId] = it.copy(deliveryState = state) }
    }
}
