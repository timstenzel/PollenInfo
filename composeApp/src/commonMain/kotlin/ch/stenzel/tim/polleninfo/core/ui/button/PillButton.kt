package ch.stenzel.tim.polleninfo.core.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.theme.meadowColors

/**
 * The Meadow button styles (Figma "Button"). One screen has at most one [Accent] — its single main
 * action in pollen gold; [Danger] marks a destructive action.
 */
enum class PillButtonStyle {
    /** Filled forest green: the main action. */
    Primary,

    /** Filled sage container: a secondary action that still needs weight. */
    Tonal,

    /** Outlined: a secondary action. */
    Outline,

    /** Text only: the least emphasis, e.g. Cancel. */
    Text,

    /** Outlined in red: a destructive action. */
    Danger,

    /** Filled pollen gold: a screen's single hero action. */
    Accent,
}

/**
 * A 52 dp pill-shaped button in one of the [PillButtonStyle]s. [icon], if given, leads the label and
 * is decorative. While [busy], a small spinner in the label colour leads instead — the caller decides
 * whether the button is still enabled.
 */
@Composable
fun PillButton(
    onClick: () -> Unit,
    style: PillButtonStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    busy: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = HEIGHT),
        enabled = enabled,
        shape = CircleShape,
        colors = style.colors(),
        border = style.border(enabled),
        contentPadding = PaddingValues(
            horizontal = if (style == PillButtonStyle.Text) 12.dp else 24.dp,
            vertical = 8.dp,
        ),
    ) {
        when {
            busy -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                Spacer(Modifier.width(8.dp))
            }
            icon != null -> {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
        }
        content()
    }
}

@Composable
private fun PillButtonStyle.colors(): ButtonColors {
    val colors = MaterialTheme.colorScheme
    val (container, label) = when (this) {
        PillButtonStyle.Primary -> colors.primary to colors.onPrimary
        PillButtonStyle.Tonal -> colors.primaryContainer to colors.onPrimaryContainer
        PillButtonStyle.Outline, PillButtonStyle.Text -> Color.Transparent to colors.primary
        PillButtonStyle.Danger -> Color.Transparent to colors.error
        PillButtonStyle.Accent -> MaterialTheme.meadowColors.accent to MaterialTheme.meadowColors.onAccent
    }
    val filled = container != Color.Transparent
    return ButtonDefaults.buttonColors(
        containerColor = container,
        contentColor = label,
        // Material's disabled look; an outlined or text button keeps no fill when disabled either.
        disabledContainerColor = if (filled) colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA) else Color.Transparent,
        disabledContentColor = colors.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
    )
}

@Composable
private fun PillButtonStyle.border(enabled: Boolean): BorderStroke? {
    val colors = MaterialTheme.colorScheme
    val color = when (this) {
        PillButtonStyle.Outline -> colors.outline
        PillButtonStyle.Danger -> colors.error
        else -> return null
    }
    return BorderStroke(1.5.dp, if (enabled) color else colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA))
}

private val HEIGHT = 52.dp
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
