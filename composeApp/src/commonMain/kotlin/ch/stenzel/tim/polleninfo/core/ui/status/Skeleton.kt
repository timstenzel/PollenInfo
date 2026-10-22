package ch.stenzel.tim.polleninfo.core.ui.status

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp

/**
 * A screen's layout drawn as grey blocks while it loads, with one light band sweeping across the
 * whole of it. [content] places [SkeletonBlock]s and [SkeletonCanvas]es; each draws the part of the
 * one band that crosses it, so they shimmer together rather than each on its own. None of it is
 * announced.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SWEEP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer progress",
    )
    val sweep = remember(progress) { Sweep(progress) }
    Box(modifier.clearAndSetSemantics {}.onGloballyPositioned { sweep.box = it }) {
        CompositionLocalProvider(LocalSweep provides sweep) { content() }
    }
}

/** The band's position and the box it crosses; read only while drawing. */
private class Sweep(val progress: State<Float>) {
    var box: LayoutCoordinates? = null
}

private val LocalSweep = staticCompositionLocalOf<Sweep?> { null }

/** One grey block of a skeleton, in the sunken colour; rounded ends unless given a [shape]. */
@Composable
fun SkeletonBlock(width: Dp, height: Dp, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(height / 2)) {
    val sunken = MaterialTheme.colorScheme.surfaceContainerHighest
    SkeletonCanvas(modifier.size(width, height)) {
        drawOutline(shape.createOutline(size, layoutDirection, this), sunken)
    }
}

/**
 * Skeleton drawing that is not a plain block (the gauge's arcs), shimmering with the blocks.
 *
 * Each canvas is its own offscreen layer, so the band's blend mode keeps it to what the canvas drew.
 * One layer over the whole skeleton would cut off the cards' shadows at its edges.
 */
@Composable
fun SkeletonCanvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) {
    val sweep = LocalSweep.current
    // The colour of the cards the blocks sit on: the band brightens a block towards it.
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = HIGHLIGHT_ALPHA)
    val own = remember { Placement() }
    Canvas(
        modifier
            .onGloballyPositioned { own.coordinates = it }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val box = sweep?.box ?: return@drawWithContent
                val self = own.coordinates ?: return@drawWithContent
                if (box.isAttached && self.isAttached) {
                    drawBand(sweep.progress.value, box.size.width.toFloat(), box.localPositionOf(self, Offset.Zero), highlight)
                }
            },
        onDraw = onDraw,
    )
}

/** Where a canvas sits; read only while drawing. */
private class Placement {
    var coordinates: LayoutCoordinates? = null
}

/**
 * The part of the band, at [progress] across a box [boxWidth] wide, that falls on a canvas placed at
 * [origin] in the box: from fully off the start edge to fully off the end edge, slanted like light.
 */
private fun DrawScope.drawBand(progress: Float, boxWidth: Float, origin: Offset, highlight: Color) {
    val band = boxWidth * BAND_RATIO
    val x = -band + (boxWidth + 2 * band) * progress - origin.x
    drawRect(
        brush = Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to highlight,
            1f to Color.Transparent,
            start = Offset(x - band / 2, -origin.y),
            end = Offset(x + band / 2, band * SLANT - origin.y),
        ),
        // Only where the canvas drew something.
        blendMode = BlendMode.SrcAtop,
    )
}

private const val SWEEP_MILLIS = 1400
private const val BAND_RATIO = 0.6f
private const val SLANT = 0.4f
private const val HIGHLIGHT_ALPHA = 0.7f
