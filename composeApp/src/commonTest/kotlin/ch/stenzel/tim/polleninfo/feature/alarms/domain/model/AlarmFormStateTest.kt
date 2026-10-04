package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.species.allSpecies
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
}
