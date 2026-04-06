package fyp.project.datingapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import datingapp.composeapp.generated.resources.Res
import datingapp.composeapp.generated.resources.tinder_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun AnimatedLogo(modifier: Modifier = Modifier, isAnimating: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedLogoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse)
    )
    Image(
        painter = painterResource(Res.drawable.tinder_logo),
        contentDescription = null,
        modifier = modifier.then(
            if (isAnimating) Modifier.graphicsLayer(scaleX = animatedLogoScale, scaleY = animatedLogoScale)
            else Modifier
        )
    )
}
