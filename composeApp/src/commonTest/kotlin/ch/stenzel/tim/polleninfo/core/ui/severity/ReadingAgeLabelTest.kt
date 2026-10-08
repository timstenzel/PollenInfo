package ch.stenzel.tim.polleninfo.core.ui.severity

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.ui.format.englishDates
import ch.stenzel.tim.polleninfo.core.ui.format.germanDates
import ch.stenzel.tim.polleninfo.core.ui.format.italianDates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

class ReadingAgeLabelTest {

    @Test
    fun `a fresh reading states its time on a 24-hour clock`() {
        assertEquals("Data from 09:00", ReadingAge.Fresh(LocalTime(9, 0)).label(englishDates))
        assertEquals("Data from 17:05", ReadingAge.Fresh(LocalTime(17, 5)).label(englishDates))
    }

    @Test
    fun `a stale reading from an earlier day names its date rather than a time`() {
        assertEquals("Data from 29 July", ReadingAge.Stale.Earlier(LocalDate(2026, 7, 29)).label(englishDates))
        assertEquals("Data from 1 August", ReadingAge.Stale.Earlier(LocalDate(2026, 8, 1)).label(englishDates))
    }

    @Test
    fun `an earlier day's date is in the app's language`() {
        assertEquals("Data from 29 luglio", ReadingAge.Stale.Earlier(LocalDate(2026, 7, 29)).label(italianDates))
        assertEquals("Data from 29. Juli", ReadingAge.Stale.Earlier(LocalDate(2026, 7, 29)).label(germanDates))
    }

    @Test
    fun `a stale reading from today names its time rather than today's date`() {
        assertEquals("Data from 06:00 today", ReadingAge.Stale.Today(LocalTime(6, 0)).label(englishDates))
    }

    @Test
    fun `a refresh from today names only its time`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-08-01T10:42:00Z"),
            now = Instant.parse("2026-08-01T23:59:59Z"),
            dates = englishDates,
            timeZone = TimeZone.UTC,
        )

        assertEquals("Refreshed 10:42", label)
    }

    @Test
    fun `a refresh from an earlier day also names its date`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-07-31T22:10:00Z"),
            now = Instant.parse("2026-08-01T00:00:00Z"),
            dates = englishDates,
            timeZone = TimeZone.UTC,
        )

        assertEquals("Refreshed 31 July, 22:10", label)
    }

    @Test
    fun `the refresh time is shown in the device's zone`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-08-01T08:42:00Z"),
            now = Instant.parse("2026-08-01T09:00:00Z"),
            dates = englishDates,
            timeZone = TimeZone.of("Europe/Zurich"),
        )

        assertEquals("Refreshed 10:42", label)
    }

    @Test
    fun `an earlier refresh names its date in the app's language and its time on a 24-hour clock`() {
        val label = refreshedLabel(
            refreshedAt = Instant.parse("2026-07-31T22:10:00Z"),
            now = Instant.parse("2026-08-01T00:00:00Z"),
            dates = germanDates,
            timeZone = TimeZone.UTC,
        )

        assertEquals("Refreshed 31. Juli, 22:10", label)
    }
}
