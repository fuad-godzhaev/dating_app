package fyp.project.datingapp.ui.theme.aura

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush

/**
 * Aurora gradient helpers (AURA_DESIGN_SPEC §3.3). Buttons/pills use the horizontal
 * variant; photo placeholders / hero cards use the diagonal variant. Each helper has a
 * plain `(colors)` form for use inside `drawWithCache` / non-composable draw scopes and
 * a `@Composable` form that reads the current theme.
 */

/** Violet -> teal. Horizontal (left to right) for pills/buttons; vertical otherwise. */
fun auraBrush(colors: AuraColors, horizontal: Boolean = true): Brush =
    if (horizontal) {
        Brush.horizontalGradient(listOf(colors.gradientStart, colors.gradientEnd))
    } else {
        Brush.verticalGradient(listOf(colors.gradientStart, colors.gradientEnd))
    }

@Composable
@ReadOnlyComposable
fun auraBrush(horizontal: Boolean = true): Brush = auraBrush(AuraTheme.colors, horizontal)

/** Diagonal (top-left to bottom-right) violet -> teal for photo placeholders / hero cards. */
fun auraDiagonalBrush(colors: AuraColors, deepStart: Boolean = false): Brush =
    Brush.linearGradient(
        listOf(if (deepStart) colors.gradientHeroStart else colors.gradientStart, colors.gradientEnd),
    )

@Composable
@ReadOnlyComposable
fun auraDiagonalBrush(deepStart: Boolean = false): Brush =
    auraDiagonalBrush(AuraTheme.colors, deepStart)

/** Solid teal -> bright teal, used for the Like action button (spec §5.10). */
fun tealButtonBrush(colors: AuraColors): Brush =
    Brush.linearGradient(listOf(colors.accentTeal, colors.accentTealBright))

@Composable
@ReadOnlyComposable
fun tealButtonBrush(): Brush = tealButtonBrush(AuraTheme.colors)
