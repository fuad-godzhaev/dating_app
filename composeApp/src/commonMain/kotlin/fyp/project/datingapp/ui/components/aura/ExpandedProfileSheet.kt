package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyp.project.datingapp.ui.theme.aura.AuraTheme
import fyp.project.datingapp.ui.theme.aura.auraDiagonalBrush

/**
 * Expanded-profile sheet body (AURA_DESIGN_SPEC §6.7). Rendered inside a Material3
 * ModalBottomSheet hosted by the calling screen (Home / Orbit). Photo header + scrim with
 * name/verified/meta, then About/Interests/Looking-for sections, Pass/Like actions, and a
 * Report link.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedProfileSheet(
    name: String,
    age: Int,
    meta: String,
    bio: String,
    interests: List<String>,
    lookingFor: List<String>,
    onPass: () -> Unit,
    onLike: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
    verified: Boolean = true,
) {
    val colors = AuraTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.bgBase)
            .verticalScroll(rememberScrollState()),
    ) {
        // Photo header.
        Box(
            Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(auraDiagonalBrush(deepStart = true)),
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bgBase.copy(alpha = 0.95f)))),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$name, $age", style = AuraTheme.text.sheetName, color = colors.textOnImage)
                    if (verified) VerifiedBadge(size = 22.dp)
                }
                Text(
                    meta,
                    style = AuraTheme.text.body16.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
                    color = colors.textOnImageDim,
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Section("About") { Text(bio, style = AuraTheme.text.body16, color = colors.textSecondary) }
            if (interests.isNotEmpty()) {
                Section("Interests & hobbies") { ChipFlow(interests) }
            }
            if (lookingFor.isNotEmpty()) {
                Section("Looking for") { ChipFlow(lookingFor) }
            }

            ActionButtonsRow(onPass = onPass, onLike = onLike, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

            Text(
                "Report $name",
                style = AuraTheme.text.label14,
                color = colors.statePassStrong,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onReport).padding(8.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = AuraTheme.text.sectionHeader, color = AuraTheme.colors.textTertiary)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(items: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { AuraChip(it) }
    }
}
