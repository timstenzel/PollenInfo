package ch.stenzel.tim.polleninfo.server.alarm.domain

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.measurement.CacheResult
import ch.stenzel.tim.polleninfo.server.pollen.measurement.SpeciesMeasurement
import ch.stenzel.tim.polleninfo.server.pollen.measurement.StationMeasurement
import java.io.IOException
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AlarmRulesTest {

    /** Monday 3 August 2026, 08:00 Swiss summer time. */
    private val mondayAtEight: ZonedDateTime = ZonedDateTime.of(2026, 8, 3, 8, 0, 0, 0, ALARM_ZONE)

    private fun alarm(
        enabled: Boolean = true,
        species: Set<PollenSpecies> = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES),
        minSeverity: PollenSeverity = PollenSeverity.NONE,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        at: LocalTime = LocalTime.of(8, 0),
    ) = Alarm(
        id = AlarmId("alarm-1"),
        deviceId = DeviceId("device-1"),
        enabled = enabled,
        station = PollenStation.ZUERICH,
        species = species,
        minSeverity = minSeverity,
        days = days,
        schedule = AlarmSchedule.Daily(at),
    )

    private fun reading(
        measuredAt: Instant = mondayAtEight.minusMinutes(30).toInstant(),
        severities: Map<PollenSpecies, PollenSeverity> = mapOf(
            PollenSpecies.BIRCH to PollenSeverity.MODERATE,
            PollenSpecies.GRASSES to PollenSeverity.HIGH,
        ),
    ) = StationMeasurement(
        station = PollenStation.ZUERICH,
        measuredAt = measuredAt,
        species = PollenSpecies.entries.map { species ->
            val severity = severities[species]
            SpeciesMeasurement(species, concentration = severity?.let { 1 }, severity = severity)
        },
    )

    private fun fresh(measurement: StationMeasurement = reading()) = CacheResult.Fresh(measurement)

    // --- timing ---

    @Test
    fun `a daily report is due at its exact minute`() {
        assertNotNull(AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh()))
    }

    @Test
    fun `a daily report is still due later within its minute`() {
        assertNotNull(AlarmRules.evaluateDaily(alarm(), mondayAtEight.plusSeconds(59), fresh()))
    }

    @Test
    fun `a daily report is not due one minute before its time`() {
        assertNull(AlarmRules.evaluateDaily(alarm(), mondayAtEight.minusMinutes(1), fresh()))
    }

    @Test
    fun `a daily report is not due one minute after its time`() {
        assertNull(AlarmRules.evaluateDaily(alarm(), mondayAtEight.plusMinutes(1), fresh()))
    }

    @Test
    fun `a daily report is not due on a day it excludes`() {
        val weekend = alarm(days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))

        assertNull(AlarmRules.evaluateDaily(weekend, mondayAtEight, fresh()))
    }

    @Test
    fun `a disabled daily report is never due`() {
        assertNull(AlarmRules.evaluateDaily(alarm(enabled = false), mondayAtEight, fresh()))
    }

    @Test
    fun `the time is compared in Swiss time whatever zone now is given in`() {
        val fiveUtc = mondayAtEight.toInstant().atZone(ZoneOffset.UTC)

        assertNotNull(AlarmRules.evaluateDaily(alarm(), fiveUtc, fresh()))
        assertNull(AlarmRules.evaluateDaily(alarm(at = LocalTime.of(6, 0)), fiveUtc, fresh()))
    }

    @Test
    fun `an eight o clock report fires at eight local time on the day summer time starts`() {
        // 29 March 2026: clocks go from 02:00 to 03:00, so 08:00 local is 06:00 UTC.
        val sunday = alarm(days = setOf(DayOfWeek.SUNDAY))
        val eight = Instant.parse("2026-03-29T06:00:00Z").atZone(ZoneOffset.UTC)
        val reading = fresh(reading(measuredAt = eight.minusMinutes(30).toInstant()))

        assertNotNull(AlarmRules.evaluateDaily(sunday, eight, reading))
        assertNull(AlarmRules.evaluateDaily(sunday, eight.plusHours(1), reading))
        assertNull(AlarmRules.evaluateDaily(sunday, eight.minusHours(1), reading))
    }

    @Test
    fun `an eight o clock report fires at eight local time on the day summer time ends`() {
        // 25 October 2026: clocks go from 03:00 back to 02:00, so 08:00 local is 07:00 UTC.
        val sunday = alarm(days = setOf(DayOfWeek.SUNDAY))
        val eight = Instant.parse("2026-10-25T07:00:00Z").atZone(ZoneOffset.UTC)
        val reading = fresh(reading(measuredAt = eight.minusMinutes(30).toInstant()))

        assertNotNull(AlarmRules.evaluateDaily(sunday, eight, reading))
        assertNull(AlarmRules.evaluateDaily(sunday, eight.plusHours(1), reading))
        assertNull(AlarmRules.evaluateDaily(sunday, eight.minusHours(1), reading))
    }

    // --- content ---

    @Test
    fun `a report whose minimum is met is sent`() {
        val high = alarm(minSeverity = PollenSeverity.HIGH)

        assertNotNull(AlarmRules.evaluateDaily(high, mondayAtEight, fresh()))
    }

    @Test
    fun `a report whose minimum no selected type reaches is not sent`() {
        val veryHigh = alarm(minSeverity = PollenSeverity.VERY_HIGH)

        assertNull(AlarmRules.evaluateDaily(veryHigh, mondayAtEight, fresh()))
    }

    @Test
    fun `a report only counts its selected types against the minimum`() {
        // Grasses is HIGH but not selected; Birch is only MODERATE.
        val birchHigh = alarm(species = setOf(PollenSpecies.BIRCH), minSeverity = PollenSeverity.HIGH)

        assertNull(AlarmRules.evaluateDaily(birchHigh, mondayAtEight, fresh()))
    }

    @Test
    fun `an Any report is sent even when nothing is in the air`() {
        val quiet = reading(severities = mapOf(PollenSpecies.BIRCH to PollenSeverity.NONE))

        assertNotNull(AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh(quiet)))
    }

    @Test
    fun `the title names the station`() {
        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh())

        assertEquals("Pollen in Zürich", message?.title)
    }

    @Test
    fun `the body lists the selected types with a reading worst first`() {
        val measurement = reading(
            severities = mapOf(
                PollenSpecies.ALDER to PollenSeverity.LOW,
                PollenSpecies.BIRCH to PollenSeverity.MODERATE,
                PollenSpecies.ASH to PollenSeverity.VERY_HIGH,
                PollenSpecies.GRASSES to PollenSeverity.MODERATE,
                PollenSpecies.OAK to PollenSeverity.VERY_HIGH,
            ),
        )
        // Hazel is selected but has no reading; Oak has a reading but is not selected.
        val selection = setOf(
            PollenSpecies.ALDER, PollenSpecies.BIRCH, PollenSpecies.HAZEL, PollenSpecies.ASH, PollenSpecies.GRASSES,
        )

        val message = AlarmRules.evaluateDaily(alarm(species = selection), mondayAtEight, fresh(measurement))

        assertEquals("Ash: Very high · Birch: Moderate · Grasses: Moderate · Alder: Low", message?.body)
    }

    @Test
    fun `selected types that are all at none give the no pollen body`() {
        val quiet = reading(
            severities = mapOf(PollenSpecies.BIRCH to PollenSeverity.NONE, PollenSpecies.GRASSES to PollenSeverity.NONE),
        )

        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh(quiet))

        assertEquals("No pollen of your selected types.", message?.body)
    }

    @Test
    fun `selected types the station does not report give the no reading body`() {
        val noBirchOrGrasses = reading(severities = mapOf(PollenSpecies.HAZEL to PollenSeverity.HIGH))

        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh(noBirchOrGrasses))

        assertEquals("No reading for your selected types.", message?.body)
    }

    @Test
    fun `the message goes on the daily report channel with the station and alarm as data`() {
        val message = assertNotNull(AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh()))

        assertEquals(PushChannel.DAILY_REPORT, message.channel)
        assertEquals(mapOf("stationAbbr" to "PZH", "alarmId" to "alarm-1"), message.data)
    }

    // --- staleness ---

    @Test
    fun `a reading just under three hours old is current`() {
        val almostStale = reading(measuredAt = mondayAtEight.minus(READING_STALE_AFTER).plusSeconds(1).toInstant())

        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh(almostStale))

        assertEquals("Grasses: High · Birch: Moderate", message?.body)
    }

    @Test
    fun `a reading three hours old gives an Any report the no current reading text with its time`() {
        val stale = reading(measuredAt = mondayAtEight.minusHours(3).toInstant())

        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, fresh(stale))

        assertEquals("Pollen in Zürich", message?.title)
        assertEquals("No current reading for Zürich. Latest from 05:00.", message?.body)
    }

    @Test
    fun `a retained reading from yesterday is named as yesterday`() {
        val yesterday = reading(measuredAt = mondayAtEight.minusDays(1).withHour(22).toInstant())

        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, CacheResult.Stale(yesterday))

        assertEquals("No current reading for Zürich. Latest from 22:00 yesterday.", message?.body)
    }

    @Test
    fun `a reading from before midnight is not current even when it is under three hours old`() {
        val justAfterMidnight = mondayAtEight.withHour(0).withMinute(30)
        val beforeMidnight = reading(measuredAt = justAfterMidnight.minusHours(1).toInstant())

        val message = AlarmRules.evaluateDaily(
            alarm(at = LocalTime.of(0, 30)),
            justAfterMidnight,
            fresh(beforeMidnight),
        )

        assertEquals("No current reading for Zürich. Latest from 23:30 yesterday.", message?.body)
    }

    @Test
    fun `a retained reading older than yesterday is named by its date`() {
        val lastWeek = reading(measuredAt = Instant.parse("2026-07-29T07:00:00Z"))

        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, CacheResult.Stale(lastWeek))

        assertEquals("No current reading for Zürich. Latest from 29 July.", message?.body)
    }

    @Test
    fun `a report with a minimum is not sent on a stale reading`() {
        // Grasses is HIGH, so the minimum would be met on a current reading.
        val stale = reading(measuredAt = mondayAtEight.minusHours(3).toInstant())

        assertNull(AlarmRules.evaluateDaily(alarm(minSeverity = PollenSeverity.HIGH), mondayAtEight, fresh(stale)))
    }

    @Test
    fun `a failed reading gives an Any report the unavailable text`() {
        val failed = CacheResult.Failed(IOException("upstream down"))

        val message = AlarmRules.evaluateDaily(alarm(), mondayAtEight, failed)

        assertEquals("No current reading for Zürich. Readings are currently unavailable.", message?.body)
    }

    @Test
    fun `a report with a minimum is not sent on a failed reading`() {
        val failed = CacheResult.Failed(IOException("upstream down"))

        assertNull(AlarmRules.evaluateDaily(alarm(minSeverity = PollenSeverity.LOW), mondayAtEight, failed))
    }

    @Test
    fun `the freshness limit matches the app's three hours`() {
        assertEquals(Duration.ofHours(3), READING_STALE_AFTER)
    }

    // --- threshold alerts ---

    private fun thresholdAlarm(
        enabled: Boolean = true,
        species: Set<PollenSpecies> = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES),
        minSeverity: PollenSeverity = PollenSeverity.HIGH,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        from: LocalTime = LocalTime.of(7, 0),
        until: LocalTime = LocalTime.of(21, 0),
    ) = alarm(enabled = enabled, species = species, minSeverity = minSeverity, days = days)
        .copy(schedule = AlarmSchedule.Threshold(from, until))

    /** Birch HIGH, Grasses VERY_HIGH, measured half an hour before [at]. */
    private fun highReading(at: ZonedDateTime = mondayAtEight) = reading(
        measuredAt = at.minusMinutes(30).toInstant(),
        severities = mapOf(PollenSpecies.BIRCH to PollenSeverity.HIGH, PollenSpecies.GRASSES to PollenSeverity.VERY_HIGH),
    )

    private fun evaluateThreshold(
        alarm: Alarm = thresholdAlarm(),
        now: ZonedDateTime = mondayAtEight,
        reading: StationMeasurement? = highReading(now),
        notifiedToday: Set<PollenSpecies> = emptySet(),
    ) = AlarmRules.evaluateThreshold(alarm, now, reading, notifiedToday)

    @Test
    fun `a threshold alert fires at the first minute of its window`() {
        val opening = mondayAtEight.withHour(7)

        assertNotNull(evaluateThreshold(now = opening))
    }

    @Test
    fun `a threshold alert does not fire one minute before its window`() {
        assertNull(evaluateThreshold(now = mondayAtEight.withHour(6).withMinute(59)))
    }

    @Test
    fun `a threshold alert fires in the last minute of its window`() {
        assertNotNull(evaluateThreshold(now = mondayAtEight.withHour(20).withMinute(59).withSecond(59)))
    }

    @Test
    fun `a threshold alert does not fire at the end of its window`() {
        assertNull(evaluateThreshold(now = mondayAtEight.withHour(21)))
    }

    @Test
    fun `a threshold alert does not fire on a day it excludes`() {
        assertNull(evaluateThreshold(alarm = thresholdAlarm(days = setOf(DayOfWeek.SUNDAY))))
    }

    @Test
    fun `a disabled threshold alert never fires`() {
        assertNull(evaluateThreshold(alarm = thresholdAlarm(enabled = false)))
    }

    @Test
    fun `a daily report is never treated as a threshold alert`() {
        assertNull(evaluateThreshold(alarm = alarm(minSeverity = PollenSeverity.HIGH)))
    }

    @Test
    fun `a type exactly at the threshold fires`() {
        val outcome = evaluateThreshold(alarm = thresholdAlarm(species = setOf(PollenSpecies.BIRCH)))

        assertEquals(setOf(PollenSpecies.BIRCH), outcome?.species)
    }

    @Test
    fun `a type one band below the threshold does not fire`() {
        val reading = reading(severities = mapOf(PollenSpecies.BIRCH to PollenSeverity.MODERATE))

        assertNull(evaluateThreshold(alarm = thresholdAlarm(species = setOf(PollenSpecies.BIRCH)), reading = reading))
    }

    @Test
    fun `only selected types are considered`() {
        val outcome = evaluateThreshold(alarm = thresholdAlarm(species = setOf(PollenSpecies.GRASSES)))

        assertEquals(setOf(PollenSpecies.GRASSES), outcome?.species)
    }

    @Test
    fun `a selected type the station does not report is ignored`() {
        val alarm = thresholdAlarm(species = setOf(PollenSpecies.ASH, PollenSpecies.BIRCH))

        assertEquals(setOf(PollenSpecies.BIRCH), evaluateThreshold(alarm = alarm)?.species)
        assertNull(evaluateThreshold(alarm = thresholdAlarm(species = setOf(PollenSpecies.ASH))))
    }

    @Test
    fun `a type already notified today is excluded`() {
        val outcome = evaluateThreshold(notifiedToday = setOf(PollenSpecies.BIRCH))

        assertEquals(setOf(PollenSpecies.GRASSES), outcome?.species)
        assertEquals("Grasses: Very high", outcome?.message?.body)
    }

    @Test
    fun `nothing fires once every qualifying type has notified today`() {
        assertNull(evaluateThreshold(notifiedToday = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES)))
    }

    @Test
    fun `two qualifying types give one message naming both worst first`() {
        val outcome = assertNotNull(evaluateThreshold())

        assertEquals(setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES), outcome.species)
        assertEquals("Grasses: Very high · Birch: High", outcome.message.body)
    }

    @Test
    fun `the alert names the station and goes on the threshold alert channel`() {
        val message = assertNotNull(evaluateThreshold()).message

        assertEquals("Pollen in Zürich", message.title)
        assertEquals(PushChannel.THRESHOLD_ALERT, message.channel)
        assertEquals(mapOf("stationAbbr" to "PZH", "alarmId" to "alarm-1"), message.data)
    }

    @Test
    fun `a reading just under three hours old fires`() {
        val reading = highReading().copy(measuredAt = mondayAtEight.minusHours(3).plusMinutes(1).toInstant())

        assertNotNull(evaluateThreshold(reading = reading))
    }

    @Test
    fun `a reading three hours old does not fire`() {
        val reading = highReading().copy(measuredAt = mondayAtEight.minusHours(3).toInstant())

        assertNull(evaluateThreshold(reading = reading))
    }

    @Test
    fun `a reading from yesterday's date does not fire even when it is under three hours old`() {
        val justAfterMidnight = mondayAtEight.withHour(0).withMinute(30)
        val alarm = thresholdAlarm(from = LocalTime.MIDNIGHT)
        val beforeMidnight = highReading().copy(measuredAt = justAfterMidnight.minusHours(1).toInstant())

        assertNull(evaluateThreshold(alarm = alarm, now = justAfterMidnight, reading = beforeMidnight))
        assertNotNull(evaluateThreshold(alarm = alarm, now = justAfterMidnight, reading = highReading(justAfterMidnight)))
    }

    @Test
    fun `no reading at all does not fire`() {
        assertNull(evaluateThreshold(reading = null))
    }

    @Test
    fun `the window is compared in Swiss time whatever zone now is given in`() {
        val fiveUtc = mondayAtEight.withZoneSameInstant(ZoneOffset.UTC).withHour(5)

        // 05:00 UTC is 07:00 in Zürich: inside the window although the UTC hour is not.
        assertNotNull(evaluateThreshold(now = fiveUtc, reading = highReading(fiveUtc)))
    }
}
