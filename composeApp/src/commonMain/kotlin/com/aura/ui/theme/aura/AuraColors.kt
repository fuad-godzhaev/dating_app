package com.aura.ui.theme.aura

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Aura semantic colour tokens (AURA_DESIGN_SPEC §3.2). Screens read THESE
 * (via [AuraTheme.colors]), never the raw [AuraPalette]. The whole set is a single
 * immutable value so a second instance = a second colour mode (e.g. a future Light
 * mode) with zero call-site changes - this is the token-driven goal of spec §8.
 */
@Immutable
data class AuraColors(
    // Backgrounds
    val bgBase: Color,
    val bgSurface: Color,
    val bgSurfaceRaised: Color,
    // Borders
    val borderHairline: Color,
    val borderStrong: Color,
    val borderStrong2: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnAccent: Color,
    val textOnImage: Color,
    val textOnImageDim: Color,
    // Accents
    val accentViolet: Color,
    val accentVioletBright: Color,
    val accentTeal: Color,
    val accentTealBright: Color,
    val accentOnTeal: Color,
    // State
    val statePass: Color,
    val statePassStrong: Color,
    val stateLike: Color,
    val stateVerified: Color,
    // Gradient endpoints (spec §3.3) - exposed so brushes can read the theme
    val gradientStart: Color,
    val gradientEnd: Color,
    val gradientHeroStart: Color,
)

/** Dark mode (the only mode shipped now; spec is dark-first). */
val AuraDarkColors = AuraColors(
    bgBase = AuraPalette.Ink,
    bgSurface = AuraPalette.Surface,
    bgSurfaceRaised = AuraPalette.Surface2,
    borderHairline = AuraPalette.Hairline,
    borderStrong = AuraPalette.Border,
    borderStrong2 = AuraPalette.Border2,
    textPrimary = AuraPalette.Ice,
    textSecondary = AuraPalette.Slate,
    textTertiary = AuraPalette.SlateDim,
    textOnAccent = AuraPalette.Ink,
    textOnImage = AuraPalette.White,
    textOnImageDim = AuraPalette.IceDim,
    accentViolet = AuraPalette.Violet,
    accentVioletBright = AuraPalette.VioletBright,
    accentTeal = AuraPalette.Teal,
    accentTealBright = AuraPalette.TealBright,
    accentOnTeal = AuraPalette.DeepTeal,
    statePass = AuraPalette.Coral,
    statePassStrong = AuraPalette.CoralSoft,
    stateLike = AuraPalette.Teal,
    stateVerified = AuraPalette.Teal,
    gradientStart = AuraPalette.Violet,
    gradientEnd = AuraPalette.Teal,
    gradientHeroStart = AuraPalette.VioletDeep,
)
