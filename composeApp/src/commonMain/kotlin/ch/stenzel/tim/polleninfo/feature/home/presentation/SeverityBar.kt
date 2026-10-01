package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity

/** The two sizes the screen uses: large for the overall severity, compact in each list row. */
enum class SeverityBarSize(val height: Dp) {
    Large(16.dp),
    Compact(8.dp),
}

/**
 * How much of the track [severity] fills: five fixed stops, `(ordinal + 1) / 5`, and nothing for
 * no reading.
 *
 * **None fills a fifth, not nothing,** because an empty bar already means "no reading" and the two
 * would otherwise look identical.
 *
 * **The fill does not track the concentration.** The bands are wildly unequal — for trees Moderate
 * spans 15–89 while High spans 90–1499 — so a birch reading of 200, comfortably High, would render
 * barely past the Moderate mark and contradict its own label.
 */
fun severityFillFraction(severity: PollenSeverity?): Float =
    if (severity == null) 0f else (severity.ordinal + 1) / PollenSeverity.entries.size.toFloat()

/**
 * A horizontal bar for one severity: the filled part is a **single** colour, the current
 * severity's. A per-segment gradient would leave the left end green during Very high — reassurance
 * inside a warning.
 *
 * `null` renders an empty, more muted track, the one way the bar says "no reading". The bar is
 * decorative to assistive technology; the severity word beside it carries the meaning.
 */
@Composable
fun SeverityBar(
    severity: PollenSeverity?,
    size: SeverityBarSize,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .height(size.height)
            .clip(shape)
            .background(if (severity == null) track.copy(alpha = NO_READING_TRACK_ALPHA) else track),
    ) {
        if (severity != null) {
            Box(
                Modifier
                    .fillMaxWidth(severityFillFraction(severity))
                    .fillMaxHeight()
                    .clip(shape)
                    .background(severity.color()),
            )
        }
    }
}

private const val NO_READING_TRACK_ALPHA = 0.4f
