package com.aura.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme
import com.aura.ui.theme.aura.auraDiagonalBrush

/**
 * Swipe card (AURA_DESIGN_SPEC §5.11): photo (with bottom scrim), name/age/meta/chips
 * bottom-left, info "i" bottom-right. [photoPaths] are the profile's loaded photos; tapping
 * the left/right half of the card cycles through them, and the story-segment bars at the top
 * (shown only for 2+ photos) highlight the current one. Falls back to the aurora-gradient
 * placeholder when there are no photos.
 */
@Composable
fun ProfileCard(
    name: String,
    age: Int,
    meta: String,
    chips: List<String>,
    modifier: Modifier = Modifier,
    photoPaths: List<String> = emptyList(),
    onInfoClick: (() -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.lg)
    var photoIndex by remember { mutableStateOf(0) }
    val index = photoIndex.coerceIn(0, (photoPaths.size - 1).coerceAtLeast(0))
    val hasMultiple = photoPaths.size > 1

    Box(
        modifier
            .clip(shape)
            .background(auraDiagonalBrush(deepStart = true)),
    ) {
        // Current photo (or the gradient placeholder when there are none).
        photoPaths.getOrNull(index)?.let { path ->
            AsyncImage(
                model = "file://$path",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Bottom scrim for text legibility.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bgBase.copy(alpha = 0.92f)))),
        )

        // Photo nav: tap the left/right half (upper area) to go to the previous/next photo.
        // Confined to the top 78% so it never overlaps the name/chips or the info button.
        if (hasMultiple) {
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.78f)) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (photoIndex > 0) photoIndex-- },
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (photoIndex < photoPaths.lastIndex) photoIndex++ },
                )
            }
        }

        // Story segments: one bar per photo, highlighting the current one.
        if (hasMultiple) {
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(photoPaths.size) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(AuraRadius.sm))
                            .background(colors.textOnImage.copy(alpha = if (i == index) 1f else 0.35f)),
                    )
                }
            }
        }

        // Info "i".
        if (onInfoClick != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(30.dp)
                    .border(1.5.dp, colors.textOnImage.copy(alpha = 0.7f), RoundedCornerShape(AuraRadius.full))
                    .clickable(onClick = onInfoClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("i", style = AuraTheme.text.body16, color = colors.textOnImage)
            }
        }

        // Name / age / meta / chips.
        Column(
            Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(name, style = AuraTheme.text.swipeName, color = colors.textOnImage)
                Text(" $age", style = AuraTheme.text.swipeAge, color = colors.textOnImage)
            }
            Text(
                meta,
                style = AuraTheme.text.body16.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
                color = colors.textOnImageDim,
            )
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.take(3).forEach { AuraChip(it, overPhoto = true) }
                }
            }
        }
    }
}
