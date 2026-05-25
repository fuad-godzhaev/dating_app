package com.aura.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.aura.ui.components.aura.AuraCard
import com.aura.ui.components.aura.AuraToggle
import com.aura.ui.components.aura.AuraTopBar
import com.aura.ui.components.aura.HairlineDivider
import com.aura.ui.components.aura.ListRow
import com.aura.ui.theme.aura.AuraTheme

@Composable
fun SettingsContent(component: SettingsComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors
    // UI-only toggles for now (binding to DiscoveryPreferencesStore is a follow-up).
    var bleOn by remember { mutableStateOf(true) }
    var lanOn by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Settings", onBack = component::onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Group("SECURITY") {
                AuraCard {
                    ListRow(title = "Change PIN", onClick = component::onChangePin)
                    HairlineDivider()
                    ListRow(title = "Recovery phrase", onClick = component::onRecoveryPhrase)
                }
            }
            Group("DISCOVERY") {
                AuraCard {
                    ListRow(
                        title = "Bluetooth (BLE) discovery",
                        subtitle = "Find people nearby, no internet needed",
                        trailing = { AuraToggle(bleOn, { bleOn = it }) },
                    )
                    HairlineDivider()
                    ListRow(
                        title = "Local network (LAN) discovery",
                        subtitle = "Discover peers on the same Wi-Fi",
                        trailing = { AuraToggle(lanOn, { lanOn = it }) },
                    )
                }
            }
            Group("BACKGROUND") {
                AuraCard {
                    ListRow(
                        title = "Stay online",
                        subtitle = "Receive messages + help the network in the background (uses battery)",
                        trailing = { AuraToggle(state.stayOnline, component::onStayOnlineChanged) },
                    )
                }
            }
            Group("PRIVACY & SAFETY") {
                AuraCard {
                    ListRow(title = "Privacy policy", onClick = component::onPrivacyPolicy)
                    HairlineDivider()
                    ListRow(title = "Blocked users", onClick = component::onBlockedUsers)
                    HairlineDivider()
                    ListRow(title = "Terms of use", onClick = component::onTermsOfUse)
                }
            }
            AuraCard {
                ListRow(title = "Sign out", titleColor = colors.statePassStrong, trailing = null, onClick = component::onSignOut)
            }
            Text(
                "aura v0.1 — research preview",
                style = AuraTheme.text.caption13,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = AuraTheme.text.sectionHeader, color = AuraTheme.colors.textTertiary)
        content()
    }
}
