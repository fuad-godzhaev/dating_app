package fyp.project.datingapp.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.components.aura.Avatar
import fyp.project.datingapp.ui.components.aura.BackChevron
import fyp.project.datingapp.ui.components.aura.HairlineDivider
import fyp.project.datingapp.ui.theme.aura.AuraTheme

@Composable
fun ConversationListContent(component: ConversationListComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        // Header: back + big "Messages" title (AURA_DESIGN_SPEC §6.14).
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackChevron(Modifier.size(20.dp).clickable(onClick = component::onBack))
            Text("Messages", style = AuraTheme.text.title28, color = colors.textPrimary)
        }
        // TODO(Orbit): likes-you pill + new-matches strip once those flows exist.

        if (state.conversations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No conversations yet", style = AuraTheme.text.body16, color = colors.textTertiary)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.conversations, key = { it.peerDid }) { convo ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { component.onOpenChat(convo.peerDid) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(size = 54.dp, initial = convo.peerDisplayName.take(1).uppercase())
                        Column(Modifier.weight(1f)) {
                            Text(convo.peerDisplayName, style = AuraTheme.text.body16, color = colors.textPrimary)
                            convo.lastMessagePreview?.let {
                                Text(it, style = AuraTheme.text.caption13, color = colors.textSecondary, maxLines = 1)
                            }
                        }
                        if (convo.unreadCount > 0) {
                            Box(Modifier.size(10.dp).background(colors.accentTeal, CircleShape))
                        }
                    }
                    HairlineDivider(inset = 90.dp)
                }
            }
        }
        Spacer(Modifier.size(0.dp))
    }
}
