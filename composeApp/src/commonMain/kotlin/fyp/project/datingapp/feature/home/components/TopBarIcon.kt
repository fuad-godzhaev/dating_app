package fyp.project.datingapp.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import fyp.project.datingapp.ui.components.withLinearGradient
import fyp.project.datingapp.ui.theme.Orange
import fyp.project.datingapp.ui.theme.Pink
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Home-screen top-bar icon, tinted with the Pink→Orange gradient used
 * across the app. Three overloads accept the three common Compose icon
 * sources. `onClick = null` → non-interactive (used for the centre logo).
 */
@Composable
fun TopBarIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .withLinearGradient(Pink, Orange)
            .run { if (onClick != null) clickable(onClick = onClick) else this },
    )
}

@Composable
fun TopBarIcon(resource: DrawableResource, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) =
    TopBarIcon(painterResource(resource), modifier, onClick)

@Composable
fun TopBarIcon(imageVector: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) =
    TopBarIcon(rememberVectorPainter(imageVector), modifier, onClick)
