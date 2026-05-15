package fyp.project.datingapp.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.components.aura.AuraTopBar
import fyp.project.datingapp.ui.theme.aura.AuraTheme

@Composable
fun LegalContent(component: LegalComponent) {
    val colors = AuraTheme.colors
    val (title, body) = when (component.kind) {
        LegalComponent.Kind.PRIVACY -> "Privacy policy" to PRIVACY_BODY
        LegalComponent.Kind.TERMS -> "Terms of use" to TERMS_BODY
    }
    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = title, onBack = component::onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            body.forEach { para ->
                Text(para, style = AuraTheme.text.body16, color = colors.textSecondary)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val PRIVACY_BODY = listOf(
    "aura is a serverless, peer-to-peer application. There is no central server that stores " +
        "your profile, messages, or location.",
    "Your identity and signing keys are generated and held on your device. Profiles you " +
        "publish are shared directly with nearby peers; messages are end-to-end encrypted to " +
        "the recipient's key and are never readable by anyone in between.",
    "Approximate location is used only to derive a coarse discovery cell so you can find " +
        "people nearby; precise coordinates are not shared. Discovery and Bluetooth features " +
        "are opt-in and can be turned off in Settings.",
    "Because there is no backend, deleting the app and signing out removes your identity " +
        "from this device. This is a research preview built for a final-year project.",
)

private val TERMS_BODY = listOf(
    "aura is provided as a research preview for a final-year project, without warranty of " +
        "any kind. Use it at your own risk.",
    "You are responsible for your own conduct and content. Be respectful, do not harass " +
        "other people, and do not use the app for anything unlawful.",
    "Because the network is decentralised, content you publish may be cached and relayed by " +
        "other peers to improve availability. Do not share anything you would not want " +
        "redistributed.",
    "These terms may change as the project evolves. Continued use indicates acceptance of " +
        "the current terms.",
)
