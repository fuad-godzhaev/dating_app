package com.aura.feature.chat

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.aura.database.appView.dao.MessageDao
import com.aura.database.appView.entities.MessageEntity
import com.aura.p2p.messaging.MessageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 1:1 chat screen (M6): streams the conversation's messages and sends new ones via
 * [MessageService] (which E2E-encrypts, signs, and delivers them).
 */
interface ChatComponent {
    val state: Value<State>
    fun onSend(text: String)
    fun onBack()
    fun onReport()

    data class State(
        val peerDid: String,
        val title: String,
        val messages: List<MessageEntity> = emptyList(),
        val sending: Boolean = false,
    )
}

class DefaultChatComponent(
    componentContext: ComponentContext,
    private val peerDid: String,
    private val messageDao: MessageDao,
    private val messageService: MessageService,
    private val onBackClick: () -> Unit,
    private val onReportClick: (String, String) -> Unit = { _, _ -> },
) : ChatComponent, ComponentContext by componentContext {

    private val _state = MutableValue(ChatComponent.State(peerDid = peerDid, title = peerDid))
    override val state: Value<ChatComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            messageDao.getMessages(peerDid).collect { msgs ->
                _state.value = _state.value.copy(messages = msgs)
            }
        }
        scope.launch {
            messageDao.getConversation(peerDid)?.let {
                _state.value = _state.value.copy(title = it.peerDisplayName)
            }
            messageDao.markAsRead(peerDid)
        }
    }

    override fun onSend(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _state.value = _state.value.copy(sending = true)
        scope.launch {
            runCatching { messageService.sendMessage(peerDid, trimmed) }
            _state.value = _state.value.copy(sending = false)
        }
    }

    override fun onBack() = onBackClick()

    override fun onReport() = onReportClick(peerDid, _state.value.title)
}
