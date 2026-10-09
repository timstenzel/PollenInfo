package ch.stenzel.tim.polleninfo.core.ui.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

/**
 * The month and weekday names of one language and how it writes a day with a month — everything a
 * date needs to read naturally: "29 July", "29. Juli", "29 juillet", "29 luglio".
 *
 * A plain value with pure formatting functions, so the forms are tested per language with literal
 * fixtures and no resource loading. The app builds it from string resources with
 * [rememberDateWording] (composables) or [loadDateWording] (suspending code outside Compose).
 *
 * [dayMonthPattern] places the day (`%1$s`) and the month name (`%2$s`): "%1$s %2$s", or
 * "%1$s. %2$s" in German. Abbreviations carry their own full stop where the language writes one
 * ("Sep.", "sept."), so the short forms need no flag of their own. Weekday lists are Monday first.
 *
 * Times are not part of it: they are `HH:mm` ([formatTime]) in every language.
 */
data class DateWording(
    val monthsFull: List<String>,
    val monthsShort: List<String>,
    val weekdaysFull: List<String>,
    val weekdaysShort: List<String>,
    val dayMonthPattern: String,
) {
    init {
        require(monthsFull.size == MONTHS && monthsShort.size == MONTHS) { "12 month names expected" }
        require(weekdaysFull.size == WEEKDAYS && weekdaysShort.size == WEEKDAYS) { "7 weekday names expected" }
    }

    /** "29 July" — a stale reading's date, a refresh from an earlier day. */
    fun fullDate(date: LocalDate): String = dayMonth(date.day, monthsFull[date.month.number - 1])

    /** "4 Sep" — the Diary's week and month axis. */
    fun shortDate(date: LocalDate): String = dayMonth(date.day, monthsShort[date.month.number - 1])

    /** "Oct" — the Diary's year axis, whose ticks are month starts. */
    fun monthOnly(date: LocalDate): String = monthsShort[date.month.number - 1]

    /** "Mon" — day chips and alarm summaries. */
    fun weekdayShort(day: DayOfWeek): String = weekdaysShort[day.isoDayNumber - 1]

    /** "Monday" — what a screen reader says for a day chip. */
    fun weekdayFull(day: DayOfWeek): String = weekdaysFull[day.isoDayNumber - 1]

    /**
     * [days] in week order, Monday first: a run of three or more consecutive days collapses to a
     * range ("Mon–Fri"), shorter runs are listed ("Sat, Sun"). All seven days are a single range
     * here; a caller that has a word for "every day" checks for that itself.
     */
    fun weekdays(days: Set<DayOfWeek>): String {
        val runs = mutableListOf<MutableList<DayOfWeek>>()
        DayOfWeek.entries.filter { it in days }.forEach { day ->
            val run = runs.lastOrNull()
            if (run != null && run.last().isoDayNumber + 1 == day.isoDayNumber) run += day else runs += mutableListOf(day)
        }
        return runs.joinToString(LIST_SEPARATOR) { run ->
            if (run.size >= MIN_RANGE_LENGTH) {
                "${weekdayShort(run.first())}–${weekdayShort(run.last())}"
            } else {
                run.joinToString(LIST_SEPARATOR) { weekdayShort(it) }
            }
        }
    }

    private fun dayMonth(day: Int, month: String): String =
        dayMonthPattern.replace("%1\$s", day.toString()).replace("%2\$s", month)

    private companion object {
        const val MONTHS = 12
        const val WEEKDAYS = 7
        const val MIN_RANGE_LENGTH = 3
        const val LIST_SEPARATOR = ", "
    }
}

/** `HH:mm`, the 24-hour form the app writes every time in, whatever the language. */
fun formatTime(time: LocalTime): String = TIME_FORMAT.format(time)

private val TIME_FORMAT = LocalTime.Format {
    hour()
    char(':')
    minute()
}
