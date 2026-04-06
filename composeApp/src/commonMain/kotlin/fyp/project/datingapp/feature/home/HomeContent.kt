package fyp.project.datingapp.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import datingapp.composeapp.generated.resources.*
import fyp.project.datingapp.feature.home.components.*
import fyp.project.datingapp.feature.home.components.dialogs.NewMatchDialog
import fyp.project.datingapp.ui.components.AnimatedLogo
import fyp.project.datingapp.ui.components.GradientButton
import fyp.project.datingapp.ui.theme.Green1
import fyp.project.datingapp.ui.theme.Green2
import fyp.project.datingapp.ui.theme.Orange
import fyp.project.datingapp.ui.theme.Pink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Decompose-aware Home screen entry. Subscribes to the component's
 * state and forwards intents. Mirrors the prototype's `HomeView`
 * layout 1:1.
 */
@Composable
fun HomeContent(component: HomeComponent) {
    val state by component.state.subscribeAsState()
    HomeContent(
        state = state,
        onLoadProfiles = component::onLoadProfiles,
        onSwiped = component::onSwiped,
        onDismissProfile = component::onDismissProfile,
        onDismissDialog = component::onDismissDialog,
        onSendMatchMessage = component::onSendMatchMessage,
        onNavigateToEditProfile = component::onNavigateToEditProfile,
        onNavigateToMessages = component::onNavigateToMessages,
    )
}

/**
 * Stateless body — split out so an @Preview can drive it with a
 * fake [HomeStore.State] and so UI tests can instantiate it without
 * going through Decompose.
 */
@Composable
fun HomeContent(
    state: HomeStore.State,
    onLoadProfiles: () -> Unit,
    onSwiped: (HomeStore.State.ProfileCardState, Boolean) -> Unit,
    onDismissProfile: () -> Unit,
    onDismissDialog: () -> Unit,
    onSendMatchMessage: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToMessages: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    state.dialog?.let { dialog ->
        NewMatchDialog(
            pictureStates = dialog.pictureBlobs,
            onSendMessage = onSendMatchMessage,
            onCloseClicked = onDismissDialog,
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TopBarIcon(
                    imageVector = Icons.Filled.AccountCircle,
                    onClick = onNavigateToEditProfile,
                )
                Spacer(Modifier.weight(1f))
                TopBarIcon(
                    resource = Res.drawable.tinder_logo,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.weight(1f))
                TopBarIcon(
                    resource = Res.drawable.ic_baseline_message_24,
                    onClick = onNavigateToMessages,
                )
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val content = state.contentState) {
                is HomeStore.State.ContentState.Error -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = content.message,
                        color = Color.Gray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    GradientButton(onClick = {
                        scope.launch {
                            delay(200)
                            onLoadProfiles()
                        }
                    }) {
                        Text(stringResource(Res.string.retry))
                    }
                    Spacer(Modifier.weight(1f))
                }
                HomeStore.State.ContentState.Loading -> {
                    Spacer(Modifier.weight(1f))
                    AnimatedLogo(
                        modifier = Modifier.fillMaxWidth(.4f),
                        isAnimating = true,
                    )
                    Spacer(Modifier.weight(1f))
                }
                is HomeStore.State.ContentState.Loaded -> {
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        // Always-present "nothing left" copy sits *behind* the
                        // stack — the cards occlude it until they drain.
                        Text(
                            text = stringResource(Res.string.no_more_profiles),
                            color = Color.Gray,
                            fontSize = 20.sp,
                        )
                        val localDensity = LocalDensity.current
                        var buttonRowHeightDp by remember { mutableStateOf(0.dp) }

                        val swipeStates = content.profiles.map { rememberSwipeableCardState() }
                        content.profiles.forEachIndexed { index, card ->
                            ProfileCardView(
                                profile = card.profile,
                                pictures = card.pictureBlobs,
                                modifier = Modifier.swipableCard(
                                    state = swipeStates[index],
                                    onSwiped = { direction ->
                                        onSwiped(card, direction == SwipingDirection.Right)
                                        onDismissProfile()
                                    },
                                ),
                                contentModifier = Modifier.padding(
                                    bottom = buttonRowHeightDp.plus(8.dp),
                                ),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(vertical = 10.dp)
                                .onGloballyPositioned { coordinates ->
                                    buttonRowHeightDp = with(localDensity) {
                                        coordinates.size.height.toDp()
                                    }
                                },
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Spacer(Modifier.weight(1f))
                            RoundGradientButton(
                                onClick = {
                                    scope.launch {
                                        swipeStates.lastOrNull()?.swipe(SwipingDirection.Left)
                                        onDismissProfile()
                                    }
                                },
                                enabled = swipeStates.isNotEmpty(),
                                imageVector = Icons.Filled.Close,
                                color1 = Pink,
                                color2 = Orange,
                            )
                            Spacer(Modifier.weight(.5f))
                            RoundGradientButton(
                                onClick = {
                                    scope.launch {
                                        swipeStates.lastOrNull()?.swipe(SwipingDirection.Right)
                                        onDismissProfile()
                                    }
                                },
                                enabled = swipeStates.isNotEmpty(),
                                imageVector = Icons.Filled.Favorite,
                                color1 = Green1,
                                color2 = Green2,
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
