package com.aura.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import aura.composeapp.generated.resources.Res
import aura.composeapp.generated.resources.ic_baseline_message_24
import com.aura.feature.home.components.SwipingDirection
import com.aura.feature.home.components.rememberSwipeableCardState
import com.aura.feature.home.components.swipableCard
import com.aura.p2p.discovery.FeedFilterPrefs
import com.aura.ui.components.aura.ActionButtonsRow
import com.aura.ui.components.aura.Avatar
import com.aura.ui.components.aura.AuraLogo
import com.aura.ui.components.aura.AuraTextField
import com.aura.ui.components.aura.ExpandedProfileSheet
import com.aura.ui.components.aura.PrimaryButton
import com.aura.ui.components.aura.ProfileCard
import com.aura.ui.theme.aura.AuraTheme
import com.aura.ui.theme.aura.auraBrush
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
        onReport = component::onReport,
        onOpenFilters = component::onOpenFilters,
        onCloseFilters = component::onCloseFilters,
        onApplyFilters = component::onApplyFilters,
    )
}

/** All fully-fetched photo paths for a card, in order (the card + sheet cycle through these
 *  as the blob fetch flips each picture Loading -> Loaded). */
private fun HomeStore.State.ProfileCardState.photoPaths(): List<String> =
    pictureBlobs.mapNotNull { (it as? HomeStore.State.PictureState.Loaded)?.filePath }

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
    onReport: (String, String) -> Unit,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onApplyFilters: (FeedFilterPrefs) -> Unit,
) {
    val colors = AuraTheme.colors
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf<HomeStore.State.ProfileCardState?>(null) }

    Box(Modifier.fillMaxSize().background(colors.bgBase)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top bar: avatar (left) · aura mark (centered) · filter + messages (right).
            // Equal-weight side slots keep the centre mark optically centred.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    Avatar(size = 36.dp, photoPath = state.selfPhotoPath, modifier = Modifier.clickable(onClick = onNavigateToEditProfile))
                }
                AuraLogo(ringSize = 28.dp, orbSize = 11.dp, ringStroke = 2.dp, glow = false)
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterIcon(onClick = onOpenFilters, modifier = Modifier.size(28.dp))
                    Icon(
                        painterResource(Res.drawable.ic_baseline_message_24),
                        contentDescription = "Messages",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp).clickable(onClick = onNavigateToMessages),
                    )
                }
            }

            when (val content = state.contentState) {
                HomeStore.State.ContentState.Loading -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AuraLogo()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Looking for new matches!",
                        style = AuraTheme.text.heading22,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "We're scanning for people nearby over Bluetooth and Wi-Fi. Please check back in a little while.",
                        style = AuraTheme.text.body16,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("You're all caught up", style = AuraTheme.text.heading22, color = colors.textPrimary)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No more profiles nearby right now — check back later for new people.",
                                    style = AuraTheme.text.body16,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        // Render only the top few cards as a peek stack: the topmost
                        // (last) is interactive; cards behind sit scaled + offset up.
                        val visible = content.profiles.takeLast(3)
                        val topCard = content.profiles.lastOrNull()
                        val topSwipe = key(topCard?.profile?.did) { rememberSwipeableCardState() }
                        visible.forEachIndexed { idx, card ->
                            val depth = visible.lastIndex - idx
                            key(card.profile.did) {
                                val isTop = depth == 0
                                val scale by animateFloatAsState(1f - depth * 0.05f, tween(250), label = "cardScale")
                                val yShift by animateFloatAsState(depth * -16f, tween(250), label = "cardShift")
                                val dim by animateFloatAsState(1f - depth * 0.10f, tween(250), label = "cardDim")
                                var cardMod = Modifier
                                    .fillMaxWidth()
                                    .height(540.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationY = yShift.dp.toPx()
                                        alpha = dim
                                    }
                                if (isTop) {
                                    // Only swipe on the top card; tapping the card cycles photos
                                    // (handled inside ProfileCard), and the "i" button opens details.
                                    cardMod = cardMod.swipableCard(
                                        state = topSwipe,
                                        onSwiped = { dir ->
                                            onSwiped(card, dir == SwipingDirection.Right)
                                            onDismissProfile()
                                        },
                                    )
                                }
                                ProfileCard(
                                    name = card.profile.displayName,
                                    age = card.profile.age,
                                    meta = card.profile.bio?.takeIf { it.isNotBlank() } ?: "Nearby",
                                    chips = card.profile.interests,
                                    photoPaths = card.photoPaths(),
                                    onInfoClick = if (isTop) ({ expanded = card }) else null,
                                    modifier = cardMod,
                                )
                            }
                        }
                        // Pass/Like only make sense when there's a card to act on.
                        if (content.profiles.isNotEmpty()) {
                            ActionButtonsRow(
                                onPass = {
                                    scope.launch {
                                        topSwipe.swipe(SwipingDirection.Left)
                                        topCard?.let { onSwiped(it, false) }
                                        onDismissProfile()
                                    }
                                },
                                onLike = {
                                    scope.launch {
                                        topSwipe.swipe(SwipingDirection.Right)
                                        topCard?.let { onSwiped(it, true) }
                                        onDismissProfile()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            )
                        }
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
                    onReport = { expanded = null; onReport(card.profile.did, card.profile.displayName) },
                    photoPaths = card.photoPaths(),
                )
            }
        }

        // Feed-filter bottom sheet (Filter icon). Editing + applying persists the prefs
        // and restarts the feed so only matching profiles are shown.
        if (state.filterSheetOpen) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onCloseFilters,
                sheetState = sheetState,
                containerColor = colors.bgBase,
            ) {
                FilterSheet(initial = state.filters, onApply = onApplyFilters)
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

/** Small funnel glyph used as the feed-filter entry point in the top bar. */
@Composable
private fun FilterIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = AuraTheme.colors.textPrimary
    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val sw = 2.dp.toPx()
            drawLine(tint, Offset(w * 0.12f, h * 0.28f), Offset(w * 0.88f, h * 0.28f), sw, StrokeCap.Round)
            drawLine(tint, Offset(w * 0.28f, h * 0.5f), Offset(w * 0.72f, h * 0.5f), sw, StrokeCap.Round)
            drawLine(tint, Offset(w * 0.42f, h * 0.72f), Offset(w * 0.58f, h * 0.72f), sw, StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(initial: FeedFilterPrefs, onApply: (FeedFilterPrefs) -> Unit) {
    val colors = AuraTheme.colors
    var dist by remember { mutableStateOf(initial.maxDistanceKm.toFloat()) }
    var ageRange by remember { mutableStateOf(initial.ageMin.toFloat()..initial.ageMax.toFloat()) }
    var gender by remember { mutableStateOf(initial.gender ?: "") }
    var interestsText by remember { mutableStateOf(initial.interests.joinToString(", ")) }
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.accentTeal,
        activeTrackColor = colors.accentTeal,
        inactiveTrackColor = colors.bgSurfaceRaised,
    )

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Filters", style = AuraTheme.text.heading22, color = colors.textPrimary)

        Text(
            "Distance: " + (if (dist <= 0f) "Any" else "${dist.toInt()} km"),
            style = AuraTheme.text.label14,
            color = colors.textSecondary,
        )
        Slider(value = dist, onValueChange = { dist = it }, valueRange = 0f..200f, colors = sliderColors)

        Text(
            "Age: ${ageRange.start.toInt()} - ${ageRange.endInclusive.toInt()}",
            style = AuraTheme.text.label14,
            color = colors.textSecondary,
        )
        RangeSlider(value = ageRange, onValueChange = { ageRange = it }, valueRange = 18f..99f, colors = sliderColors)

        Text("Show me", style = AuraTheme.text.label14, color = colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("" to "Any", "woman" to "Women", "man" to "Men", "nonbinary" to "Nonbinary").forEach { (value, label) ->
                GenderChip(label = label, selected = gender == value) { gender = value }
            }
        }

        AuraTextField(
            value = interestsText,
            onValueChange = { interestsText = it },
            label = "Interests (comma-separated)",
            placeholder = "e.g. hiking, music",
        )

        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            "Apply filters",
            onClick = {
                onApply(
                    FeedFilterPrefs(
                        maxDistanceKm = dist.toInt(),
                        ageMin = ageRange.start.toInt(),
                        ageMax = ageRange.endInclusive.toInt(),
                        gender = gender.takeIf { it.isNotBlank() },
                        interests = interestsText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Reset",
            style = AuraTheme.text.label14,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth().clickable { onApply(FeedFilterPrefs()) }.padding(8.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GenderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) colors.accentTeal else colors.bgSurfaceRaised)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.borderStrong, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = AuraTheme.text.label14,
            color = if (selected) colors.textOnAccent else colors.textSecondary,
        )
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
    val appear = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { appear.animateTo(1f, tween(400)) }
        burst.animateTo(1f, tween(1500))
    }
    val pulse = rememberInfiniteTransition(label = "matchPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val confetti = remember { buildConfetti() }

    Box(
        Modifier.fillMaxSize().background(colors.bgBase.copy(alpha = 0.96f)).systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val p = burst.value
            confetti.forEach { c ->
                val x = size.width * (c.cx + c.dx * p)
                val y = size.height * (c.cy + c.dy * p) + size.height * 0.35f * p * p
                val a = (1f - p).coerceIn(0f, 1f)
                drawCircle(
                    color = (if (c.violet) colors.accentViolet else colors.accentTeal).copy(alpha = a),
                    radius = c.size,
                    center = Offset(x, y),
                )
            }
        }
        Column(
            Modifier.padding(24.dp).graphicsLayer {
                alpha = appear.value
                val s = 0.9f + 0.1f * appear.value
                scaleX = s
                scaleY = s
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(size = 120.dp, glow = true, modifier = Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale })
                Avatar(
                    size = 120.dp,
                    ring = true,
                    initial = title.take(1).uppercase(),
                    modifier = Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
                )
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
                    SendArrow(tint = colors.textOnAccent)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Keep swiping", style = AuraTheme.text.label14, color = colors.textSecondary, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
    }
}

@Composable
private fun SendArrow(tint: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()
        drawLine(tint, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.55f, h * 0.28f), Offset(w * 0.84f, h * 0.5f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.55f, h * 0.72f), Offset(w * 0.84f, h * 0.5f), sw, StrokeCap.Round)
    }
}

private class Confetto(
    val cx: Float,
    val cy: Float,
    val dx: Float,
    val dy: Float,
    val size: Float,
    val violet: Boolean,
)

private fun buildConfetti(): List<Confetto> = List(30) {
    val angle = Random.nextDouble() * 2.0 * PI
    val speed = 0.15 + Random.nextDouble() * 0.5
    Confetto(
        cx = 0.5f,
        cy = 0.34f,
        dx = (cos(angle) * speed).toFloat(),
        dy = (sin(angle) * speed - 0.25).toFloat(),
        size = (3.0 + Random.nextDouble() * 4.0).toFloat(),
        violet = Random.nextBoolean(),
    )
}
