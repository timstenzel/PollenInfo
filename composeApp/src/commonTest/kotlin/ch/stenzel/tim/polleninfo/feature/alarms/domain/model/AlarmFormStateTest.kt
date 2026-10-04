package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.species.allSpecies
import ch.stenzel.tim.polleninfo.feature.alarms.dailyAlarm
import ch.stenzel.tim.polleninfo.feature.alarms.thresholdAlarm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

class AlarmFormStateTest {

    private val speciesIds = allSpecies.map { it.id }

    private val newForm = AlarmFormState.newDailyReport(homeStationAbbr = "PZH", speciesIds = speciesIds)

    // --- Defaults ---

    @Test
    fun `a new daily report preselects the home station`() {
        assertEquals("PZH", newForm.stationAbbr)
    }

    @Test
    fun `a new daily report selects every pollen type and every day`() {
        assertEquals(speciesIds.toSet(), newForm.species)
        assertEquals(DayOfWeek.entries.toSet(), newForm.days)
    }

    @Test
    fun `a new daily report is sent at 08 00 whatever the levels`() {
        assertEquals(AlarmSchedule.Daily(LocalTime(8, 0)), newForm.schedule)
        assertEquals(PollenSeverity.NONE, newForm.minSeverity)
    }

    @Test
    fun `a new daily report is valid and not dirty`() {
        assertTrue(newForm.isValid)
        assertFalse(newForm.isDirty)
    }

    @Test
    fun `a daily report offers Any and every band as its minimum`() {
        assertEquals(PollenSeverity.entries, newForm.severityOptions)
    }

    // --- Validity ---

    @Test
    fun `isValid is false with no pollen types`() {
        val form = speciesIds.fold(newForm) { form, id -> form.toggleSpecies(id) }

        assertTrue(form.species.isEmpty())
        assertFalse(form.isValid)
    }

    @Test
    fun `a single pollen type is enough`() {
        val form = speciesIds.drop(1).fold(newForm) { form, id -> form.toggleSpecies(id) }

        assertEquals(setOf("ALDER"), form.species)
        assertTrue(form.isValid)
    }

    @Test
    fun `isValid is false with no days`() {
        val form = DayOfWeek.entries.fold(newForm) { form, day -> form.toggleDay(day) }

        assertTrue(form.days.isEmpty())
        assertFalse(form.isValid)
    }

    @Test
    fun `a single day is enough`() {
        val form = DayOfWeek.entries.drop(1).fold(newForm) { form, day -> form.toggleDay(day) }

        assertEquals(setOf(DayOfWeek.MONDAY), form.days)
        assertTrue(form.isValid)
    }

    // --- Dirtiness ---

    @Test
    fun `each kind of change makes the form dirty`() {
        listOf(
            newForm.withStation("PBE"),
            newForm.toggleSpecies("BIRCH"),
            newForm.withMinSeverity(PollenSeverity.HIGH),
            newForm.toggleDay(DayOfWeek.SUNDAY),
            newForm.withTime(LocalTime(7, 30)),
        ).forEach { assertTrue(it.isDirty, "$it should be dirty") }
    }

    @Test
    fun `isDirty is false again after a change is undone`() {
        val undone = newForm
            .toggleSpecies("BIRCH").toggleSpecies("BIRCH")
            .toggleDay(DayOfWeek.SUNDAY).toggleDay(DayOfWeek.SUNDAY)
            .withStation("PBE").withStation("PZH")
            .withMinSeverity(PollenSeverity.HIGH).withMinSeverity(PollenSeverity.NONE)
            .withTime(LocalTime(7, 30)).withTime(LocalTime(8, 0))

        assertFalse(undone.isDirty)
    }

    // --- Draft ---

    @Test
    fun `toDraft carries every field`() {
        val form = newForm
            .withStation("PBE")
            .toggleSpecies("ALDER")
            .withMinSeverity(PollenSeverity.MODERATE)
            .toggleDay(DayOfWeek.SATURDAY)
            .toggleDay(DayOfWeek.SUNDAY)
            .withTime(LocalTime(6, 45))

        assertEquals(
            AlarmDraft(
                enabled = true,
                stationAbbr = "PBE",
                species = speciesIds.toSet() - "ALDER",
                minSeverity = PollenSeverity.MODERATE,
                days = DayOfWeek.entries.toSet() - DayOfWeek.SATURDAY - DayOfWeek.SUNDAY,
                schedule = AlarmSchedule.Daily(LocalTime(6, 45)),
            ),
            form.toDraft(),
        )
    }

    // --- Type ---

    private val threshold = newForm.withType(AlarmType.THRESHOLD)

    @Test
    fun `a new alarm is a daily report`() {
        assertEquals(AlarmType.DAILY, newForm.type)
    }

    @Test
    fun `switching to threshold sets High and 07 00 to 21 00`() {
        assertEquals(AlarmType.THRESHOLD, threshold.type)
        assertEquals(PollenSeverity.HIGH, threshold.minSeverity)
        assertEquals(AlarmSchedule.Threshold(LocalTime(7, 0), LocalTime(21, 0)), threshold.schedule)
        assertTrue(threshold.isValid)
    }

