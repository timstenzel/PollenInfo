package ch.stenzel.tim.polleninfo.feature.diary.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryDay
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.ui.feeling.label
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.core.ui.species.speciesColor
import kotlin.math.ceil
import kotlinx.datetime.LocalDate

/**
 * The diary graph: one line per species in its palette colour, and the user's [entries] as the
 * feeling line, all on one scale (None at the bottom, Very high at the top). Severity words label
 * the left side, feeling words the right — "Very bad" level with "Very high" — and dates run below
 * in the app's language: "4 Sep" ("4. Sep.") for a week or a month, the month alone ("Oct") for a
 * year, whose ticks are month starts.
 *
 * The feeling line is drawn last, thicker and in `onSurface`, with a dot on every answered day, so
 * it stays distinguishable among the species lines and a single answer between gaps is still seen.
 *
 * Drawn on a plain canvas from [diaryChartGeometry], which holds every layout rule. The whole chart
 * is one accessibility node announced with [diaryChartDescription]; the checkboxes below it are
 * where a screen-reader user learns which species are shown.
 */
@Composable
fun DiaryChart(
    days: List<HistoryDay>,
    speciesIds: List<String>,
    range: HistoryRange,
    entries: List<DiaryEntry>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val feelingColor = MaterialTheme.colorScheme.onSurface
    val colors = speciesIds.associateWith { speciesColor(it) }
    val description = diaryChartDescription(speciesIds.size, range).resolve()
    // Resolved here: the draw lambda below is not composable.
    val severityWords = DiaryLevels.map { it.label() }
    val feelingWords = Feeling.entries.associateWith { it.label() }
    val dates = rememberDateWording()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val levelLabels = severityWords.map { textMeasurer.measure(it, labelStyle) }
        val feelingLabels = feelingWords.mapValues { (_, word) -> textMeasurer.measure(word, labelStyle) }
        // A dot on the first or last day is centred on the plot's edge; its radius keeps it off the labels.
        val gutterStart = levelLabels.maxOf { it.size.width } + LABEL_GAP.toPx() + FEELING_DOT_RADIUS.toPx()
        val gutterEnd = feelingLabels.values.maxOf { it.size.width } + LABEL_GAP.toPx() + FEELING_DOT_RADIUS.toPx()
        val dateLabelHeight = textMeasurer.measure("0", labelStyle).size.height
        // Half a label of room above Very high and below the date labels, so nothing is clipped.
        val top = levelLabels.first().size.height / 2f
        val plotWidth = size.width - gutterStart - gutterEnd
        val plotHeight = size.height - top - dateLabelHeight - LABEL_GAP.toPx()

        val geometry = diaryChartGeometry(days, speciesIds, range, plotWidth, plotHeight, entries)
        val dateLabel: (LocalDate) -> String = if (range == HistoryRange.YEAR) dates::monthOnly else dates::shortDate
        fun at(x: Float, y: Float) = Offset(gutterStart + x, top + y)

        geometry.levelTicks.forEach { tick ->
            drawLine(gridColor, at(0f, tick.y), at(plotWidth, tick.y), strokeWidth = 1.dp.toPx())
            val label = levelLabels[tick.level.ordinal]
            drawText(label, topLeft = Offset(0f, top + tick.y - label.size.height / 2f))
        }

        geometry.feelingTicks.forEach { tick ->
            val label = feelingLabels.getValue(tick.feeling)
            drawText(label, topLeft = Offset(size.width - label.size.width, top + tick.y - label.size.height / 2f))
        }

        val dateLabels = geometry.dateTicks.map { textMeasurer.measure(dateLabel(it.date), labelStyle) }
        val step = dateLabelStep(geometry.dateTicks.map { it.x }, dateLabels.map { it.size.width.toFloat() }, LABEL_GAP.toPx())
        geometry.dateTicks.forEachIndexed { index, tick ->
            // Counted back from the last tick, so the most recent date is always labelled.
            if ((geometry.dateTicks.lastIndex - index) % step != 0) return@forEachIndexed
            val label = dateLabels[index]
            val x = (gutterStart + tick.x - label.size.width / 2f)
                .coerceIn(gutterStart, size.width - label.size.width)
            drawText(label, topLeft = Offset(x, top + plotHeight + LABEL_GAP.toPx()))
        }

        val lineWidth = SPECIES_LINE_WIDTH.toPx()
        fun polyline(run: List<ChartPoint>) = Path().apply {
            moveTo(gutterStart + run[0].x, top + run[0].y)
            run.drop(1).forEach { lineTo(gutterStart + it.x, top + it.y) }
        }

        geometry.lines.forEach { line ->
            val color = colors[line.speciesId] ?: gridColor
            line.runs.forEach { run ->
                if (run.size == 1) {
                    drawCircle(color, radius = lineWidth * 1.5f, center = at(run[0].x, run[0].y))
                } else {
                    drawPath(polyline(run), color, style = Stroke(lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }

        // Last, so it lies on top of every species line.
        val feelingWidth = FEELING_LINE_WIDTH.toPx()
        geometry.feeling.runs.filter { it.size > 1 }.forEach { run ->
            drawPath(polyline(run), feelingColor, style = Stroke(feelingWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        geometry.feeling.dots.forEach { dot ->
            drawCircle(feelingColor, radius = FEELING_DOT_RADIUS.toPx(), center = at(dot.x, dot.y))
        }
    }
}

/**
 * Every how many date ticks a label is drawn, so labels of [widths] centred on [xs] never touch:
 * 1 while the narrowest spacing between ticks fits the widest label plus [gap], more once the
 * language's words are longer ("März", "sept.") than the plot has room for.
 */
internal fun dateLabelStep(xs: List<Float>, widths: List<Float>, gap: Float): Int {
    val spacing = xs.zipWithNext { a, b -> b - a }.minOrNull() ?: return 1
    if (spacing <= 0f) return 1
    val needed = (widths.maxOrNull() ?: 0f) + gap
    return maxOf(1, ceil(needed / spacing).toInt())
}

/** Bottom to top, matching `PollenSeverity`'s order and so [LevelTick.level]'s ordinal. */
private val DiaryLevels = PollenSeverity.entries

private val CHART_HEIGHT = 240.dp
private val LABEL_GAP = 6.dp
private val SPECIES_LINE_WIDTH = 2.dp
private val FEELING_LINE_WIDTH = 3.dp
private val FEELING_DOT_RADIUS = 4.5.dp
