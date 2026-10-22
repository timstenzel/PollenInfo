package ch.stenzel.tim.polleninfo.core.ui.field

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A field whose value is chosen elsewhere — from a menu or a picker — rather than typed (Figma
 * "TextField"): [label] small above [value] on the raised colour, a hairline edge, [trailingIcon] at
 * the end. [isError] turns the edge and the label red.
 *
 * With [onClick] the field is a button of its own; without it, the caller makes it one (a dropdown's
 * `menuAnchor`). Either way its words are one focus stop.
 */
@Composable
fun SelectField(
    label: String,
    value: String,
    trailingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val border = BorderStroke(
        width = if (isError) 1.5.dp else 1.dp,
        color = if (isError) colors.error else colors.outlineVariant,
    )
    val body = @Composable {
        Row(
            modifier = Modifier
                .heightIn(min = HEIGHT)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isError) colors.error else colors.onSurfaceVariant,
                )
                Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            // Decorative: the field's role says what tapping it does.
            Icon(trailingIcon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceContainerLowest,
            border = border,
            content = body,
        )
    } else {
        Surface(
            modifier = modifier.semantics(mergeDescendants = true) {},
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceContainerLowest,
            border = border,
            content = body,
        )
    }
}

private val HEIGHT = 64.dp
private const val DISABLED_ALPHA = 0.38f
