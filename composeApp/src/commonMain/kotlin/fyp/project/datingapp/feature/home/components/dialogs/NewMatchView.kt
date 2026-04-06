package fyp.project.datingapp.feature.home.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import datingapp.composeapp.generated.resources.Res
import datingapp.composeapp.generated.resources.its_a
import datingapp.composeapp.generated.resources.match
import datingapp.composeapp.generated.resources.say_something_nice
import datingapp.composeapp.generated.resources.send
import fyp.project.datingapp.feature.home.HomeStore
import fyp.project.datingapp.feature.home.components.ProfileCardView
import fyp.project.datingapp.ui.theme.Green1
import org.jetbrains.compose.resources.stringResource

/**
 * Full-bleed "It's a match!" dialog body. Ported from the prototype's
 * `NewMatchView`.
 *
 * Deviations from the original:
 *  - The prototype's `ChatFooter` is not ported (it lives in a Coil-
 *    dependent core module we're not bringing across); replaced with a
 *    simple [OutlinedTextField] + send button row.
 *  - Pictures are the placeholder boxes used elsewhere — see
 *    [ProfileCardView]. The "scale-in" text animation fires as soon as
 *    a Loaded picture is visible, matching the prototype's feel.
 */
@Composable
fun NewMatchView(
    pictureStates: List<HomeStore.State.PictureState>,
    onSendMessage: (String) -> Unit,
    onCloseClicked: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var currentIndex by remember { mutableStateOf(0) }
    var isTextVisible by remember { mutableStateOf(false) }
    val safeIndex = currentIndex.coerceIn(0, (pictureStates.size - 1).coerceAtLeast(0))
    val current = pictureStates.getOrNull(safeIndex)

    // Trigger the scale-in once a picture is considered "loaded". Our
    // placeholder boxes report Loaded immediately, which matches the
    // prototype's post-AsyncImage behaviour from the user's POV.
    LaunchedEffect(current) {
        if (current is HomeStore.State.PictureState.Loaded) isTextVisible = true
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Picture slot — placeholder or progress.
        when (current) {
            null, is HomeStore.State.PictureState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Color.White) }
            is HomeStore.State.PictureState.Loaded -> fyp.project.datingapp.feature.home.components.PlaceholderBackdrop(current.ref)
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            enter = scaleIn(tween(300, easing = LinearEasing), initialScale = 5f),
            exit = fadeOut(),
            visible = isTextVisible,
        ) {
            val density = LocalDensity.current
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.its_a).uppercase(),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        color = Green1,
                        fontStyle = FontStyle.Italic,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
                Text(
                    text = stringResource(Res.string.match).uppercase(),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        color = Green1,
                        fontStyle = FontStyle.Italic,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = Color.Blue.copy(alpha = .5f),
                            offset = with(density) { Offset(4.dp.toPx(), 10.dp.toPx()) },
                            blurRadius = 3f,
                        ),
                    ),
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            // Picture-index strip.
            Row(Modifier.fillMaxWidth().padding(6.dp)) {
                repeat(pictureStates.size.coerceAtLeast(1)) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .padding(horizontal = 4.dp)
                            .alpha(if (index == safeIndex) 1f else .5f)
                            .background(if (index == safeIndex) Color.White else Color.LightGray),
                    )
                }
            }
            // Tap zones.
            Row(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            if (currentIndex > 0) currentIndex--
                            isTextVisible = false
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            if (currentIndex < pictureStates.size - 1) currentIndex++
                            isTextVisible = false
                        },
                )
            }

            IconButton(
                onClick = onCloseClicked,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            SimpleChatFooter(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                onSendClicked = onSendMessage,
            )
        }
    }
}

/**
 * Replacement for the prototype's `core_ui.components.ChatFooter`.
 * Minimal to keep scope tight — just text + send button. Messaging is
 * deliberately a no-op until the chat feature lands.
 */
@Composable
private fun SimpleChatFooter(
    modifier: Modifier = Modifier,
    onSendClicked: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(Res.string.say_something_nice)) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.isNotBlank()) {
                    onSendClicked(text)
                    text = ""
                }
            }),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSendClicked(text)
                    text = ""
                }
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = stringResource(Res.string.send),
                tint = Color.White,
            )
        }
    }
}
