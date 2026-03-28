package fyp.project.datingapp.database.appView.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["lastMessageAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val peerDid: String,

    val peerDisplayName: String,
    val lastMessagePreview: String? = null,
    val lastMessageAt: Long? = null,
    val unreadCount: Int = 0,

    val matchedAt: Long,
    val isArchived: Boolean = false
)