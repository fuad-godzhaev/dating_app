package com.aura.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.aura.database.appView.entities.MessageEntity
import com.aura.ui.components.aura.Avatar
import com.aura.ui.components.aura.BackChevron
import com.aura.ui.theme.aura.AuraTheme
import com.aura.ui.theme.aura.auraBrush

@Composable
fun ChatContent(component: ChatComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding().imePadding()) {
        // Header.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackChevron(Modifier.size(20.dp).clickable(onClick = component::onBack))
            Avatar(size = 40.dp, initial = state.title.take(1).uppercase())
            Column(Modifier.weight(1f)) {
                Text(state.title, style = AuraTheme.text.body16, color = colors.textPrimary, maxLines = 1)
                Text("Active now", style = AuraTheme.text.caption13, color = colors.accentTeal)
            }
            // Overflow menu -> Report screen.
            Column(
                Modifier.size(width = 28.dp, height = 28.dp).clickable(onClick = component::onReport),
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                repeat(3) { Box(Modifier.size(4.dp).background(colors.textSecondary, CircleShape)) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderHairline))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.msgId }) { msg -> MessageBubble(msg) }
        }

        // Input bar.
        var draft by remember { mutableStateOf("") }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.bgSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (draft.isEmpty()) {
                    Text("Message", style = AuraTheme.text.body16, color = colors.textTertiary)
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = AuraTheme.text.body16.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accentTeal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val canSend = draft.isNotBlank() && !state.sending
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (canSend) auraBrush(horizontal = true) else SolidColor(colors.bgSurfaceRaised))
                    .clickable(enabled = canSend) {
                        component.onSend(draft)
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) colors.textOnAccent else colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: MessageEntity) {
    val colors = AuraTheme.colors
    val isOut = msg.direction == MessageEntity.DIRECTION_OUT
    val shape = if (isOut) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOut) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 256.dp)
                .clip(shape)
                .then(
                    if (isOut) Modifier.background(auraBrush(horizontal = true))
                    else Modifier.background(colors.bgSurfaceRaised),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                msg.plaintext,
                style = AuraTheme.text.body16,
                color = if (isOut) colors.textOnAccent else colors.textPrimary,
            )
        }
        if (isOut) {
            val label = when (msg.deliveryState) {
                MessageEntity.STATE_DELIVERED -> "Delivered"
                MessageEntity.STATE_SENT -> "Sent"
                MessageEntity.STATE_QUEUED -> "Queued"
                else -> null
            }
            if (label != null) {
                Text(
                    label,
                    style = AuraTheme.text.caption13,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp),
                )
            }
        }
    }
}
