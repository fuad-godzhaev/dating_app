package com.aura.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme

/**
 * Text input (AURA_DESIGN_SPEC §5.4). Surface fill + hairline border + optional floating
 * label above. Single-line or multiline via [singleLine]/[minHeight].
 */
@Composable
fun AuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 54.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.md)
    Column(modifier) {
        if (label != null) {
            Text(label, style = AuraTheme.text.label14, color = colors.textSecondary)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = if (label != null) 8.dp else 0.dp)
                .heightIn(min = minHeight)
                .clip(shape)
                .background(colors.bgSurface)
                .border(1.dp, colors.borderHairline, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = AuraTheme.text.body16.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accentTeal),
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, style = AuraTheme.text.body16, color = colors.textTertiary)
                    }
                    inner()
                },
            )
        }
    }
}
