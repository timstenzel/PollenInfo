package ch.stenzel.tim.polleninfo.feature.allstations.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A small outline of Switzerland for the All stations tab, drawn from [SWISS_BORDER] through
 * [SwissMapProjection] — the same shape as the map on the screen it opens.
 *
 * Stroke only and black, like a Material icon: `Icon` tints the whole vector with the content
 * colour, so it follows the bar's selected and unselected colours.
 */
val SwissOutline: ImageVector by lazy {
    ImageVector.Builder(
        name = "SwissOutline",
        defaultWidth = ICON_SIZE.dp,
        defaultHeight = ICON_SIZE.dp,
        viewportWidth = ICON_SIZE,
        viewportHeight = ICON_SIZE,
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        val inner = ICON_SIZE - 2 * PADDING
        SWISS_BORDER.forEachIndexed { index, point ->
            val p = SwissMapProjection.project(point, inner, inner)
            if (index == 0) moveTo(p.x + PADDING, p.y + PADDING) else lineTo(p.x + PADDING, p.y + PADDING)
        }
        close()
    }.build()
}

private const val ICON_SIZE = 24f

/** Material icons keep their artwork about 2 units inside the 24-unit viewport. */
private const val PADDING = 2f

private const val STROKE_WIDTH = 1.5f
