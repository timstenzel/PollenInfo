package ch.stenzel.tim.polleninfo.server.alarm.domain

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AlarmValidationTest {

    private val validDaily = AlarmInput(
        enabled = true,
        stationAbbr = "PZH",
        species = listOf("BIRCH", "GRASSES"),
        minSeverity = "NONE",
        days = listOf("MONDAY", "FRIDAY"),
        schedule = ScheduleInput.Daily("08:00"),
    )

    private fun invalidMessage(input: AlarmInput): String =
        assertIs<ValidationResult.Invalid>(AlarmValidation.validate(input)).message

    private fun valid(input: AlarmInput): AlarmSpec =
        assertIs<ValidationResult.Valid>(AlarmValidation.validate(input)).spec

    // --- Acceptance ---

    @Test
    fun `accepts a valid daily report with every field carried over`() {
        assertEquals(
            AlarmSpec(
                enabled = true,
                station = PollenStation.ZUERICH,
                species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES),
                minSeverity = PollenSeverity.NONE,
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                schedule = AlarmSchedule.Daily(LocalTime.of(8, 0)),
            ),
            valid(validDaily),
        )
    }

    @Test
    fun `a paused alarm stays paused`() {
        assertEquals(false, valid(validDaily.copy(enabled = false)).enabled)
    }

    @Test
    fun `matches the station abbreviation case-insensitively`() {
        assertEquals(PollenStation.BERN, valid(validDaily.copy(stationAbbr = "pbe")).station)
        assertEquals(PollenStation.BERN, valid(validDaily.copy(stationAbbr = "Pbe")).station)
    }

    @Test
    fun `accepts every severity as the minimum of a daily report`() {
        PollenSeverity.entries.forEach { severity ->
            assertEquals(severity, valid(validDaily.copy(minSeverity = severity.name)).minSeverity)
        }
    }

    @Test
    fun `a single species and a single day are enough`() {
        val spec = valid(validDaily.copy(species = listOf("ASH"), days = listOf("SUNDAY")))

        assertEquals(setOf(PollenSpecies.ASH), spec.species)
        assertEquals(setOf(DayOfWeek.SUNDAY), spec.days)
    }

    @Test
    fun `repeated species and days collapse into one each`() {
        val spec = valid(validDaily.copy(species = listOf("ASH", "ASH"), days = listOf("MONDAY", "MONDAY")))

        assertEquals(setOf(PollenSpecies.ASH), spec.species)
        assertEquals(setOf(DayOfWeek.MONDAY), spec.days)
    }

    @Test
    fun `accepts the first and the last minute of the day`() {
        assertEquals(
            AlarmSchedule.Daily(LocalTime.MIDNIGHT),
            valid(validDaily.copy(schedule = ScheduleInput.Daily("00:00"))).schedule,
        )
        assertEquals(
            AlarmSchedule.Daily(LocalTime.of(23, 59)),
            valid(validDaily.copy(schedule = ScheduleInput.Daily("23:59"))).schedule,
        )
    }

    // --- Rejection ---

    @Test
    fun `rejects empty species`() {
        assertEquals("Select at least one pollen type", invalidMessage(validDaily.copy(species = emptyList())))
    }

    @Test
    fun `rejects an unknown species and names it`() {
        assertTrue("MUGWORT" in invalidMessage(validDaily.copy(species = listOf("BIRCH", "MUGWORT"))))
    }

    @Test
    fun `rejects species names in the wrong case`() {
        assertIs<ValidationResult.Invalid>(AlarmValidation.validate(validDaily.copy(species = listOf("birch"))))
    }

    @Test
    fun `rejects empty days`() {
        assertEquals("Select at least one day", invalidMessage(validDaily.copy(days = emptyList())))
    }

    @Test
    fun `rejects an unknown day and names it`() {
        assertTrue("MON" in invalidMessage(validDaily.copy(days = listOf("MON"))))
    }

    @Test
    fun `rejects an unknown station and names it`() {
        assertTrue("PXX" in invalidMessage(validDaily.copy(stationAbbr = "PXX")))
    }

    @Test
    fun `rejects an unknown severity`() {
        assertTrue("EXTREME" in invalidMessage(validDaily.copy(minSeverity = "EXTREME")))
    }

    @Test
    fun `rejects a malformed time`() {
        listOf("8:00", "08:00:00", "0800", "08:60", "24:00", "25:00", "", "eight").forEach { time ->
            val result = AlarmValidation.validate(validDaily.copy(schedule = ScheduleInput.Daily(time)))
            assertIs<ValidationResult.Invalid>(result, "'$time' should be rejected")
        }
    }

    @Test
    fun `rejects threshold alerts until they can be created`() {
        val input = validDaily.copy(minSeverity = "HIGH", schedule = ScheduleInput.Threshold("07:00", "21:00"))

        assertIs<ValidationResult.Invalid>(AlarmValidation.validate(input))
    }
}
