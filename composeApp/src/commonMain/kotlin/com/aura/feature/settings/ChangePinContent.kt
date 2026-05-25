package com.aura.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.aura.ui.components.aura.AuraTopBar
import com.aura.ui.components.aura.PinDots
import com.aura.ui.theme.aura.AuraTheme

private const val PIN_LENGTH = 4

@Composable
fun ChangePinContent(component: ChangePinComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    var firstPin by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Change PIN", onBack = component::onBack)
        Column(
            Modifier.weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                if (firstPin == null) "Enter a new 4-digit PIN" else "Confirm your new PIN",
                style = AuraTheme.text.heading22,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                PinDots(filled = entered.length)
                BasicTextField(
                    value = entered,
                    onValueChange = { raw ->
                        if (state.busy) return@BasicTextField
                        val filtered = raw.filter { it.isDigit() }.take(PIN_LENGTH)
                        entered = filtered
                        mismatch = false
                        if (filtered.length == PIN_LENGTH) {
                            val fp = firstPin
                            if (fp == null) {
                                firstPin = filtered
                                entered = ""
                            } else if (filtered == fp) {
                                component.submitNewPin(filtered)
                            } else {
                                mismatch = true
                                firstPin = null
                                entered = ""
                            }
                        }
                    },
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    cursorBrush = SolidColor(Color.Transparent),
                    textStyle = AuraTheme.text.body16.copy(color = Color.Transparent),
                    modifier = Modifier.focusRequester(focusRequester).size(1.dp).alpha(0f),
                )
            }
            val error = when {
                mismatch -> "PINs don't match. Try again."
                state.error != null -> state.error
                else -> null
            }
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(error, style = AuraTheme.text.caption13, color = colors.statePassStrong, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "This PIN unlocks your identity on this device.",
                style = AuraTheme.text.caption13,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
