package fyp.project.datingapp.ui.theme.aura

import androidx.compose.ui.unit.dp

/** Corner radii (AURA_DESIGN_SPEC §3.5). */
object AuraRadius {
    val sm = 8.dp
    val md = 14.dp
    val lg = 20.dp
    val pill = 28.dp
    val full = 999.dp
}

/** Spacing scale (AURA_DESIGN_SPEC §3.5). */
object AuraSpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/** Global layout constants (AURA_DESIGN_SPEC §4). */
object AuraLayout {
    /** Horizontal screen margin; content width = 390 - 2*24 = 342dp on the design canvas. */
    val screenHMargin = 24.dp
}
