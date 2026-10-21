package ch.stenzel.tim.polleninfo.core.ui.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * The Meadow checkbox's box (Figma "Checkbox"): a 24 dp rounded square, filled `primary` with a white
 * tick when [checked], a 2 dp `onSurfaceVariant` edge when not. Only the picture — the row around it
 * is the toggle (`toggleable` with `Role.Checkbox`), so box and sentence are one focus stop.
 */
@Composable
fun MeadowCheckbox(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val base = modifier.size(24.dp).alpha(if (enabled) 1f else DISABLED_ALPHA)
    Box(
        modifier = if (checked) {
            base.background(colors.primary, SHAPE)
        } else {
            base.border(2.dp, colors.onSurfaceVariant, SHAPE)
        },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

private val SHAPE = RoundedCornerShape(7.dp)
private const val DISABLED_ALPHA = 0.38f
