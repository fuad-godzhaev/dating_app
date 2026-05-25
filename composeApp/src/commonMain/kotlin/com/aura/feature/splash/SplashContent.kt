package com.aura.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.aura.ui.components.aura.AuraLogo
import com.aura.ui.components.aura.AuraWordmark
import com.aura.ui.components.aura.AuroraBackground
import com.aura.ui.components.aura.PrimaryButton
import com.aura.ui.theme.aura.AuraTheme

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
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700)) }
    AuroraBackground {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = appear.value
                    val s = 0.85f + 0.15f * appear.value
                    scaleX = s
                    scaleY = s
                },
            ) {
                AuraLogo()
                Spacer(Modifier.height(32.dp))
                AuraWordmark()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Connection, in your own light.",
                    style = AuraTheme.text.body16,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))

            if (state is SplashComponent.State.Error) {
                Text(
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
            Text(
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
    val transition = rememberInfiniteTransition(label = "loadingDots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val a by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 200),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        alpha = a
                        val s = 0.7f + 0.3f * a
                        scaleX = s
                        scaleY = s
                    }
                    .background(colors.accentTeal, CircleShape),
            )
        }
    }
}

@Preview
@Composable
fun SplashContentLoadingPreview() {
    AuraTheme { SplashScreen(state = SplashComponent.State.Loading, onRetry = {}) }
}
