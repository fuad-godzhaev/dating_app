package fyp.project.datingapp.ui.theme.aura

import androidx.compose.ui.graphics.Color

/**
 * Aura primitive colour palette (AURA_DESIGN_SPEC §3.1). RAW values only - never use
 * these directly in screens; bind to the semantic [AuraColors] tokens instead. Kept
 * internal so screen code is forced through the semantic layer.
 */
internal object AuraPalette {
    val Ink = Color(0xFF0B0E20)
    val Surface = Color(0xFF161B3D)
    val Surface2 = Color(0xFF1E2450)
    val Hairline = Color(0xFF2A2F55)
    val Border = Color(0xFF3A3F66)
    val Border2 = Color(0xFF4A4F78)
    val Violet = Color(0xFF7B61FF)
    val VioletBright = Color(0xFFA99CFF)
    val Teal = Color(0xFF2BD4B0)
    val TealBright = Color(0xFF5DE9CB)
    val Coral = Color(0xFFFF6B8A)
    val CoralSoft = Color(0xFFFF8FA6)
    val White = Color(0xFFFFFFFF)
    val Ice = Color(0xFFF5F6FF)
    val IceDim = Color(0xFFE4E6F5)
    val Slate = Color(0xFFA6ABD0)
    // slate-dim lightened from #6B7099 -> #8A90B6 for WCAG 2.1 AA contrast on all
    // surfaces (AURA_DESIGN_CHANGELOG D1): tertiary/muted text was the only failing pair.
    val SlateDim = Color(0xFF8A90B6)
    val DeepTeal = Color(0xFF06231E)

    /** Slightly deeper violet some hero/card gradients start from (spec §3.3). */
    val VioletDeep = Color(0xFF6E59E6)
}
