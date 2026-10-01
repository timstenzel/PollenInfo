package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.feature.home.domain.model.ReadingAge
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/**
 * The words for a [ReadingAge]: "Data from 09:00" for a fresh reading; for a stale one "Data from
 * 06:00 today" if it is from today, otherwise "Data from 29 July".
 *
 * Kept apart from the composable so the wording can be tested without UI. Formatted with
 * kotlinx-datetime's own format builders, since `String.format` and `java.time` do not exist in
 * shared code. The clock is always 24-hour — the app's audience is Swiss.
 */
fun ReadingAge.label(): String = when (this) {
    // Not "Updated": beside the refresh time that word would not say which of the two it means.
    is ReadingAge.Fresh -> "Data from ${TIME_FORMAT.format(localTime)}"
    is ReadingAge.Stale.Today -> "Data from ${TIME_FORMAT.format(localTime)} today"
    is ReadingAge.Stale.Earlier -> "Data from ${DATE_FORMAT.format(localDate)}"
}

/**
 * When the app last received its reading: "Refreshed 10:42", or "Refreshed 30 September, 22:10"
 * once that is no longer today — a screen left open overnight would otherwise claim this morning.
 *
 * Only a label, never a warning: an old refresh of a current reading is harmless, and the reading's
 * own age is what [ReadingAge] judges.
 */
fun refreshedLabel(
    refreshedAt: Instant,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = refreshedAt.toLocalDateTime(timeZone)
    val time = TIME_FORMAT.format(local.time)
    return if (local.date == now.toLocalDateTime(timeZone).date) {
        "Refreshed $time"
    } else {
        "Refreshed ${DATE_FORMAT.format(local.date)}, $time"
    }
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
