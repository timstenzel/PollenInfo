package ch.stenzel.tim.polleninfo.core.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.theme.meadowColors

/**
 * The frame of a tab screen, which has no top bar: the Meadow background with its decorative
 * washes, and a scrim that fades content out under the status bar.
 *
 * [content] fills the whole screen — behind the status bar and behind the glass tab bar — and is
 * handed the padding that keeps it clear of both: the safe top and sides, and
 * [LocalBottomBarPadding] at the bottom. A scrolling screen applies it inside its scroll, so the
 * content can pass under the bars and still reach its end; the first thing it shows is a
 * [ScreenHeader].
 */
@Composable
fun TabScreenFrame(content: @Composable (contentPadding: PaddingValues) -> Unit) {
    val safe = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding = PaddingValues(
        start = safe.calculateStartPadding(layoutDirection),
        top = safe.calculateTopPadding(),
        end = safe.calculateEndPadding(layoutDirection),
        bottom = LocalBottomBarPadding.current,
    )
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize().background(background)) {
        Washes(Modifier.fillMaxSize())
        content(contentPadding)
        TopScrim(opaqueHeight = safe.calculateTopPadding(), fade = SCRIM_FADE)
    }
}

/** How far below the status bar the top scrim fades out. */
private val SCRIM_FADE = 16.dp

/**
 * Opaque `background` for [opaqueHeight] — the status bar — then fading out over [fade], so
 * scrolled content never runs into the clock and icons. It has no semantics and takes no touches.
 */
@Composable
internal fun TopScrim(opaqueHeight: Dp, fade: Dp) {
    val background = MaterialTheme.colorScheme.background
    val height = opaqueHeight + fade
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to background,
                    (opaqueHeight / height) to background,
                    1f to background.copy(alpha = 0f),
                ),
            ),
    )
}

/**
 * The soft blobs of the Meadow background: a large sage wash at the top end and a small pollen-gold
 * one at the start. Pure decoration — a `Canvas` without semantics, never announced.
 */
@Composable
internal fun Washes(modifier: Modifier) {
    val wash = MaterialTheme.meadowColors.wash
    val accent = MaterialTheme.meadowColors.accent.copy(alpha = ACCENT_WASH_ALPHA)
    Canvas(modifier) {
        drawWash(wash, center = Offset(size.width - 33.dp.toPx(), 70.dp.toPx()), radius = 210.dp.toPx())
        drawWash(accent, center = Offset(-20.dp.toPx(), 260.dp.toPx()), radius = 90.dp.toPx())
    }
}

/** The gold wash is a hint of colour, not a second sage blob. */
private const val ACCENT_WASH_ALPHA = 0.3f

/** A circle that is solid to half its radius and then fades out — Figma's blurred blob. */
internal fun DrawScope.drawWash(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color,
            0.5f to color,
            1f to color.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * The top of a screen without a top bar: a small upper-cased [overline] and the large serif
 * [title], announced as a heading. [icon], if given, sits before the title and is decorative.
 *
 * A `null` [overline] keeps its line empty, so the title does not move when the overline arrives
 * later (All stations knows its station count only once loaded).
 */
@Composable
fun ScreenHeader(
    overline: String?,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(modifier.padding(top = 12.dp, bottom = 20.dp)) {
        val overlineStyle = MaterialTheme.typography.labelSmall
        if (overline != null) {
            Text(
                text = overline.uppercase(),
                style = overlineStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(with(LocalDensity.current) { overlineStyle.lineHeight.toDp() }))
        }
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { heading() },
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    // Decorative: the title beside it says what this is.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * A tab screen in a state without a scrolling list — loading, an error, a message: the [header] at
 * the top, [content] centred in the rest. Lays itself out inside [contentPadding] from
 * [TabScreenFrame].
 */
@Composable
fun TabCenteredContent(
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 24.dp)) {
        header()
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
