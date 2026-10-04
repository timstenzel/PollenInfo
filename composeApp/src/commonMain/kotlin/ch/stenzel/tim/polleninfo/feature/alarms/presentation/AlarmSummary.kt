package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char
import kotlinx.datetime.isoDayNumber

/**
 * The one line a list row says about [alarm], e.g.
 * `Daily report at 08:00 · Mon–Fri · Birch, Grasses ≥ Moderate` or
 * `Threshold alert 07:00–21:00 · Every day · Birch, Grasses ≥ High` — the same three parts for both
 * types, so a list of both scans alike. The station is not part of it: the row shows that as its
 * headline.
 *
 * [speciesNames] maps species ids to display names in the order the app lists pollen types; the
 * selected ones are named in that order, and an id missing from it is shown as itself rather than
 * dropped. Selecting every known type reads "All pollen types". A minimum of
 * [PollenSeverity.NONE] ("Any") adds nothing, since the report is then always sent; a threshold
 * alert never has it.
 *
 * Lives beside the screen rather than in `domain/` because it is wording, and it takes the severity
 * words from `PollenSeverity.label()` so the list cannot spell a band differently from the rest of
 * the app.
 */
fun summaryOf(alarm: Alarm, speciesNames: Map<String, String>): String {
    val type = when (val schedule = alarm.schedule) {
        is AlarmSchedule.Daily -> "Daily report at ${formatTime(schedule.at)}"
        is AlarmSchedule.Threshold -> "Threshold alert ${formatTime(schedule.from)}–${formatTime(schedule.until)}"
    }
    return listOf(
        type,
        daysSummary(alarm.days),
        speciesSummary(alarm.species, speciesNames) + severitySuffix(alarm.minSeverity),
    ).joinToString(SEPARATOR)
}

/**
 * Days in week order, Monday first. Every day is "Every day"; a run of three or more consecutive
 * days collapses to a range ("Mon–Fri"), and shorter runs are listed ("Sat, Sun").
 */
fun daysSummary(days: Set<DayOfWeek>): String {
    if (days.size == DayOfWeek.entries.size) return "Every day"
    val runs = mutableListOf<MutableList<DayOfWeek>>()
    DayOfWeek.entries.filter { it in days }.forEach { day ->
        val run = runs.lastOrNull()
        if (run != null && run.last().isoDayNumber + 1 == day.isoDayNumber) run += day else runs += mutableListOf(day)
    }
    return runs.joinToString(", ") { run ->
        if (run.size >= MIN_RANGE_LENGTH) {
            "${run.first().shortName()}–${run.last().shortName()}"
        } else {
            run.joinToString(", ") { it.shortName() }
        }
    }
}

/** `HH:mm`, the 24-hour form Swiss time is written in. */
fun formatTime(time: LocalTime): String = TIME_FORMAT.format(time)

/** The three-letter English abbreviation, as on the editor's day chips. */
fun DayOfWeek.shortName(): String = SHORT_DAY_NAMES[isoDayNumber - 1]

/** What the editor calls a minimum severity: [PollenSeverity.NONE] is "Any", not "None". */
fun PollenSeverity.minimumLabel(): String = if (this == PollenSeverity.NONE) "Any" else label()

private fun speciesSummary(selected: Set<String>, speciesNames: Map<String, String>): String {
    if (speciesNames.isNotEmpty() && selected.containsAll(speciesNames.keys)) return "All pollen types"
    val known = speciesNames.keys.filter { it in selected }.map { speciesNames.getValue(it) }
    val unknown = selected.filterNot { it in speciesNames }.sorted()
    return (known + unknown).joinToString(", ")
}

private fun severitySuffix(minSeverity: PollenSeverity): String =
    if (minSeverity == PollenSeverity.NONE) "" else " ≥ ${minSeverity.label()}"

/** Indexed by ISO day number, Monday first. A list rather than a `when`: `DayOfWeek` is an expect enum. */
private val SHORT_DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private const val SEPARATOR = " · "

private const val MIN_RANGE_LENGTH = 3

private val TIME_FORMAT = LocalTime.Format {
    hour()
    char(':')
    minute()
}
