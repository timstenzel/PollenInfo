package ch.stenzel.tim.polleninfo.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * A Meadow card: the raised surface (`surfaceContainerLowest`) in [shape] with Figma's soft
 * "Elevation/Card" shadow — a forest-tinted blur rather than Material's grey elevation. Defaults to
 * the large-card shape (`extraLarge`, 36 dp).
 */
@Composable
fun MeadowCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .dropShadow(shape, CARD_SHADOW)
            .clip(shape)
            .background(color),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

/** Figma "Elevation/Card": `#17331F` at 8 %, 6 dp down, 20 dp blur. */
private val CARD_SHADOW = Shadow(
    radius = 20.dp,
    color = Color(0xFF17331F).copy(alpha = 0.08f),
    offset = DpOffset(0.dp, 6.dp),
)
