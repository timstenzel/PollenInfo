package ch.stenzel.tim.polleninfo.core.ui.severity

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.format.ResourceText
import ch.stenzel.tim.polleninfo.core.ui.format.englishDates
import ch.stenzel.tim.polleninfo.core.ui.format.germanDates
import ch.stenzel.tim.polleninfo.core.ui.format.italianDates
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.reading_age_fresh
import ch.stenzel.tim.polleninfo.resources.reading_age_stale_earlier
import ch.stenzel.tim.polleninfo.resources.reading_age_stale_today
import ch.stenzel.tim.polleninfo.resources.reading_refreshed_earlier
import ch.stenzel.tim.polleninfo.resources.reading_refreshed_today
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

/**
 * The sentences are translations, so these pin which one is chosen and what goes into it — the
 * times on a 24-hour clock and the dates in the language of the given [DateWording].
 */
class ReadingAgeLabelTest {

    private fun earlier(date: String) = ResourceText(Res.string.reading_age_stale_earlier, listOf(date))

    @Test
    fun `a fresh reading states its time on a 24-hour clock`() {
        assertEquals(ResourceText(Res.string.reading_age_fresh, listOf("09:00")), ReadingAge.Fresh(LocalTime(9, 0)).label(englishDates))
        assertEquals(ResourceText(Res.string.reading_age_fresh, listOf("17:05")), ReadingAge.Fresh(LocalTime(17, 5)).label(englishDates))
    }

    @Test
    fun `a stale reading from an earlier day names its date rather than a time`() {
        assertEquals(earlier("29 July"), ReadingAge.Stale.Earlier(LocalDate(2026, 7, 29)).label(englishDates))
        assertEquals(earlier("1 August"), ReadingAge.Stale.Earlier(LocalDate(2026, 8, 1)).label(englishDates))
    }

    @Test
    fun `an earlier day's date is in the app's language`() {
        assertEquals(earlier("29 luglio"), ReadingAge.Stale.Earlier(LocalDate(2026, 7, 29)).label(italianDates))
        assertEquals(earlier("29. Juli"), ReadingAge.Stale.Earlier(LocalDate(2026, 7, 29)).label(germanDates))
    }

    @Test
    fun `a stale reading from today names its time rather than today's date`() {
        assertEquals(ResourceText(Res.string.reading_age_stale_today, listOf("06:00")), ReadingAge.Stale.Today(LocalTime(6, 0)).label(englishDates))
    }

    @Test
    fun `a refresh from today names only its time`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-08-01T10:42:00Z"),
            now = Instant.parse("2026-08-01T23:59:59Z"),
            dates = englishDates,
            timeZone = TimeZone.UTC,
        )

        assertEquals(ResourceText(Res.string.reading_refreshed_today, listOf("10:42")), label)
    }

    @Test
    fun `a refresh from an earlier day also names its date`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-07-31T22:10:00Z"),
            now = Instant.parse("2026-08-01T00:00:00Z"),
            dates = englishDates,
            timeZone = TimeZone.UTC,
        )

        assertEquals(ResourceText(Res.string.reading_refreshed_earlier, listOf("31 July", "22:10")), label)
    }

    @Test
    fun `the refresh time is shown in the device's zone`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-08-01T08:42:00Z"),
            now = Instant.parse("2026-08-01T09:00:00Z"),
            dates = englishDates,
            timeZone = TimeZone.of("Europe/Zurich"),
        )

        assertEquals(ResourceText(Res.string.reading_refreshed_today, listOf("10:42")), label)
    }

    @Test
    fun `an earlier refresh names its date in the app's language and its time on a 24-hour clock`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-07-31T22:10:00Z"),
            now = Instant.parse("2026-08-01T00:00:00Z"),
            dates = germanDates,
            timeZone = TimeZone.UTC,
        )

        assertEquals(ResourceText(Res.string.reading_refreshed_earlier, listOf("31. Juli", "22:10")), label)
    }
}
