package ch.stenzel.tim.polleninfo.core.ui.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.theme.meadowColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * The Meadow glass panel at the bottom of a screen — the tab bar and the non-tab action bar: rounded
 * upper corners, the content behind ([hazeState]'s source) blurred and tinted with the glass colour,
 * a hairline along the top edge. It pads [content] for the navigation bar.
 *
 * Where blur is unavailable (Android before 12) Haze draws the tint alone, with its alpha boosted
 * for the blur radius, so whatever sits on the bar stays readable over any content.
 */
@Composable
fun GlassBar(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val hairline = MaterialTheme.colorScheme.outline.copy(alpha = HAIRLINE_ALPHA)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glass(hazeState, BarShape)
            .drawWithContent {
                drawContent()
                // The hairline follows the top edge and its rounded corners only; down the sides and
                // along the screen's bottom edge it would read as a box.
                val corner = BAR_CORNER.toPx()
                clipRect(bottom = corner) {
                    drawOutline(
                        outline = BarShape.createOutline(size, layoutDirection, this),
                        color = hairline,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        content()
    }
}

/**
 * Clips to [shape] and fills it with the blurred, glass-tinted content of [hazeState]'s source —
 * the material of the bars and of the floating back button.
 */
@Composable
internal fun Modifier.glass(hazeState: HazeState, shape: Shape): Modifier {
    val tint = MaterialTheme.meadowColors.glass
    val style = remember(tint) {
        HazeBlurStyle {
            blurRadius(BLUR_RADIUS)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
        }
    }
    return clip(shape).hazeBlur(HazeInput.Sources(hazeState), style)
}

/** The glass circle's edge: Figma draws it in `outline` all round. */
@Composable
internal fun Modifier.glassEdge(shape: Shape): Modifier =
    border(1.dp, MaterialTheme.colorScheme.outline, shape)

private val BAR_CORNER = 36.dp
private val BarShape = RoundedCornerShape(topStart = BAR_CORNER, topEnd = BAR_CORNER)
private val BLUR_RADIUS = 24.dp

/** Figma's top hairline: `outline` at 70 %. */
private const val HAIRLINE_ALPHA = 0.7f
