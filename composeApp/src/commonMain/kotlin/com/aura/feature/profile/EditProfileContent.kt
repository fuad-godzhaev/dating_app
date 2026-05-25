package com.aura.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.aura.p2p.blob.UploadedPhoto
import com.aura.ui.theme.aura.auraDiagonalBrush
import com.aura.ui.components.aura.AddChip
import com.aura.ui.components.aura.AuraChip
import com.aura.ui.components.aura.AuraStepper
import com.aura.ui.components.aura.AuraTextField
import com.aura.ui.components.aura.AuraTopBar
import com.aura.ui.components.aura.PrimaryButton
import com.aura.ui.theme.aura.AuraTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileContent(component: EditProfileComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors

    val picker = rememberImagePicker { bytes, mime -> component.onPhotoPicked(bytes, mime) }
    var showAddInterest by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Edit profile", onBack = component::onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Photos grid (spec §6.11): a fixed 3x2 grid. Filled tiles carry a remove
            // badge; empty slots are dashed add tiles that open the OS picker.
            PhotoGrid(
                photos = state.photos,
                onAdd = { picker.launch() },
                onRemove = component::onRemovePhoto,
            )
            AuraTextField(state.displayName, component::onDisplayNameChanged, label = "Display name", placeholder = "Your name")
            AuraTextField(state.bio, component::onBioChanged, label = "Bio", placeholder = "A little about you", singleLine = false, minHeight = 96.dp)
            AuraStepper("Age", state.age, onDecrement = { component.onAgeChanged((state.age - 1).coerceAtLeast(18)) }, onIncrement = { component.onAgeChanged(state.age + 1) })
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Interests", style = AuraTheme.text.label14, color = colors.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.interests.forEach { interest ->
                        AuraChip(interest, onRemove = { component.onInterestsChanged(state.interests - interest) })
                    }
                    AddChip(onClick = { showAddInterest = true })
                }
            }
            if (state.error != null) {
                Text(state.error!!, style = AuraTheme.text.caption13, color = colors.statePassStrong)
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.padding(24.dp)) {
            PrimaryButton("Save changes", onClick = component::onSave, enabled = state.canSave && !state.isSaving, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showAddInterest) {
        var draft by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddInterest = false },
            confirmButton = {
                TextButton(onClick = {
                    val t = draft.trim()
                    if (t.isNotEmpty() && t !in state.interests) {
                        component.onInterestsChanged(state.interests + t)
                    }
                    showAddInterest = false
                }) { Text("Add", color = colors.accentTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showAddInterest = false }) { Text("Cancel", color = colors.textSecondary) }
            },
            title = { Text("Add interest", style = AuraTheme.text.heading22, color = colors.textPrimary) },
            text = { AuraTextField(draft, { draft = it }, placeholder = "e.g. hiking") },
            containerColor = colors.bgSurface,
        )
    }
}

@Composable
private fun PhotoGrid(
    photos: List<UploadedPhoto>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    // Fixed 3x2 grid of 6 slots (spec §6.11): filled photos then dashed add tiles.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0 until 3) {
                    val i = row * 3 + col
                    if (i < photos.size) {
                        PhotoTile(photos[i], { onRemove(i) }, Modifier.weight(1f))
                    } else {
                        AddTile(onAdd, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(photo: UploadedPhoto, onRemove: () -> Unit, modifier: Modifier) {
    Box(modifier.height(116.dp).clip(RoundedCornerShape(14.dp)).background(auraDiagonalBrush(deepStart = true))) {
        if (photo.filePath.isNotEmpty()) {
            AsyncImage(
                model = "file://${photo.filePath}",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        RemoveBadge(onRemove, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

@Composable
private fun AddTile(onClick: () -> Unit, modifier: Modifier) {
    val colors = AuraTheme.colors
    Box(
        modifier
            .height(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                val r = 14.dp.toPx()
                drawRoundRect(
                    color = colors.borderStrong2,
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", style = AuraTheme.text.heading22, color = colors.accentVioletBright)
    }
}

@Composable
private fun RemoveBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    Box(
        modifier.size(24.dp).clip(CircleShape).background(colors.bgBase.copy(alpha = 0.7f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(10.dp)) {
            val s = size.minDimension
            val w = 1.6.dp.toPx()
            drawLine(colors.textOnImage, Offset(0f, 0f), Offset(s, s), w, StrokeCap.Round)
            drawLine(colors.textOnImage, Offset(s, 0f), Offset(0f, s), w, StrokeCap.Round)
        }
    }
}
