package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.species.allSpecies
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
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
import kotlin.test.assertFalse
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

    /**
     * Marker words rather than English sentences: what is pinned is which pattern is chosen and what
     * goes into its placeholders. The sentences themselves are resources in four languages.
     */
    private val wording = AlarmSummaryWording(
        dailyReport = "<daily %1\$s>",
        thresholdAlert = "<threshold %1\$s..%2\$s>",
        everyDay = "<every day>",
        allPollenTypes = "<all types>",
        paused = "<paused> %1\$s",
    )

    private fun summaryOf(alarm: Alarm, speciesNames: Map<String, String>) =
        summaryOf(alarm, speciesNames, englishSeverities, englishDates, wording)

    private fun daysSummary(days: Set<DayOfWeek>, dates: DateWording = englishDates) =
        daysSummary(days, dates, wording)

    // --- Alarm types ---

    @Test
    fun `a threshold alert fills the threshold pattern with its window`() {
        val alarm = thresholdAlarm().copy(enabled = true, species = setOf("BIRCH", "GRASSES"), days = WEEKDAYS)

        assertEquals(
            "<threshold 07:00..21:00> · Mon–Fri · Birch, Grasses ≥ High",
            summaryOf(alarm, speciesNames),
        )
    }

    @Test
    fun `a daily report fills the daily pattern with its time`() {
        assertEquals(
            "<daily 08:00> · Mon–Fri · Birch, Grasses",
            summaryOf(dailyAlarm(), speciesNames),
        )
    }

    @Test
    fun `times are 24-hour with leading zeros in both patterns`() {
        val threshold = thresholdAlarm().copy(
            enabled = true,
            schedule = AlarmSchedule.Threshold(LocalTime(6, 5), LocalTime(19, 30)),
        )

        assertEquals("<daily 18:05>", summaryOf(dailyAlarm(at = LocalTime(18, 5)), speciesNames).substringBefore(" · "))
        assertEquals("<threshold 06:05..19:30>", summaryOf(threshold, speciesNames).substringBefore(" · "))
    }

    // --- Paused ---

    @Test
    fun `a paused alarm is wrapped in the paused pattern`() {
        val alarm = dailyAlarm().copy(enabled = false)

        assertEquals("<paused> <daily 08:00> · Mon–Fri · Birch, Grasses", summaryOf(alarm, speciesNames))
    }

    @Test
    fun `an enabled alarm is not marked paused`() {
        assertFalse(summaryOf(dailyAlarm(), speciesNames).contains("<paused>"))
    }

    // --- Pollen types and severity ---

    @Test
    fun `a filtered daily report adds its minimum severity`() {
        val alarm = dailyAlarm().copy(minSeverity = PollenSeverity.VERY_HIGH)

        assertEquals("<daily 08:00> · Mon–Fri · Birch, Grasses ≥ Very high", summaryOf(alarm, speciesNames))
    }

    @Test
    fun `types are named in display order whatever order they were stored in`() {
        val alarm = dailyAlarm().copy(species = setOf("GRASSES", "ASH", "ALDER"))

        assertEquals("<daily 08:00> · Mon–Fri · Alder, Ash, Grasses", summaryOf(alarm, speciesNames))
    }

    @Test
    fun `every type selected uses the all-types word`() {
        val alarm = dailyAlarm().copy(species = speciesNames.keys, minSeverity = PollenSeverity.HIGH)

        assertEquals("<daily 08:00> · Mon–Fri · <all types> ≥ High", summaryOf(alarm, speciesNames))
    }

    @Test
    fun `a threshold alert on every day for every type uses both words`() {
        val alarm = thresholdAlarm().copy(enabled = true, species = speciesNames.keys, minSeverity = PollenSeverity.LOW)

        assertEquals("<threshold 07:00..21:00> · <every day> · <all types> ≥ Low", summaryOf(alarm, speciesNames))
    }

    @Test
    fun `a type without a known name is shown by its id`() {
        val alarm = dailyAlarm().copy(species = setOf("BIRCH", "MUGWORT"))

        assertEquals("<daily 08:00> · Mon–Fri · Birch, MUGWORT", summaryOf(alarm, speciesNames))
    }

    @Test
    fun `without species names every type is shown by its id`() {
        assertEquals("<daily 08:00> · Mon–Fri · BIRCH, GRASSES", summaryOf(dailyAlarm(), emptyMap()))
    }

    // --- Day ranges ---

    @Test
    fun `all seven days use the every-day word`() {
        assertEquals("<every day>", daysSummary(DayOfWeek.entries.toSet()))
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
            enabled = true,
            species = setOf("BIRCH", "GRASSES"),
            days = WEEKDAYS,
            schedule = AlarmSchedule.Threshold(LocalTime(7, 0), LocalTime(21, 30)),
        )
        val names = mapOf("BIRCH" to "Bouleau", "GRASSES" to "Graminées", "OAK" to "Chêne")

        assertEquals(
            "<threshold 07:00..21:30> · lun–ven · Bouleau, Graminées ≥ Élevé",
            summaryOf(alarm, names, frenchSeverities, frenchDates, wording),
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
