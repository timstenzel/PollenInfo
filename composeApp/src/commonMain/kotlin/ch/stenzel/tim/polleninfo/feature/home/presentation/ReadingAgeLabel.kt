package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.feature.home.domain.model.ReadingAge
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

/**
 * The words for a [ReadingAge]: "Updated 09:00" for a fresh reading; for a stale one "Data from
 * 06:00 today" if it is from today, otherwise "Data from 29 July".
 *
 * Kept apart from the composable so the wording can be tested without UI. Formatted with
 * kotlinx-datetime's own format builders, since `String.format` and `java.time` do not exist in
 * shared code. The clock is always 24-hour — the app's audience is Swiss.
 */
fun ReadingAge.label(): String = when (this) {
    is ReadingAge.Fresh -> "Updated ${TIME_FORMAT.format(localTime)}"
    is ReadingAge.Stale.Today -> "Data from ${TIME_FORMAT.format(localTime)} today"
    is ReadingAge.Stale.Earlier -> "Data from ${DATE_FORMAT.format(localDate)}"
}

private val TIME_FORMAT = LocalTime.Format {
    hour()
    char(':')
    minute()
}

private val DATE_FORMAT = LocalDate.Format {
    dayOfMonth(Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_FULL)
}
