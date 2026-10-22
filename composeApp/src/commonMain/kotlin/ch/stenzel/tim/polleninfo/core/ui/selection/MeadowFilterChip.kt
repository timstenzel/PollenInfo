package ch.stenzel.tim.polleninfo.core.ui.selection

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * One choice of several as a pill (Figma "Chip"): on the raised colour with a hairline edge, or
 * filled `primary` when [selected]; no check mark, the fill says it. Material's filter chip
 * underneath keeps its selected state for screen readers. [label] may carry its own spoken text
 * (the chip merges it into its one node).
 */
@Composable
fun MeadowFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.heightIn(min = HEIGHT),
        enabled = enabled,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceContainerLowest,
            labelColor = colors.onSurface,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceContainerLowest,
            disabledSelectedContainerColor = colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = colors.outlineVariant,
            selectedBorderColor = Color.Transparent,
            disabledBorderColor = colors.outlineVariant,
            disabledSelectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
        ),
    )
}

/** The chip's text in the label style, with Figma's inner spacing. */
@Composable
fun ChipLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = modifier.padding(horizontal = 6.dp))
}

private val HEIGHT = 38.dp
private const val DISABLED_CONTAINER_ALPHA = 0.12f
