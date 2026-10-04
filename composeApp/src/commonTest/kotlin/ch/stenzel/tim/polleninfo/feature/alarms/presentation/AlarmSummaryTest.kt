package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.species.allSpecies
import ch.stenzel.tim.polleninfo.feature.alarms.WEEKDAYS
import ch.stenzel.tim.polleninfo.feature.alarms.dailyAlarm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

class AlarmSummaryTest {

    private val speciesNames = allSpecies.associate { it.id to it.name }

    // --- Daily summary ---

    @Test
    fun `an Any daily report names its time days and types`() {
        assertEquals(
            "Daily report at 08:00 · Mon–Fri · Birch, Grasses",
            summaryOf(dailyAlarm(), speciesNames),
        )
    }

    @Test
    fun `a filtered daily report adds its minimum severity`() {
        val alarm = dailyAlarm().copy(minSeverity = PollenSeverity.VERY_HIGH)

        assertEquals(
            "Daily report at 08:00 · Mon–Fri · Birch, Grasses ≥ Very high",
            summaryOf(alarm, speciesNames),
        )
    }

    @Test
    fun `types are named in display order whatever order they were stored in`() {
        val alarm = dailyAlarm().copy(species = setOf("GRASSES", "ASH", "ALDER"))

        assertEquals(
            "Daily report at 08:00 · Mon–Fri · Alder, Ash, Grasses",
            summaryOf(alarm, speciesNames),
        )
    }

    @Test
    fun `every type selected reads All pollen types`() {
        val alarm = dailyAlarm().copy(species = speciesNames.keys, minSeverity = PollenSeverity.HIGH)

        assertEquals(
            "Daily report at 08:00 · Mon–Fri · All pollen types ≥ High",
            summaryOf(alarm, speciesNames),
        )
    }

    @Test
    fun `a type without a known name is shown by its id`() {
        val alarm = dailyAlarm().copy(species = setOf("BIRCH", "MUGWORT"))

        assertEquals(
            "Daily report at 08:00 · Mon–Fri · Birch, MUGWORT",
            summaryOf(alarm, speciesNames),
        )
    }

    @Test
    fun `without species names every type is shown by its id`() {
        assertEquals(
            "Daily report at 08:00 · Mon–Fri · BIRCH, GRASSES",
            summaryOf(dailyAlarm(), emptyMap()),
        )
    }

    @Test
    fun `times are written with two digit hours and minutes`() {
        assertEquals("06:05", formatTime(LocalTime(6, 5)))
        assertEquals("00:00", formatTime(LocalTime(0, 0)))
        assertEquals("23:59", formatTime(LocalTime(23, 59)))
    }

    // --- Day ranges ---

    @Test
    fun `all seven days read Every day`() {
        assertEquals("Every day", daysSummary(DayOfWeek.entries.toSet()))
    }

    @Test
    fun `the working week collapses to Mon–Fri`() {
        assertEquals("Mon–Fri", daysSummary(WEEKDAYS))
    }

    @Test
    fun `the weekend is listed as Sat and Sun`() {
        assertEquals("Sat, Sun", daysSummary(setOf(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)))
    }

    @Test
    fun `a non-contiguous set is listed in week order`() {
        assertEquals("Mon, Wed, Fri", daysSummary(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
    }

    @Test
    fun `three consecutive days are a range and two are not`() {
        assertEquals("Mon–Wed", daysSummary(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)))
        assertEquals("Mon, Tue", daysSummary(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)))
    }

    @Test
    fun `ranges and single days combine`() {
        assertEquals("Mon–Thu, Sat", daysSummary(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)))
    }

    @Test
    fun `six days without Sunday collapse to Mon–Sat`() {
        assertEquals("Mon–Sat", daysSummary(DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY))
    }

    @Test
    fun `a single day is its abbreviation`() {
        assertEquals("Thu", daysSummary(setOf(DayOfWeek.THURSDAY)))
    }

    // --- Severity wording ---

    @Test
    fun `the minimum NONE is called Any and the others use the shared severity words`() {
        assertEquals(
            listOf("Any", "Low", "Moderate", "High", "Very high"),
            PollenSeverity.entries.map { it.minimumLabel() },
        )
    }
}
