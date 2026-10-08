package ch.stenzel.tim.polleninfo.core.ui.severity

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.format.formatTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The words for a [ReadingAge]: "Data from 09:00" for a fresh reading; for a stale one "Data from
 * 06:00 today" if it is from today, otherwise "Data from 29 July".
 *
 * Kept apart from the composable so the wording can be tested without UI. The date is in the
 * language of [dates]; the clock is always 24-hour — the app's audience is Swiss.
 */
fun ReadingAge.label(dates: DateWording): String = when (this) {
    // Not "Updated": beside the refresh time that word would not say which of the two it means.
    is ReadingAge.Fresh -> "Data from ${formatTime(localTime)}"
    is ReadingAge.Stale.Today -> "Data from ${formatTime(localTime)} today"
    is ReadingAge.Stale.Earlier -> "Data from ${dates.fullDate(localDate)}"
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
    dates: DateWording,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = refreshedAt.toLocalDateTime(timeZone)
    val time = formatTime(local.time)
    return if (local.date == now.toLocalDateTime(timeZone).date) {
        "Refreshed $time"
    } else {
        "Refreshed ${dates.fullDate(local.date)}, $time"
    }
}
