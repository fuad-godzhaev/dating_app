package fyp.project.datingapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import datingapp.composeapp.generated.resources.Res
import datingapp.composeapp.generated.resources.ic_baseline_message_24
import fyp.project.datingapp.feature.home.components.SwipingDirection
import fyp.project.datingapp.feature.home.components.rememberSwipeableCardState
import fyp.project.datingapp.feature.home.components.swipableCard
import fyp.project.datingapp.ui.components.aura.ActionButtonsRow
import fyp.project.datingapp.ui.components.aura.Avatar
import fyp.project.datingapp.ui.components.aura.AuraLogo
import fyp.project.datingapp.ui.components.aura.ExpandedProfileSheet
import fyp.project.datingapp.ui.components.aura.PrimaryButton
import fyp.project.datingapp.ui.components.aura.ProfileCard
import fyp.project.datingapp.ui.theme.aura.AuraTheme
import fyp.project.datingapp.ui.theme.aura.auraBrush
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val colors = AuraTheme.colors
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf<HomeStore.State.ProfileCardState?>(null) }

    Box(Modifier.fillMaxSize().background(colors.bgBase)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top bar.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(size = 36.dp, modifier = Modifier.clickable(onClick = onNavigateToEditProfile))
                Spacer(Modifier.weight(1f))
                AuraLogo(ringSize = 28.dp, orbSize = 11.dp, ringStroke = 2.dp, glow = false)
                Spacer(Modifier.weight(1f))
                Icon(
                    painterResource(Res.drawable.ic_baseline_message_24),
                    contentDescription = "Messages",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(28.dp).clickable(onClick = onNavigateToMessages),
                )
            }

            when (val content = state.contentState) {
                HomeStore.State.ContentState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    AuraLogo()
                }
                is HomeStore.State.ContentState.Error -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(content.message, style = AuraTheme.text.body16, color = colors.textSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Retry", onClick = onLoadProfiles, modifier = Modifier.fillMaxWidth())
                }
                is HomeStore.State.ContentState.Loaded -> {
                    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                        if (content.profiles.isEmpty()) {
                            Text("No more profiles", style = AuraTheme.text.heading22, color = colors.textTertiary)
                        }
                        val swipeStates = content.profiles.map { rememberSwipeableCardState() }
                        content.profiles.forEachIndexed { index, card ->
                            ProfileCard(
                                name = card.profile.displayName,
                                age = card.profile.age,
                                meta = card.profile.bio?.takeIf { it.isNotBlank() } ?: "Nearby",
                                chips = card.profile.interests,
                                onInfoClick = { expanded = card },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(540.dp)
                                    .swipableCard(
                                        state = swipeStates[index],
                                        onSwiped = { dir ->
                                            onSwiped(card, dir == SwipingDirection.Right)
                                            onDismissProfile()
                                        },
                                    )
                                    .clickable { expanded = card },
                            )
                        }
                        ActionButtonsRow(
                            onPass = {
                                scope.launch {
                                    swipeStates.lastOrNull()?.swipe(SwipingDirection.Left)
                                    content.profiles.lastOrNull()?.let { onSwiped(it, false) }
                                    onDismissProfile()
                                }
                            },
                            onLike = {
                                scope.launch {
                                    swipeStates.lastOrNull()?.swipe(SwipingDirection.Right)
                                    content.profiles.lastOrNull()?.let { onSwiped(it, true) }
                                    onDismissProfile()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        )
                    }
                }
            }
        }

        // Expanded profile bottom sheet (card tap / info).
        expanded?.let { card ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { expanded = null },
                sheetState = sheetState,
                containerColor = colors.bgBase,
            ) {
                ExpandedProfileSheet(
                    name = card.profile.displayName,
                    age = card.profile.age,
                    meta = card.profile.bio?.takeIf { it.isNotBlank() } ?: "Nearby",
                    bio = card.profile.bio ?: "",
                    interests = card.profile.interests,
                    lookingFor = emptyList(),
                    onPass = { expanded = null; onSwiped(card, false); onDismissProfile() },
                    onLike = { expanded = null; onSwiped(card, true); onDismissProfile() },
                    onReport = { /* TODO(Report): open Report screen */ },
                )
            }
        }

        // It's-a-match overlay.
        state.dialog?.let { dialog ->
            MatchOverlay(
                title = dialog.matchDialog,
                onSend = onSendMatchMessage,
                onClose = onDismissDialog,
            )
        }
    }
}

@Composable
private fun MatchOverlay(
    title: String,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = AuraTheme.colors
    var draft by remember { mutableStateOf("") }
    Box(
        Modifier.fillMaxSize().background(colors.bgBase.copy(alpha = 0.96f)).systemBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(size = 120.dp, glow = true)
                Avatar(size = 120.dp, ring = true, initial = title.take(1).uppercase())
            }
            Spacer(Modifier.height(32.dp))
            Text("It's a match!", style = AuraTheme.text.display40.copy(brush = auraBrush(horizontal = true)))
            Spacer(Modifier.height(8.dp))
            Text(title, style = AuraTheme.text.body16, color = colors.textSecondary)
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(colors.bgSurface).padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (draft.isEmpty()) Text("Say hi", style = AuraTheme.text.body16, color = colors.textTertiary)
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = AuraTheme.text.body16.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accentTeal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val canSend = draft.isNotBlank()
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(if (canSend) auraBrush(horizontal = true) else SolidColor(colors.bgSurfaceRaised))
                        .clickable(enabled = canSend) { onSend(draft); draft = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("->", style = AuraTheme.text.button17, color = colors.textOnAccent)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Keep swiping", style = AuraTheme.text.label14, color = colors.textSecondary, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
    }
}
