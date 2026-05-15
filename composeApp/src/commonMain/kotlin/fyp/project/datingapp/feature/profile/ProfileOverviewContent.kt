package fyp.project.datingapp.feature.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import fyp.project.datingapp.ui.components.aura.AuraCard
import fyp.project.datingapp.ui.components.aura.AuraChip
import fyp.project.datingapp.ui.components.aura.AuraTopBar
import fyp.project.datingapp.ui.components.aura.Avatar
import fyp.project.datingapp.ui.components.aura.HairlineDivider
import fyp.project.datingapp.ui.components.aura.ListRow
import fyp.project.datingapp.ui.components.aura.PrimaryButton
import fyp.project.datingapp.ui.components.aura.VerifiedBadge
import fyp.project.datingapp.ui.theme.aura.AuraTheme
import fyp.project.datingapp.ui.theme.aura.auraDiagonalBrush

@Composable
fun ProfileOverviewContent(component: ProfileOverviewComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors
    val profile = state.profile

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(
            onBack = component::onBack,
            trailing = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp).clickable(onClick = component::onSettings),
                )
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Avatar(size = 104.dp, ring = true, glow = true, initial = profile?.displayName?.take(1)?.uppercase())
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (profile != null) "${profile.displayName}, ${profile.age}" else "Your profile",
                    style = AuraTheme.text.name24,
                    color = colors.textPrimary,
                )
                VerifiedBadge(size = 20.dp)
            }
            state.did?.let { did ->
                Spacer(Modifier.height(4.dp))
                Text(did.take(24) + "...", style = AuraTheme.text.caption13, color = colors.textTertiary)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Edit profile", onClick = component::onEditProfile, modifier = Modifier.width(220.dp), height = 46.dp)

            Spacer(Modifier.height(24.dp))
            // Photo strip (UI-only placeholders for now).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(14.dp)).background(auraDiagonalBrush(deepStart = true)))
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("About")
            Spacer(Modifier.height(8.dp))
            Text(profile?.bio?.takeIf { it.isNotBlank() } ?: "No bio yet.", style = AuraTheme.text.body16, color = colors.textSecondary, modifier = Modifier.fillMaxWidth())

            if (!profile?.interests.isNullOrEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Interests")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile!!.interests.take(4).forEach { AuraChip(it) }
                }
            }

            Spacer(Modifier.height(24.dp))
            AuraCard {
                ListRow(
                    title = "Recovery phrase",
                    subtitle = "Backed up · keep it offline",
                    trailing = { VerifiedBadge(size = 18.dp) },
                    onClick = component::onRecoveryPhrase,
                )
                HairlineDivider()
                ListRow(
                    title = "Sign out",
                    titleColor = colors.statePassStrong,
                    trailing = null,
                    onClick = component::onSignOut,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = AuraTheme.text.sectionHeader, color = AuraTheme.colors.textTertiary, modifier = Modifier.fillMaxWidth())
}
