package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity

/** How many arc segments the gauge has: one per severity above None. */
const val GAUGE_SEGMENTS = 4

/**
 * How many of the [GAUGE_SEGMENTS] the gauge fills for [severity]: none for no reading and for
 * None, then one per step from Low (1) to Very high (4).
 *
 * Unlike [severityFillFraction], None fills nothing: the gauge never stands alone — the severity
 * word sits in its middle — so an empty gauge reading "None" cannot be mistaken for "no reading".
 */
fun gaugeFilledSegments(severity: PollenSeverity?): Int = when (severity) {
    null, PollenSeverity.NONE -> 0
    PollenSeverity.LOW -> 1
    PollenSeverity.MODERATE -> 2
    PollenSeverity.HIGH -> 3
    PollenSeverity.VERY_HIGH -> 4
}

/**
 * Home's hero: a half circle of [GAUGE_SEGMENTS] arc segments, the first [gaugeFilledSegments] in
 * the severity's colour and the rest in the sunken track colour, a soft glow of that colour behind,
 * and the severity word large in the middle.
 *
 * The arcs and the glow are decorative — the word carries the meaning, so colour never stands alone.
 */
@Composable
fun SeverityGauge(severity: PollenSeverity, modifier: Modifier = Modifier) {
    val fill = severity.color()
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val filled = gaugeFilledSegments(severity)
    // The word may outgrow the arc (a long word at a large system font size); the box then grows
    // with it rather than clipping it.
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.size(GAUGE_WIDTH, GAUGE_HEIGHT)) {
            drawGlow(fill)
            drawGaugeSegments { index -> if (index < filled) fill else track }
        }
        Text(
            text = severity.label(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WORD_BOTTOM),
        )
    }
}

/** The gauge's frame: the half circle's diameter by its radius plus the arc's foot. */
val GAUGE_WIDTH: Dp = 260.dp
val GAUGE_HEIGHT: Dp = 140.dp

/** The severity word sits on the half circle's baseline, 10 dp above the frame's bottom. */
private val WORD_BOTTOM = 10.dp

/** Thickness of an arc segment: the inner radius is 84 % of the outer. */
private const val INNER_RADIUS_RATIO = 0.84f

/** The angle left empty between two segments, and half of it at either foot. */
private const val GAP_DEGREES = 4f

/**
 * Draws the [GAUGE_SEGMENTS] arcs of the gauge over the top half of a circle as wide as the canvas,
 * each in [segmentColor] of its index (0 = leftmost). Shared with the loading skeleton.
 */
fun DrawScope.drawGaugeSegments(segmentColor: (index: Int) -> Color) {
    val outer = size.width / 2
    val thickness = outer * (1 - INNER_RADIUS_RATIO)
    val radius = outer - thickness / 2
    val center = Offset(size.width / 2, outer)
    val step = 180f / GAUGE_SEGMENTS
    repeat(GAUGE_SEGMENTS) { index ->
        drawArc(
            color = segmentColor(index),
            // 180° is the left end; angles grow clockwise, over the top.
            startAngle = 180f + index * step + GAP_DEGREES / 2,
            sweepAngle = step - GAP_DEGREES,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = thickness),
        )
    }
}

/** Figma's blurred ellipse behind the word: the severity colour at 14 %, fading out. */
private fun DrawScope.drawGlow(color: Color) {
    val center = Offset(size.width / 2, 115.dp.toPx())
    val radiusX = 100.dp.toPx() + GLOW_BLUR.toPx()
    val radiusY = 65.dp.toPx() + GLOW_BLUR.toPx()
    // A radial gradient is round; scaling it vertically makes the ellipse without a blur effect,
    // which Android draws only from version 12.
    scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = GLOW_ALPHA),
                0.55f to color.copy(alpha = GLOW_ALPHA * 0.6f),
                1f to color.copy(alpha = 0f),
                center = center,
                radius = radiusX,
            ),
            radius = radiusX,
            center = center,
        )
    }
}

private const val GLOW_ALPHA = 0.14f
private val GLOW_BLUR = 25.dp
