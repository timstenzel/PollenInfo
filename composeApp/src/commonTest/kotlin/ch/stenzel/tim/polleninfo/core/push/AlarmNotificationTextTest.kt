package ch.stenzel.tim.polleninfo.core.push

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.ui.format.englishDates
import ch.stenzel.tim.polleninfo.core.ui.format.germanDates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

/**
 * Asserts on resource keys and arguments, never on English sentences: the fake lookup writes each
 * string as `key(arg1|arg2)`.
 */
class AlarmNotificationTextTest {

    private val strings = NotificationStrings { resource, args -> "${resource.key}(${args.joinToString("|")})" }

    private fun clockAt(instant: String) = object : Clock {
        override fun now() = Instant.parse(instant)
    }

    /** Monday 3 August 2026, 08:00 in Zürich (summer time, UTC+2). */
    private val mondayMorning = clockAt("2026-08-03T06:00:00Z")

    private suspend fun text(content: AlarmNotificationContent, clock: Clock = mondayMorning) =
        notificationText(content, englishDates, clock, strings)

    private fun noCurrentReading(measuredAt: String) =
        AlarmNotificationContent.NoCurrentReading("Zürich", AlarmChannel.DAILY_REPORT, Instant.parse(measuredAt))

    @Test
    fun `the title names the station`() = runTest {
        val text = text(AlarmNotificationContent.NoPollen("Zürich", AlarmChannel.DAILY_REPORT))

        assertEquals("notification_title(Zürich)", text.title)
    }

    @Test
    fun `without a station the title is the station-less one`() = runTest {
        val text = text(AlarmNotificationContent.Generic("", AlarmChannel.DAILY_REPORT))

        assertEquals("notification_title_no_station()", text.title)
    }

    @Test
    fun `a report lists each type with its severity word in the order given`() = runTest {
        val content = AlarmNotificationContent.Levels(
            "Zürich",
            AlarmChannel.DAILY_REPORT,
            listOf("GRASSES" to PollenSeverity.HIGH, "BIRCH" to PollenSeverity.MODERATE),
            isAlert = false,
        )

        assertEquals(
            "notification_level(species_grasses()|severity_high()) · notification_level(species_birch()|severity_moderate())",
            text(content).body,
        )
    }

    @Test
    fun `an alert is worded like a report`() = runTest {
        val content = AlarmNotificationContent.Levels(
            "Zürich",
            AlarmChannel.THRESHOLD_ALERT,
            listOf("ASH" to PollenSeverity.VERY_HIGH),
            isAlert = true,
        )

        val text = text(content)

        assertEquals("notification_title(Zürich)", text.title)
        assertEquals("notification_level(species_ash()|severity_very_high())", text.body)
    }

    @Test
    fun `the fixed bodies use their own keys`() = runTest {
        assertEquals("notification_no_pollen()", text(AlarmNotificationContent.NoPollen("Zürich", AlarmChannel.DAILY_REPORT)).body)
        assertEquals(
            "notification_not_reported()",
            text(AlarmNotificationContent.NotReported("Zürich", AlarmChannel.DAILY_REPORT)).body,
        )
        assertEquals(
            "notification_unavailable(Zürich)",
            text(AlarmNotificationContent.Unavailable("Zürich", AlarmChannel.DAILY_REPORT)).body,
        )
        assertEquals("notification_generic()", text(AlarmNotificationContent.Generic("Zürich", AlarmChannel.DAILY_REPORT)).body)
    }

    @Test
    fun `an old reading from today is named by its Swiss time`() = runTest {
        assertEquals(
            "notification_no_current_reading_today(Zürich|05:00)",
            text(noCurrentReading("2026-08-03T03:00:00Z")).body,
        )
    }

    @Test
    fun `a reading from just after Swiss midnight is today although its UTC date is yesterday`() = runTest {
        assertEquals(
            "notification_no_current_reading_today(Zürich|00:30)",
            text(noCurrentReading("2026-08-02T22:30:00Z")).body,
        )
    }

    @Test
    fun `a reading from just before Swiss midnight is yesterday`() = runTest {
        assertEquals(
            "notification_no_current_reading_yesterday(Zürich|23:59)",
            text(noCurrentReading("2026-08-02T21:59:00Z")).body,
        )
    }

    @Test
    fun `the same reading is today one minute before Swiss midnight and yesterday at it`() = runTest {
        val reading = noCurrentReading("2026-08-03T20:00:00Z") // 22:00 Monday in Zürich

        assertEquals(
            "notification_no_current_reading_today(Zürich|22:00)",
            text(reading, clockAt("2026-08-03T21:59:00Z")).body,
        )
        assertEquals(
            "notification_no_current_reading_yesterday(Zürich|22:00)",
            text(reading, clockAt("2026-08-03T22:00:00Z")).body,
        )
    }

    @Test
    fun `yesterday's reading becomes a date at the next Swiss midnight`() = runTest {
        val reading = noCurrentReading("2026-08-02T20:00:00Z") // 22:00 Sunday in Zürich

        assertEquals(
            "notification_no_current_reading_yesterday(Zürich|22:00)",
            text(reading, clockAt("2026-08-03T21:59:00Z")).body,
        )
        assertEquals(
            "notification_no_current_reading_earlier(Zürich|2 August)",
            text(reading, clockAt("2026-08-03T22:00:00Z")).body,
        )
    }

    @Test
    fun `an earlier date is written in the app's language`() = runTest {
        val text = notificationText(noCurrentReading("2026-07-29T07:00:00Z"), germanDates, mondayMorning, strings)

        assertEquals("notification_no_current_reading_earlier(Zürich|29. Juli)", text.body)
    }
}
