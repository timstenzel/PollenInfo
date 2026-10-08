package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.runtime.Composable
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.format.formatTime
import ch.stenzel.tim.polleninfo.core.ui.severity.labelResource
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_minimum_any
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The one line a list row says about [alarm], e.g.
 * `Daily report at 08:00 · Mon–Fri · Birch, Grasses ≥ Moderate` or
 * `Threshold alert 07:00–21:00 · Every day · Birch, Grasses ≥ High` — the same three parts for both
 * types, so a list of both scans alike. The station is not part of it: the row shows that as its
 * headline.
 *
 * [speciesNames] maps species ids to display names — already in the app's language — in the order
 * the app lists pollen types; the selected ones are named in that order, and an id missing from it
 * is shown as itself rather than dropped. Selecting every known type reads "All pollen types". A
 * minimum of [PollenSeverity.NONE] ("Any") adds nothing, since the report is then always sent; a
 * threshold alert never has it.
 *
 * Lives beside the screen rather than in `domain/` because it is wording. The screen resolves the
 * words — [severityLabels] from `PollenSeverity.label()`, so the list cannot spell a band
 * differently from the rest of the app, and the weekday names from [dates] — so this stays pure.
 */
fun summaryOf(
    alarm: Alarm,
    speciesNames: Map<String, String>,
    severityLabels: Map<PollenSeverity, String>,
    dates: DateWording,
): String {
    val type = when (val schedule = alarm.schedule) {
        is AlarmSchedule.Daily -> "Daily report at ${formatTime(schedule.at)}"
        is AlarmSchedule.Threshold -> "Threshold alert ${formatTime(schedule.from)}–${formatTime(schedule.until)}"
    }
    return listOf(
        type,
        daysSummary(alarm.days, dates),
        speciesSummary(alarm.species, speciesNames) + severitySuffix(alarm.minSeverity, severityLabels),
    ).joinToString(SEPARATOR)
}

/**
 * Days in week order, Monday first, in the language of [dates]. Every day is "Every day";
 * otherwise [DateWording.weekdays] collapses runs of three or more ("Mon–Fri") and lists the rest
 * ("Sat, Sun").
 */
fun daysSummary(days: Set<DayOfWeek>, dates: DateWording): String =
    if (days.size == DayOfWeek.entries.size) "Every day" else dates.weekdays(days)

/** What the editor calls a minimum severity: [PollenSeverity.NONE] is "Any", not "None". */
@Composable
fun PollenSeverity.minimumLabel(): String = stringResource(minimumLabelResource())

/** The resource behind [minimumLabel]; the other severities share `PollenSeverity.label()`'s words. */
fun PollenSeverity.minimumLabelResource(): StringResource =
    if (this == PollenSeverity.NONE) Res.string.alarm_minimum_any else labelResource()

private fun speciesSummary(selected: Set<String>, speciesNames: Map<String, String>): String {
    if (speciesNames.isNotEmpty() && selected.containsAll(speciesNames.keys)) return "All pollen types"
    val known = speciesNames.keys.filter { it in selected }.map { speciesNames.getValue(it) }
    val unknown = selected.filterNot { it in speciesNames }.sorted()
    return (known + unknown).joinToString(", ")
}

private fun severitySuffix(minSeverity: PollenSeverity, severityLabels: Map<PollenSeverity, String>): String =
    if (minSeverity == PollenSeverity.NONE) "" else " ≥ ${severityLabels.getValue(minSeverity)}"

private const val SEPARATOR = " · "