    @Test
    fun `switching back to daily sets Any and 08 00`() {
        val daily = newForm
            .withMinSeverity(PollenSeverity.LOW)
            .withTime(LocalTime(6, 15))
            .withType(AlarmType.THRESHOLD)
            .withType(AlarmType.DAILY)

        assertEquals(AlarmType.DAILY, daily.type)
        assertEquals(PollenSeverity.NONE, daily.minSeverity)
        assertEquals(AlarmSchedule.Daily(LocalTime(8, 0)), daily.schedule)
    }

    @Test
    fun `switching type keeps station pollen types and days`() {
        val edited = newForm.withStation("PBE").toggleSpecies("BIRCH").toggleDay(DayOfWeek.SUNDAY)

        val switched = edited.withType(AlarmType.THRESHOLD)

        assertEquals("PBE", switched.stationAbbr)
        assertEquals(edited.species, switched.species)
        assertEquals(edited.days, switched.days)
    }

    @Test
    fun `choosing the current type changes nothing`() {
        val edited = newForm.withMinSeverity(PollenSeverity.LOW).withTime(LocalTime(6, 15))

        assertEquals(edited, edited.withType(AlarmType.DAILY))
    }

    @Test
    fun `switching type makes the form dirty and switching back to the defaults does not`() {
        assertTrue(threshold.isDirty)
        assertFalse(threshold.withType(AlarmType.DAILY).isDirty)
    }

    @Test
    fun `the threshold severity options exclude Any`() {
        assertEquals(
            listOf(PollenSeverity.LOW, PollenSeverity.MODERATE, PollenSeverity.HIGH, PollenSeverity.VERY_HIGH),
            threshold.severityOptions,
        )
    }

    // --- Threshold window ---

    @Test
    fun `the window start and end can be changed`() {
        val form = threshold.withWindowStart(LocalTime(6, 30)).withWindowEnd(LocalTime(22, 0))

        assertEquals(AlarmSchedule.Threshold(LocalTime(6, 30), LocalTime(22, 0)), form.schedule)
    }

    @Test
    fun `isValid is false when the end equals the start`() {
        val form = threshold.withWindowEnd(LocalTime(7, 0))

        assertFalse(form.isWindowValid)
        assertFalse(form.isValid)
    }

    @Test
    fun `isValid is false when the end is before the start`() {
        assertFalse(threshold.withWindowStart(LocalTime(22, 0)).isValid)
    }

    @Test
    fun `an end one minute after the start is valid`() {
        val form = threshold.withWindowEnd(LocalTime(7, 1))

        assertTrue(form.isWindowValid)
        assertTrue(form.isValid)
    }

    @Test
    fun `window times are ignored on a daily report and the report time on a threshold alert`() {
        assertEquals(newForm, newForm.withWindowStart(LocalTime(6, 0)).withWindowEnd(LocalTime(9, 0)))
        assertEquals(threshold, threshold.withTime(LocalTime(9, 0)))
    }

    @Test
    fun `toDraft carries the threshold window`() {
        val draft = threshold.withMinSeverity(PollenSeverity.MODERATE).withWindowEnd(LocalTime(19, 0)).toDraft()

        assertEquals(PollenSeverity.MODERATE, draft.minSeverity)
        assertEquals(AlarmSchedule.Threshold(LocalTime(7, 0), LocalTime(19, 0)), draft.schedule)
    }

    // --- Editing an existing alarm ---

    @Test
    fun `an existing alarm opens with every field as stored and is not dirty`() {
        val form = AlarmFormState.fromAlarm(thresholdAlarm())

        assertEquals(thresholdAlarm().toDraft(), form.toDraft())
        assertEquals(AlarmType.THRESHOLD, form.type)
        assertFalse(form.isDirty)
    }

    @Test
    fun `an existing alarm has its type locked and a new one does not`() {
        assertTrue(AlarmFormState.fromAlarm(dailyAlarm()).typeLocked)
        assertFalse(newForm.typeLocked)
    }

    @Test
    fun `switching type on an existing alarm changes nothing`() {
        val form = AlarmFormState.fromAlarm(dailyAlarm())

        assertEquals(form, form.withType(AlarmType.THRESHOLD))
    }

    @Test
    fun `an edit keeps the alarm paused`() {
        val form = AlarmFormState.fromAlarm(thresholdAlarm()).withWindowEnd(LocalTime(20, 0))

        assertFalse(form.toDraft().enabled)
        assertTrue(form.isDirty)
    }

    @Test
    fun `an edited alarm is not dirty after the change is undone`() {
        val form = AlarmFormState.fromAlarm(dailyAlarm()).toggleDay(DayOfWeek.MONDAY).toggleDay(DayOfWeek.MONDAY)

        assertFalse(form.isDirty)
    }
}
