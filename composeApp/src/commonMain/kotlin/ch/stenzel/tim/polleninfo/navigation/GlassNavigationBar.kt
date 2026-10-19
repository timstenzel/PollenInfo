package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.theme.meadowColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import org.jetbrains.compose.resources.stringResource

/**
 * The tab bar: icon-only [TopLevelDestination]s on a glass panel with rounded upper corners, the
 * current tab on a green pill.
 *
 * The tab content behind it ([hazeState]'s source) shows through blurred and tinted with the Meadow
 * glass colour. Where blur is unavailable (Android before 12) Haze draws the tint alone, with its
 * alpha boosted for the blur radius, so the icons stay readable over any content.
 *
 * Each tab is a `Role.Tab` selectable announced by its `contentDescription`, as with Material's
 * `NavigationBarItem` before.
 */
@Composable
fun GlassNavigationBar(
    currentTab: TopLevelDestination,
    onTabSelected: (TopLevelDestination) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val glass = MaterialTheme.meadowColors.glass
    val hairline = MaterialTheme.colorScheme.outline.copy(alpha = HAIRLINE_ALPHA)
    val blurStyle = remember(glass) {
        HazeBlurStyle {
            blurRadius(BLUR_RADIUS)
            colorEffects(listOf(HazeColorEffect.tint(glass)))
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(BarShape)
            .hazeBlur(HazeInput.Sources(hazeState), blurStyle)
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
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevelDestination.entries.forEach { tab ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TabItem(
                    tab = tab,
                    selected = tab == currentTab,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun TabItem(tab: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val pill by animateColorAsState(if (selected) colors.primary else Color.Transparent)
    val tint by animateColorAsState(if (selected) colors.onPrimary else colors.onSurfaceVariant)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(width = 60.dp, height = 44.dp)
            .clip(CircleShape)
            .background(pill)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Icon(tab.icon, contentDescription = stringResource(tab.contentDescription), tint = tint)
    }
}

private val BAR_CORNER = 36.dp
private val BarShape = RoundedCornerShape(topStart = BAR_CORNER, topEnd = BAR_CORNER)
private val BLUR_RADIUS = 24.dp

/** Figma's top hairline: `outline` at 70 %. */
private const val HAIRLINE_ALPHA = 0.7f
