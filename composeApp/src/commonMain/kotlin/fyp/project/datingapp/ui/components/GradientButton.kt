package fyp.project.datingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fyp.project.datingapp.ui.theme.Orange
import fyp.project.datingapp.ui.theme.Pink

/**
 * Material3 [Button] painted with a horizontal Pink→Orange gradient.
 * Mirrors the prototype's `com.apiguave.core_ui.components.GradientButton`
 * signature: `(modifier, enabled, onClick, content: RowScope.() -> Unit)`.
 *
 * The button itself renders transparent; the gradient is drawn on an
 * inner [Row] so the ripple + elevation stay on the Material3 container.
 */
@Composable
fun GradientButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Row(
            modifier = modifier
                .alpha(if (enabled) 1f else .12f)
                .background(Brush.horizontalGradient(listOf(Pink, Orange)))
                .padding(ButtonDefaults.ContentPadding)
                .width(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
