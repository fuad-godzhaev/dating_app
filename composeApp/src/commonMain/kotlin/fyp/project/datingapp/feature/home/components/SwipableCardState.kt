package fyp.project.datingapp.feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** Cardinal direction of a completed swipe. */
enum class SwipingDirection { Left, Right, Up, Down }

/**
 * Multiplatform port of the prototype's `SwipeableCardState`.
 *
 * The prototype used `LocalConfiguration` (Android-only) to read
 * screen dimensions. Here we use `LocalWindowInfo.current.containerSize`,
 * which Compose Multiplatform exposes on every target. If the window
 * hasn't measured yet (zero size), we fall back to sane defaults so
 * previews and tests don't crash with a zero-pixel animation target.
 */
@Composable
fun rememberSwipeableCardState(): SwipeableCardState {
    val density = LocalDensity.current
    val container = LocalWindowInfo.current.containerSize
    val fallbackWidth = with(density) { 400.dp.toPx() }
    val fallbackHeight = with(density) { 800.dp.toPx() }
    val widthPx = if (container.width > 0) container.width.toFloat() else fallbackWidth
    val heightPx = if (container.height > 0) container.height.toFloat() else fallbackHeight
    return remember { SwipeableCardState(widthPx, heightPx) }
}

class SwipeableCardState(
    internal val maxWidth: Float,
    internal val maxHeight: Float,
) {
    val offset = Animatable(Offset(0f, 0f), Offset.VectorConverter)

    /** Null until the card has swiped fully off-screen in one direction. */
    var swipedDirection: SwipingDirection? by mutableStateOf(null)
        private set

    internal suspend fun reset() {
        offset.animateTo(Offset(0f, 0f), tween(400))
    }

    suspend fun swipe(
        direction: SwipingDirection,
        animationSpec: AnimationSpec<Offset> = tween(400),
    ) {
        val endX = maxWidth * 1.5f
        val endY = maxHeight
        val target = when (direction) {
            SwipingDirection.Left -> Offset(x = -endX, y = offset.value.y)
            SwipingDirection.Right -> Offset(x = endX, y = offset.value.y)
            SwipingDirection.Up -> Offset(x = offset.value.x, y = -endY)
            SwipingDirection.Down -> Offset(x = offset.value.x, y = endY)
        }
        offset.animateTo(target, animationSpec)
        swipedDirection = direction
    }

    internal suspend fun drag(x: Float, y: Float) {
        offset.snapTo(Offset(x, y))
    }
}
