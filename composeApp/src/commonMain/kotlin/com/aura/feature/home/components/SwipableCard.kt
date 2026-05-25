package com.aura.feature.home.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Applies Tinder-style swipe gestures to the receiver [Modifier].
 *
 * - [state] — tracks the current drag offset + committed direction.
 * - [onSwiped] — fires once a swipe clears the third-of-screen threshold.
 * - [onSwipeCancel] — fires when the gesture ends below threshold.
 * - [blockedDirections] — default blocks vertical so only horizontal
 *   swipes commit, matching the prototype's Tinder behaviour.
 */
fun Modifier.swipableCard(
    state: SwipeableCardState,
    onSwiped: (SwipingDirection) -> Unit,
    onSwipeCancel: () -> Unit = {},
    blockedDirections: List<SwipingDirection> = listOf(SwipingDirection.Up, SwipingDirection.Down),
): Modifier = this
    .pointerInput(Unit) {
        coroutineScope {
            detectDragGestures(
                onDragCancel = {
                    launch {
                        state.reset()
                        onSwipeCancel()
                    }
                },
                onDrag = { change, dragAmount ->
                    val summed = state.offset.targetValue + dragAmount
                    val x = summed.x.coerceIn(-state.maxWidth, state.maxWidth)
                    val y = summed.y.coerceIn(-state.maxHeight, state.maxHeight)
                    if (change.positionChange() != Offset.Zero) change.consume()
                    launch { state.drag(x, y) }
                },
                onDragEnd = {
                    val coerced = state.offset.targetValue.clampToAllowed(
                        blockedDirections, state.maxWidth, state.maxHeight,
                    )
                    if (coerced.isBelowThreshold(state)) {
                        launch {
                            state.reset()
                            onSwipeCancel()
                        }
                    } else {
                        val o = state.offset.targetValue
                        val direction = when {
                            abs(o.x) > abs(o.y) -> if (o.x > 0) SwipingDirection.Right else SwipingDirection.Left
                            else -> if (o.y < 0) SwipingDirection.Up else SwipingDirection.Down
                        }
                        launch {
                            state.swipe(direction)
                            onSwiped(direction)
                        }
                    }
                },
            )
        }
    }
    .graphicsLayer {
        translationX = state.offset.value.x
        translationY = state.offset.value.y
        rotationZ = (state.offset.value.x / 60).coerceIn(-40f, 40f)
    }

private fun Offset.clampToAllowed(
    blocked: List<SwipingDirection>,
    maxWidth: Float,
    maxHeight: Float,
): Offset = Offset(
    x = x.coerceIn(
        minimumValue = if (SwipingDirection.Left in blocked) 0f else -maxWidth,
        maximumValue = if (SwipingDirection.Right in blocked) 0f else maxWidth,
    ),
    y = y.coerceIn(
        minimumValue = if (SwipingDirection.Up in blocked) 0f else -maxHeight,
        maximumValue = if (SwipingDirection.Down in blocked) 0f else maxHeight,
    ),
)

private fun Offset.isBelowThreshold(state: SwipeableCardState): Boolean =
    abs(x) < state.maxWidth / 3 && abs(y) < state.maxHeight / 3
