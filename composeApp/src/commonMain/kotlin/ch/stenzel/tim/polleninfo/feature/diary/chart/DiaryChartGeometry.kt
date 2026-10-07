package ch.stenzel.tim.polleninfo.feature.diary.chart

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryDay
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlinx.datetime.LocalDate

/** A point in the plot area, in px from its top-left corner. */
data class ChartPoint(val x: Float, val y: Float)

/**
 * One species' line. [runs] are the polylines between missing days, in date order: a day without a
 * value ends a run, so the line never pretends to know it. A run of one point is kept — a single
 * known day between two gaps must still be drawn.
 */
data class SpeciesLine(val speciesId: String, val runs: List<List<ChartPoint>>)

/**
 * The user's feeling line, split like a [SpeciesLine] at every unanswered day. [dots] holds one
 * point per answered day, so an answer between two gaps is still seen.
 */
data class FeelingLine(val runs: List<List<ChartPoint>>) {
    val dots: List<ChartPoint> get() = runs.flatten()
}

/** A horizontal grid line at [level]'s height. */
data class LevelTick(val level: PollenSeverity, val y: Float)

/** A labelled day on the x-axis. */
data class DateTick(val date: LocalDate, val x: Float)

/** A feeling word on the second side of the y-axis, at the height of the level it shares. */
data class FeelingTick(val feeling: Feeling, val y: Float)

data class DiaryChartGeometry(
    val lines: List<SpeciesLine>,
    val feeling: FeelingLine,
    val levelTicks: List<LevelTick>,
    val feelingTicks: List<FeelingTick>,
    val dateTicks: List<DateTick>,
)

/**
 * Lays out the diary chart for a plot area of [width] × [height] px. Pure — no Compose — so the
 * mapping is tested in `commonTest`, like `SwissMapProjection`.
 *
 * - **x** is the day's index in [days]: the first day at 0, the last at [width], evenly between.
 * - **y** is the severity level, `NONE` = 0 … `VERY_HIGH` = 4, inverted so a higher point is
 *   always worse: `VERY_HIGH` at the top (0), `NONE` at the bottom ([height]).
 * - One line per id in [speciesIds], in that order; a species with no value on a day has a gap there.
 * - The feeling line from [entries] on the same scale — each feeling at its [Feeling.level], so
 *   "Very bad" is level with `VERY_HIGH` — with a gap on every day without an answer. Entries for
 *   dates outside [days] are ignored.
 * - Date ticks thinned to what fits the [range] ([dateTickIndices]).
 */
fun diaryChartGeometry(
    days: List<HistoryDay>,
    speciesIds: List<String>,
    range: HistoryRange,
    width: Float,
    height: Float,
    entries: List<DiaryEntry> = emptyList(),
): DiaryChartGeometry {
    fun xOf(index: Int): Float = if (days.size <= 1) width / 2 else width * index / (days.size - 1)

    /** Polylines through each day's level, split wherever [levelOn] has none. */
    fun runs(levelOn: (HistoryDay) -> PollenSeverity?): List<List<ChartPoint>> {
        val runs = mutableListOf<List<ChartPoint>>()
        var run = mutableListOf<ChartPoint>()
        days.forEachIndexed { index, day ->
            val level = levelOn(day)
            if (level == null) {
                if (run.isNotEmpty()) runs += run
                run = mutableListOf()
            } else {
                run += ChartPoint(xOf(index), levelY(level, height))
            }
        }
        if (run.isNotEmpty()) runs += run
        return runs
    }

    val feelings = entries.associate { it.date to it.feeling }

    return DiaryChartGeometry(
        lines = speciesIds.map { id -> SpeciesLine(id, runs { it.levels[id] }) },
        feeling = FeelingLine(runs { feelings[it.date]?.level }),
        levelTicks = PollenSeverity.entries.map { LevelTick(it, levelY(it, height)) },
        feelingTicks = Feeling.entries.map { FeelingTick(it, levelY(it.level, height)) },
        dateTicks = dateTickIndices(days, range).map { DateTick(days[it].date, xOf(it)) },
    )
}

/**
 * Which days get a date label, by range:
 *
 * - **Week** — every day; seven labels fit.
 * - **Month** — every seventh day, counted back from the last so yesterday is always labelled.
 * - **Year** — the first of each month; a weekly label would be fifty-odd and unreadable, and month
 *   starts are what a year is read by.
 */
private fun dateTickIndices(days: List<HistoryDay>, range: HistoryRange): List<Int> = when (range) {
    HistoryRange.WEEK -> days.indices.toList()
    HistoryRange.MONTH -> days.indices.filter { (days.lastIndex - it) % 7 == 0 }
    HistoryRange.YEAR -> days.indices.filter { days[it].date.dayOfMonth == 1 }
}

/** The height of [level] in a plot [height] px tall — higher means worse. */
fun levelY(level: PollenSeverity, height: Float): Float =
    height * (1f - level.ordinal.toFloat() / PollenSeverity.VERY_HIGH.ordinal)
