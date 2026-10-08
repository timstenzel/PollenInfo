package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.format.formatTime
import ch.stenzel.tim.polleninfo.core.ui.severity.labelResource
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_minimum_any
import ch.stenzel.tim.polleninfo.resources.alarm_paused
import ch.stenzel.tim.polleninfo.resources.alarm_summary_all_types
import ch.stenzel.tim.polleninfo.resources.alarm_summary_daily
import ch.stenzel.tim.polleninfo.resources.alarm_summary_every_day
import ch.stenzel.tim.polleninfo.resources.alarm_summary_threshold
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The words of an alarm summary in the app's language, as a pure value so [summaryOf] stays pure.
 * The patterns keep their positional placeholders: [dailyReport] takes the time (`%1$s`),
 * [thresholdAlert] the window's start and end (`%1$s`, `%2$s`), [paused] the rest of the summary.
 */
data class AlarmSummaryWording(
    val dailyReport: String,
    val thresholdAlert: String,
    val everyDay: String,
    val allPollenTypes: String,
    val paused: String,
)

/** The [AlarmSummaryWording] of the app's current language, for composables. */
@Composable
fun rememberAlarmSummaryWording(): AlarmSummaryWording {
    // Read without arguments, so the placeholders come back unformatted.
    val dailyReport = stringResource(Res.string.alarm_summary_daily)
    val thresholdAlert = stringResource(Res.string.alarm_summary_threshold)
    val everyDay = stringResource(Res.string.alarm_summary_every_day)
    val allPollenTypes = stringResource(Res.string.alarm_summary_all_types)
    val paused = stringResource(Res.string.alarm_paused)
    return remember(dailyReport, thresholdAlert, everyDay, allPollenTypes, paused) {
        AlarmSummaryWording(dailyReport, thresholdAlert, everyDay, allPollenTypes, paused)
    }
}

/**
 * The one line a list row says about [alarm], e.g.
 * `Daily report at 08:00 · Mon–Fri · Birch, Grasses ≥ Moderate` or
 * `Threshold alert 07:00–21:00 · Every day · Birch, Grasses ≥ High` — the same three parts for both
 * types, so a list of both scans alike — prefixed `Paused · ` while the alarm is paused. The station
 * is not part of it: the row shows that as its headline.
 *
 * [speciesNames] maps species ids to display names — already in the app's language — in the order
 * the app lists pollen types; the selected ones are named in that order, and an id missing from it
 * is shown as itself rather than dropped. Selecting every known type reads "All pollen types". A
 * minimum of [PollenSeverity.NONE] ("Any") adds nothing, since the report is then always sent; a
 * threshold alert never has it.
 *
 * Lives beside the screen rather than in `domain/` because it is wording. The screen resolves the
 * words — [severityLabels] from `PollenSeverity.label()`, so the list cannot spell a band
 * differently from the rest of the app, the weekday names from [dates] and the sentences from
 * [wording] — so this stays pure.
 */
fun summaryOf(
    alarm: Alarm,
    speciesNames: Map<String, String>,
    severityLabels: Map<PollenSeverity, String>,
    dates: DateWording,
    wording: AlarmSummaryWording,
): String {
    val type = when (val schedule = alarm.schedule) {
        is AlarmSchedule.Daily -> wording.dailyReport.fill(formatTime(schedule.at))
        is AlarmSchedule.Threshold -> wording.thresholdAlert.fill(formatTime(schedule.from), formatTime(schedule.until))
    }
    val summary = listOf(
        type,
        daysSummary(alarm.days, dates, wording),
        speciesSummary(alarm.species, speciesNames, wording) + severitySuffix(alarm.minSeverity, severityLabels),
    ).joinToString(SEPARATOR)
    return if (alarm.enabled) summary else wording.paused.fill(summary)
}

/**
 * Days in week order, Monday first, in the language of [dates]. Every day is [wording]'s "Every
 * day"; otherwise [DateWording.weekdays] collapses runs of three or more ("Mon–Fri") and lists the
 * rest ("Sat, Sun").
 */
fun daysSummary(days: Set<DayOfWeek>, dates: DateWording, wording: AlarmSummaryWording): String =
    if (days.size == DayOfWeek.entries.size) wording.everyDay else dates.weekdays(days)

/** What the editor calls a minimum severity: [PollenSeverity.NONE] is "Any", not "None". */
@Composable
fun PollenSeverity.minimumLabel(): String = stringResource(minimumLabelResource())

/** The resource behind [minimumLabel]; the other severities share `PollenSeverity.label()`'s words. */
fun PollenSeverity.minimumLabelResource(): StringResource =
    if (this == PollenSeverity.NONE) Res.string.alarm_minimum_any else labelResource()

private fun speciesSummary(
    selected: Set<String>,
    speciesNames: Map<String, String>,
    wording: AlarmSummaryWording,
): String {
    if (speciesNames.isNotEmpty() && selected.containsAll(speciesNames.keys)) return wording.allPollenTypes
    val known = speciesNames.keys.filter { it in selected }.map { speciesNames.getValue(it) }
    val unknown = selected.filterNot { it in speciesNames }.sorted()
    return (known + unknown).joinToString(", ")
}

private fun severitySuffix(minSeverity: PollenSeverity, severityLabels: Map<PollenSeverity, String>): String =
    if (minSeverity == PollenSeverity.NONE) "" else " ≥ ${severityLabels.getValue(minSeverity)}"

/** Puts [args] into the positional placeholders `%1$s`, `%2$s`, … — all Compose resources use. */
private fun String.fill(vararg args: String): String =
    args.foldIndexed(this) { index, text, arg -> text.replace("%${index + 1}\$s", arg) }

private const val SEPARATOR = " · "
