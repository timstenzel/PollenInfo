package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.species.allSpecies
import ch.stenzel.tim.polleninfo.core.ui.format.englishDates
import ch.stenzel.tim.polleninfo.core.ui.format.frenchDates
import ch.stenzel.tim.polleninfo.core.ui.format.germanDates
import ch.stenzel.tim.polleninfo.core.ui.severity.labelResource
import ch.stenzel.tim.polleninfo.feature.alarms.WEEKDAYS
import ch.stenzel.tim.polleninfo.feature.alarms.dailyAlarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.thresholdAlarm
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_minimum_any
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

class AlarmSummaryTest {

    private val speciesNames = allSpecies.associate { it.id to it.name }

    private val englishSeverities = mapOf(
        PollenSeverity.NONE to "None",
        PollenSeverity.LOW to "Low",
        PollenSeverity.MODERATE to "Moderate",
        PollenSeverity.HIGH to "High",
        PollenSeverity.VERY_HIGH to "Very high",
    )

    private val frenchSeverities = mapOf(
        PollenSeverity.NONE to "Nul",
        PollenSeverity.LOW to "Faible",
        PollenSeverity.MODERATE to "Modéré",
        PollenSeverity.HIGH to "Élevé",
        PollenSeverity.VERY_HIGH to "Très élevé",
    )

    private fun summaryOf(alarm: Alarm, speciesNames: Map<String, String>) =
        summaryOf(alarm, speciesNames, englishSeverities, englishDates)

    private fun daysSummary(days: Set<DayOfWeek>) = daysSummary(days, englishDates)

    // --- Threshold summary ---

    @Test
    fun `a threshold alert names its window days types and severity`() {
        val alarm = thresholdAlarm().copy(species = setOf("BIRCH", "GRASSES"), days = WEEKDAYS)

        assertEquals(
            "Threshold alert 07:00–21:00 · Mon–Fri · Birch, Grasses ≥ High",
            summaryOf(alarm, speciesNames),
        )
    }

    @Test
    fun `a threshold alert on every day for every type says so`() {
        val alarm = thresholdAlarm().copy(
            species = speciesNames.keys,
            minSeverity = PollenSeverity.LOW,
            schedule = AlarmSchedule.Threshold(LocalTime(6, 5), LocalTime(9, 30)),
        )

        assertEquals("Threshold alert 06:05–09:30 · Every day · All pollen types ≥ Low", summaryOf(alarm, speciesNames))
    }

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

    @Test
    fun `days are named in the app's language`() {
        assertEquals("Mo–Fr", daysSummary(WEEKDAYS, germanDates))
        assertEquals("Sa, So", daysSummary(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), germanDates))
    }

    @Test
    fun `a French summary uses French weekdays and severity words with 24-hour times`() {
        val alarm = thresholdAlarm().copy(
            species = setOf("BIRCH", "GRASSES"),
            days = WEEKDAYS,
            schedule = AlarmSchedule.Threshold(LocalTime(7, 0), LocalTime(21, 30)),
        )
        val names = mapOf("BIRCH" to "Bouleau", "GRASSES" to "Graminées", "OAK" to "Chêne")

        assertEquals(
            "Threshold alert 07:00–21:30 · lun–ven · Bouleau, Graminées ≥ Élevé",
            summaryOf(alarm, names, frenchSeverities, frenchDates),
        )
    }

    // --- Severity wording ---

    @Test
    fun `the minimum NONE is called Any and the others use the shared severity words`() {
        assertEquals(Res.string.alarm_minimum_any, PollenSeverity.NONE.minimumLabelResource())
        (PollenSeverity.entries - PollenSeverity.NONE).forEach { severity ->
            assertEquals(severity.labelResource(), severity.minimumLabelResource(), severity.name)
        }
    }
}
