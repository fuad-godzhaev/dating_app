package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyp.project.datingapp.ui.theme.aura.AuraRadius
import fyp.project.datingapp.ui.theme.aura.AuraTheme
import fyp.project.datingapp.ui.theme.aura.auraDiagonalBrush

/**
 * Swipe card (AURA_DESIGN_SPEC §5.11): diagonal aurora-gradient placeholder, bottom scrim
 * for legibility, story segments on top, name/age/meta/chips bottom-left, info "i"
 * bottom-right. [photo] can later overlay a real image while keeping the scrim treatment.
 */
@Composable
fun ProfileCard(
    name: String,
    age: Int,
    meta: String,
    chips: List<String>,
    modifier: Modifier = Modifier,
    segments: Int = 3,
    onInfoClick: (() -> Unit)? = null,
    photo: (@Composable BoxScope.() -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.lg)
    Box(
        modifier
            .clip(shape)
            .background(auraDiagonalBrush(deepStart = true)),
    ) {
        if (photo != null) photo()

        // Bottom scrim for text legibility.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bgBase.copy(alpha = 0.92f)))),
        )

        // Story segments.
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(segments) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(AuraRadius.sm))
                        .background(colors.textOnImage.copy(alpha = if (i == 0) 1f else 0.35f)),
                )
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
