package fyp.project.datingapp.ui.theme.aura

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Material3 [Shapes] mapped to Aura radii (§3.5). Components mostly use [AuraRadius]
 * directly; this keeps stock M3 components on-brand.
 */
val AuraShapes = Shapes(
    extraSmall = RoundedCornerShape(AuraRadius.sm),
    small = RoundedCornerShape(AuraRadius.sm),
    medium = RoundedCornerShape(AuraRadius.md),
    large = RoundedCornerShape(AuraRadius.lg),
    extraLarge = RoundedCornerShape(AuraRadius.pill),
)
