package ch.stenzel.tim.polleninfo.core.measurement.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

class ReadingAgeTest {

    private val measuredAt = Instant.parse("2026-08-01T07:00:00Z")

    @Test
    fun `a reading just inside three hours old is fresh`() {
        val age = readingAgeOf(measuredAt, now = measuredAt + 3.hours - 1.nanoseconds, TimeZone.UTC)

        assertEquals(ReadingAge.Fresh(LocalTime(7, 0)), age)
    }

    @Test
    fun `a reading exactly three hours old is stale`() {
        val age = readingAgeOf(measuredAt, now = measuredAt + 3.hours, TimeZone.UTC)

        assertEquals(ReadingAge.Stale.Today(LocalTime(7, 0)), age)
    }

    @Test
    fun `a reading just over three hours old is stale`() {
        val age = readingAgeOf(measuredAt, now = measuredAt + 3.hours + 1.nanoseconds, TimeZone.UTC)

        assertEquals(ReadingAge.Stale.Today(LocalTime(7, 0)), age)
    }

    @Test
    fun `a stale reading from today is named by its time rather than today's date`() {
        val age = readingAgeOf(measuredAt, now = Instant.parse("2026-08-01T23:59:59Z"), TimeZone.UTC)

        assertEquals(ReadingAge.Stale.Today(LocalTime(7, 0)), age)
    }

    @Test
    fun `a stale reading from yesterday is named by its date`() {
        val age = readingAgeOf(measuredAt, now = Instant.parse("2026-08-02T00:00:00Z"), TimeZone.UTC)

        assertEquals(ReadingAge.Stale.Earlier(LocalDate(2026, 8, 1)), age)
    }

    @Test
    fun `whether a stale reading is from today is decided in the device's zone`() {
        // 21:30 UTC on 31 July is 23:30 in Zürich; 01:30 UTC on 1 August is 03:30 there — four
        // hours later and across local midnight, so the reading is from yesterday.
        val lateEvening = Instant.parse("2026-07-31T21:30:00Z")

        val age = readingAgeOf(lateEvening, now = lateEvening + 4.hours, ZURICH)

        assertEquals(ReadingAge.Stale.Earlier(LocalDate(2026, 7, 31)), age)
    }

    @Test
    fun `a reading taken this instant is fresh`() {
        assertEquals(ReadingAge.Fresh(LocalTime(7, 0)), readingAgeOf(measuredAt, measuredAt, TimeZone.UTC))
    }

    @Test
    fun `a reading timestamped slightly in the future is fresh`() {
        val age = readingAgeOf(measuredAt, now = measuredAt - 5.minutes, TimeZone.UTC)

        assertEquals(ReadingAge.Fresh(LocalTime(7, 0)), age)
    }

    @Test
    fun `a fresh reading is given in the device's zone rather than the source's UTC`() {
        // 07:00 UTC is 09:00 in Zürich in summer (CEST, UTC+2) — the UAT's "updated at 09:00".
        val age = readingAgeOf(measuredAt, now = measuredAt + 30.minutes, ZURICH)

        assertEquals(ReadingAge.Fresh(LocalTime(9, 0)), age)
    }

    @Test
    fun `a fresh reading in winter is shifted by one hour rather than two`() {
        val winter = Instant.parse("2026-01-15T07:00:00Z")

        assertEquals(ReadingAge.Fresh(LocalTime(8, 0)), readingAgeOf(winter, winter, ZURICH))
    }

    @Test
    fun `a stale reading is dated in the device's zone even where that crosses midnight`() {
        // 23:30 UTC on 31 July is already 1 August in Zürich.
        val lateEvening = Instant.parse("2026-07-31T23:30:00Z")

        val age = readingAgeOf(lateEvening, now = lateEvening + 48.hours, ZURICH)

        assertEquals(ReadingAge.Stale.Earlier(LocalDate(2026, 8, 1)), age)
    }

    private companion object {
        val ZURICH = TimeZone.of("Europe/Zurich")
    }
}
