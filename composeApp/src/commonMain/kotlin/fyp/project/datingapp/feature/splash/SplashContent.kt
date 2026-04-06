package fyp.project.datingapp.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import fyp.project.datingapp.ui.components.AnimatedLogo
import fyp.project.datingapp.ui.theme.Orange
import fyp.project.datingapp.ui.theme.Pink
import fyp.project.datingapp.ui.theme.TinderCloneComposeTheme

@Composable
fun SplashContent(component: SplashComponent) {
    val state by component.state.subscribeAsState()
    SplashScreen(
        state = state,
        onRetry = component::onRetry,
    )
}

@Composable
private fun SplashScreen(
    state: SplashComponent.State,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.horizontalGradient(listOf(Pink, Orange)))
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        AnimatedLogo(
            modifier = Modifier.fillMaxWidth(.4f),
            isAnimating = state is SplashComponent.State.Loading,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (state is SplashComponent.State.Error) {
            Text(
                text = "Something went wrong. Please try again.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
            )
            Button(onClick = onRetry, modifier = Modifier.padding(bottom = 44.dp)) {
                Text("Retry")
            }
        } else {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.padding(bottom = 44.dp),
            )
        }
    }
}

@Preview
@Composable
fun SplashContentLoadingPreview() {
    TinderCloneComposeTheme {
        SplashScreen(state = SplashComponent.State.Loading, onRetry = {})
    }
}

@Preview
@Composable
fun SplashContentErrorPreview() {
    TinderCloneComposeTheme {
        SplashScreen(state = SplashComponent.State.Error, onRetry = {})
    }
}
