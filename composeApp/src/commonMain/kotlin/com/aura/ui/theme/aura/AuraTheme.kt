package com.aura.ui.theme.aura

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import aura.composeapp.generated.resources.Res
import aura.composeapp.generated.resources.inter_bold
import aura.composeapp.generated.resources.inter_medium
import aura.composeapp.generated.resources.inter_regular
import aura.composeapp.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/** The semantic Aura colours for the current mode. */
val LocalAuraColors = staticCompositionLocalOf { AuraDarkColors }

/**
 * Aura theme (AURA_DESIGN_SPEC §8). Dark-first and token-driven: brand colours come from
 * [LocalAuraColors] (read via [AuraTheme.colors]); the matching Material3 [darkColorScheme]
 * keeps stock M3 components on-brand. A future Light mode = a second [AuraColors] passed
 * in, with no call-site changes.
 */
@Composable
fun AuraTheme(
    colors: AuraColors = AuraDarkColors,
    content: @Composable () -> Unit,
) {
    val fontFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
    val textStyles = auraTextStyles(fontFamily)

    val scheme = darkColorScheme(
        background = colors.bgBase,
        surface = colors.bgSurface,
        surfaceVariant = colors.bgSurfaceRaised,
        onBackground = colors.textPrimary,
        onSurface = colors.textPrimary,
        onSurfaceVariant = colors.textSecondary,
        primary = colors.accentViolet,
        onPrimary = colors.textOnAccent,
        secondary = colors.accentTeal,
        onSecondary = colors.accentOnTeal,
        error = colors.statePass,
        onError = colors.textOnAccent,
        outline = colors.borderStrong,
        outlineVariant = colors.borderHairline,
    )

    CompositionLocalProvider(
        LocalAuraColors provides colors,
        LocalAuraTypography provides textStyles,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = auraMaterialTypography(textStyles),
            shapes = AuraShapes,
            content = content,
        )
    }
}

/** Convenience accessors mirroring `MaterialTheme.*`. */
object AuraTheme {
    val colors: AuraColors
        @Composable @ReadOnlyComposable get() = LocalAuraColors.current

    val text: AuraTextStyles
        @Composable @ReadOnlyComposable get() = LocalAuraTypography.current
}
