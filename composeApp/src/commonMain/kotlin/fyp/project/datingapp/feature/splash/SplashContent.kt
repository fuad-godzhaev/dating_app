package fyp.project.datingapp.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import fyp.project.datingapp.ui.components.aura.AuraLogo
import fyp.project.datingapp.ui.components.aura.AuraWordmark
import fyp.project.datingapp.ui.components.aura.AuroraBackground
import fyp.project.datingapp.ui.components.aura.PrimaryButton
import fyp.project.datingapp.ui.theme.aura.AuraTheme

@Composable
fun SplashContent(component: SplashComponent) {
    val state by component.state.subscribeAsState()
    SplashScreen(state = state, onRetry = component::onRetry)
}

@Composable
private fun SplashScreen(
    state: SplashComponent.State,
    onRetry: () -> Unit,
) {
    val colors = AuraTheme.colors
    AuroraBackground {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            AuraLogo()
            Spacer(Modifier.height(32.dp))
            AuraWordmark()
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Text(
                "Connection, in your own light.",
                style = AuraTheme.text.body16,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))

            if (state is SplashComponent.State.Error) {
                androidx.compose.material3.Text(
                    "Something went wrong.",
                    style = AuraTheme.text.body16,
                    color = colors.statePassStrong,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
                PrimaryButton("Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            } else {
                LoadingDots()
            }
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Text(
                "Decentralized · private · yours",
                style = AuraTheme.text.caption13,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LoadingDots() {
    val colors = AuraTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (i == 0) colors.accentTeal else colors.borderStrong, CircleShape),
            )
        }
    }
}

@Preview
@Composable
fun SplashContentLoadingPreview() {
    AuraTheme { SplashScreen(state = SplashComponent.State.Loading, onRetry = {}) }
}
