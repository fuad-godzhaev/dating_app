package com.aura.ui.theme.aura

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Aura type scale (AURA_DESIGN_SPEC §3.4) plus the named one-off styles used by specific
 * components/screens (§5/§6). Screens read these via [AuraTheme.text] so the underlying
 * font family can change in one place.
 *
 * FONT: the spec calls for Inter (Regular/Medium/SemiBold/Bold). The .ttf files are an
 * external OFL asset not yet in the repo; until they are added under
 * `composeResources/font/`, [AuraTheme] builds these styles on [FontFamily.Default].
 * Swapping in Inter = build a `FontFamily(Font(Res.font.inter_*, ...))` inside
 * [AuraTheme] and pass it to [auraTextStyles] - no screen changes.
 */
@Immutable
class AuraTextStyles(
    val display40: TextStyle,
    val title28: TextStyle,
    val heading22: TextStyle,
    val body16: TextStyle,
    val label14: TextStyle,
    val caption13: TextStyle,
    val button17: TextStyle,
    // One-offs referenced by name in the spec
    val wordmark: TextStyle,        // "aura" wordmark, Display/40 @ ~56sp, letter-spacing 0.5
    val name24: TextStyle,          // self profile name "Eva, 26"
    val swipeName: TextStyle,       // swipe-card name, Display/30 Bold
    val swipeAge: TextStyle,        // swipe-card age, Body/26 Regular
    val sheetName: TextStyle,       // expanded-sheet name, Display/30
    val title26: TextStyle,         // "Report received" etc.
    val sectionHeader: TextStyle,   // card section captions (SECURITY), Caption SemiBold
)

fun auraTextStyles(family: FontFamily): AuraTextStyles = AuraTextStyles(
    display40 = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
    title28 = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    heading22 = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 29.sp),
    body16 = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    label14 = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    caption13 = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    button17 = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 17.sp),
    wordmark = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = 0.5.sp),
    name24 = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 29.sp),
    swipeName = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp),
    swipeAge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp),
    sheetName = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp),
    title26 = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp),
    sectionHeader = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

/** Map the Aura scale onto Material3 slots so stock M3 components stay on-brand. */
fun auraMaterialTypography(t: AuraTextStyles): Typography = Typography(
    displayLarge = t.display40,
    titleLarge = t.title28,
    headlineSmall = t.heading22,
    bodyLarge = t.body16,
    bodyMedium = t.body16,
    labelLarge = t.label14,
    bodySmall = t.caption13,
)

/** Fallback styles until [AuraTheme] provides the real (Inter) family. */
val LocalAuraTypography = staticCompositionLocalOf { auraTextStyles(FontFamily.Default) }
