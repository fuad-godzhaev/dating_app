package fyp.project.datingapp.feature.chat

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.database.appView.dao.MessageDao
import fyp.project.datingapp.database.appView.entities.ConversationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Conversation-list screen (M6): observes the active conversations and opens a chat
 * on tap. Decompose `Value`-based (lighter than the MVIKotlin store used by Home).
 */
interface ConversationListComponent {
    val state: Value<State>
    fun onOpenChat(peerDid: String)
    fun onBack()

    data class State(val conversations: List<ConversationEntity> = emptyList())
}

class DefaultConversationListComponent(
    componentContext: ComponentContext,
    messageDao: MessageDao,
    private val onOpen: (String) -> Unit,
    private val onBackClick: () -> Unit,
) : ConversationListComponent, ComponentContext by componentContext {

    private val _state = MutableValue(ConversationListComponent.State())
    override val state: Value<ConversationListComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            messageDao.getActiveConversations().collect { list ->
                _state.value = ConversationListComponent.State(list)
            }
        }
    }

    override fun onOpenChat(peerDid: String) = onOpen(peerDid)
    override fun onBack() = onBackClick()
}
