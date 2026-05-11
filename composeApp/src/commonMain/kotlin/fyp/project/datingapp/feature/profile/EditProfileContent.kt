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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import fyp.project.datingapp.ui.theme.aura.auraDiagonalBrush
import fyp.project.datingapp.ui.components.aura.AddChip
import fyp.project.datingapp.ui.components.aura.AuraChip
import fyp.project.datingapp.ui.components.aura.AuraStepper
import fyp.project.datingapp.ui.components.aura.AuraTextField
import fyp.project.datingapp.ui.components.aura.AuraTopBar
import fyp.project.datingapp.ui.components.aura.PrimaryButton
import fyp.project.datingapp.ui.theme.aura.AuraTheme

@Composable
fun EditProfileContent(component: EditProfileComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors

    val picker = rememberImagePicker { bytes, mime -> component.onPhotoPicked(bytes, mime) }

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Edit profile", onBack = component::onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Photos grid: existing/picked photos + an add tile that opens the OS picker.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.photos.take(2).forEach { photo ->
                    Box(
                        Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(14.dp)).background(auraDiagonalBrush(deepStart = true)),
                    ) {
                        if (photo.filePath.isNotEmpty()) {
                            AsyncImage(
                                model = "file://${photo.filePath}",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
                Box(
                    Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(14.dp)).background(colors.bgSurface).clickable { picker.launch() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", style = AuraTheme.text.heading22, color = colors.accentVioletBright)
                }
            }
            AuraTextField(state.displayName, component::onDisplayNameChanged, label = "Display name", placeholder = "Your name")
            AuraTextField(state.bio, component::onBioChanged, label = "Bio", placeholder = "A little about you", singleLine = false, minHeight = 96.dp)
            AuraStepper("Age", state.age, onDecrement = { component.onAgeChanged((state.age - 1).coerceAtLeast(18)) }, onIncrement = { component.onAgeChanged(state.age + 1) })
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Interests", style = AuraTheme.text.label14, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.interests.forEach { interest ->
                        AuraChip(interest, onRemove = { component.onInterestsChanged(state.interests - interest) })
                    }
                    AddChip(onClick = { component.onInterestsChanged(state.interests + "new") })
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
}
