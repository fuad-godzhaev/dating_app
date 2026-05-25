package com.aura.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.aura.AuraTheme

/**
 * Row of PIN dots (AURA_DESIGN_SPEC §5.14). [filled] dots are solid teal; the rest are an
 * outlined ring. The actual digits come from the platform keyboard via a BasicTextField;
 * this only reflects entry length.
 */
@Composable
fun PinDots(
    filled: Int,
    total: Int = 4,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val isFilled = i < filled
            Box(
                Modifier
                    .size(15.dp)
                    .then(
                        if (isFilled) {
                            Modifier.background(colors.accentTeal, CircleShape)
                        } else {
                            Modifier.background(Color.Transparent, CircleShape)
                                .border(1.5.dp, colors.borderStrong2, CircleShape)
                        },
                    ),
            )
        }
    }
}

/**
 * PIN entry: [PinDots] over a hidden BasicTextField driven by the platform numeric keyboard.
 * The whole row is a large, full-width tap target that re-focuses the field and re-opens the
 * keyboard, so an accidentally-dismissed keyboard is easy to bring back (the field itself is
 * 1dp, which is why a generous hitbox is needed). Auto-focuses + shows the keyboard on first
 * display. [value] is the current digits; [onDigit]/[onBackspace] are diffed from it.
 */
@Composable
fun PinEntry(
    value: String,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    enabled: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                runCatching { focusRequester.requestFocus() }
                keyboard?.show()
            }
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        PinDots(filled = value.length.coerceIn(0, length), total = length)
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() }.take(length)
                when {
                    filtered.length > value.length ->
                        for (i in value.length until filtered.length) onDigit(filtered[i])
                    filtered.length < value.length ->
                        repeat(value.length - filtered.length) { onBackspace() }
                }
            },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = AuraTheme.text.body16.copy(color = Color.Transparent),
            modifier = Modifier.focusRequester(focusRequester).size(1.dp).alpha(0f),
        )
    }
}
