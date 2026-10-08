package ch.stenzel.tim.polleninfo.core.ui.severity

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.format.ResourceText
import ch.stenzel.tim.polleninfo.core.ui.format.formatTime
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.reading_age_fresh
import ch.stenzel.tim.polleninfo.resources.reading_age_stale_earlier
import ch.stenzel.tim.polleninfo.resources.reading_age_stale_today
import ch.stenzel.tim.polleninfo.resources.reading_refreshed_earlier
import ch.stenzel.tim.polleninfo.resources.reading_refreshed_today
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The words for a [ReadingAge]: "Data from 09:00" for a fresh reading; for a stale one "Data from
 * 06:00 today" if it is from today, otherwise "Data from 29 July".
 *
 * Kept apart from the composable so the wording can be tested without UI. The date is in the
 * language of [dates]; the clock is always 24-hour — the app's audience is Swiss. Each case is its
 * own sentence, since languages word a time and a date differently ("Daten von 09:00", "Daten vom
 * 29. Juli").
 */
fun ReadingAge.label(dates: DateWording): ResourceText = when (this) {
    // Not "Updated": beside the refresh time that word would not say which of the two it means.
    is ReadingAge.Fresh -> ResourceText(Res.string.reading_age_fresh, listOf(formatTime(localTime)))
    is ReadingAge.Stale.Today -> ResourceText(Res.string.reading_age_stale_today, listOf(formatTime(localTime)))
    is ReadingAge.Stale.Earlier -> ResourceText(Res.string.reading_age_stale_earlier, listOf(dates.fullDate(localDate)))
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
): ResourceText {
    val local = refreshedAt.toLocalDateTime(timeZone)
    val time = formatTime(local.time)
    return if (local.date == now.toLocalDateTime(timeZone).date) {
        ResourceText(Res.string.reading_refreshed_today, listOf(time))
    } else {
        ResourceText(Res.string.reading_refreshed_earlier, listOf(dates.fullDate(local.date), time))
    }
}
