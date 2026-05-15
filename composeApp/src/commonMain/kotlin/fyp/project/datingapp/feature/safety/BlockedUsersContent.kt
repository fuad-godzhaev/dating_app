package fyp.project.datingapp.feature.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.components.aura.AuraTopBar
import fyp.project.datingapp.ui.theme.aura.AuraTheme

@Composable
fun BlockedUsersContent(component: BlockedUsersComponent) {
    val colors = AuraTheme.colors
    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Blocked users", onBack = component::onBack)
        Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "You haven't blocked anyone.",
                style = AuraTheme.text.body16,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
