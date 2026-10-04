package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.FakeSpeciesRepository
import ch.stenzel.tim.polleninfo.core.species.allSpecies
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.allStations
import ch.stenzel.tim.polleninfo.feature.alarms.FakeAlarmRepository
import ch.stenzel.tim.polleninfo.feature.alarms.dailyAlarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmFormState
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmType
import ch.stenzel.tim.polleninfo.feature.alarms.thresholdAlarm
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmEditorViewModelTest {

    private val stations = FakeStationRepository()
    private val species = FakeSpeciesRepository()
    private val home = FakeSelectedStationRepository(initial = SelectedStation("PZH", "Zürich"))
    private val alarms = FakeAlarmRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(alarmId: String? = null) = AlarmEditorViewModel(alarmId, stations, species, home, alarms)

    private fun TestScope.loadedViewModel(): AlarmEditorViewModel = viewModel().also { advanceUntilIdle() }

    private fun AlarmEditorViewModel.editing() = assertIs<AlarmEditorUiState.Editing>(uiState.value)

    /** Collects every event the ViewModel sends for the rest of the test. */
    private fun TestScope.collectEvents(viewModel: AlarmEditorViewModel): List<AlarmEditorEvent> {
        val events = mutableListOf<AlarmEditorEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    // --- Loading ---

    @Test
    fun `starts in Loading`() {
        assertEquals(AlarmEditorUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `opens a new daily report on the home station with the loaded choices`() = runTest {
        val editing = loadedViewModel().editing()

        assertEquals(AlarmFormState.newDailyReport("PZH", allSpecies.map { it.id }), editing.form)
        assertEquals(allStations, editing.stations)
        assertEquals(allSpecies, editing.species)
        assertFalse(editing.isSaving)
        assertNull(editing.saveError)
    }

    @Test
    fun `loads the stations and the species once each`() = runTest {
        loadedViewModel()

        assertEquals(1, stations.callCount)
        assertEquals(1, species.callCount)
    }

    @Test
    fun `a home station missing from the list falls back to the first station`() = runTest {
        home.select(SelectedStation("PXX", "Nowhere"))

        assertEquals(allStations.first().abbr, loadedViewModel().editing().form.stationAbbr)
    }

    @Test
    fun `a failing species load shows Error`() = runTest {
        species.result = Result.Failure(RuntimeException("offline"))

        assertEquals(AlarmEditorUiState.Error("offline"), loadedViewModel().uiState.value)
    }

    @Test
    fun `a failing station load shows Error`() = runTest {
        stations.result = Result.Failure(RuntimeException("offline"))

        assertEquals(AlarmEditorUiState.Error("offline"), loadedViewModel().uiState.value)
    }

    @Test
    fun `retry after a failed load opens the form`() = runTest {
        species.result = Result.Failure(RuntimeException("offline"))
        val viewModel = loadedViewModel()
        species.result = Result.Success(allSpecies)

        viewModel.retry()
        advanceUntilIdle()

        viewModel.editing()
    }

    // --- Editing ---

    @Test
    fun `changes to the fields reach the form`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onStationSelected("PBE")
        viewModel.onSpeciesToggled("BIRCH")
        viewModel.onMinSeveritySelected(PollenSeverity.HIGH)
        viewModel.onDayToggled(DayOfWeek.SUNDAY)
        viewModel.onTimeSelected(LocalTime(6, 30))

        val expected = AlarmFormState.newDailyReport("PZH", allSpecies.map { it.id })
            .withStation("PBE")
            .toggleSpecies("BIRCH")
            .withMinSeverity(PollenSeverity.HIGH)
            .toggleDay(DayOfWeek.SUNDAY)
            .withTime(LocalTime(6, 30))
        assertEquals(expected, viewModel.editing().form)
    }

    @Test
    fun `an invalid form cannot be saved`() = runTest {
        val viewModel = loadedViewModel()
        DayOfWeek.entries.forEach(viewModel::onDayToggled)

        assertFalse(viewModel.editing().canSave)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(alarms.createdDrafts.isEmpty())
    }

    // --- Saving ---

    @Test
    fun `a successful save sends the form and emits Done exactly once`() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)
        viewModel.onStationSelected("PBE")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf(viewModel.editing().form.toDraft()), alarms.createdDrafts)
        assertEquals("PBE", alarms.createdDrafts.single().stationAbbr)
        assertEquals(listOf<AlarmEditorEvent>(AlarmEditorEvent.Done), events)
    }

    @Test
    fun `saving never changes the home station`() = runTest {
        val viewModel = loadedViewModel()
        viewModel.onStationSelected("PBE")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(SelectedStation("PZH", "Zürich"), home.stored)
    }

    @Test
    fun `isSaving is observable while a save is in flight and taps meanwhile are ignored`() = runTest {
        val viewModel = loadedViewModel()
        alarms.createGate = CompletableDeferred()

        viewModel.save()
        runCurrent()
        assertTrue(viewModel.editing().isSaving)
        assertFalse(viewModel.editing().canSave)

        viewModel.save()
        viewModel.onDayToggled(DayOfWeek.MONDAY)
        runCurrent()
        assertEquals(1, alarms.createdDrafts.size)
        assertTrue(DayOfWeek.MONDAY in viewModel.editing().form.days)

        alarms.createGate!!.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `a failed save keeps the form and sets saveError`() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)
        viewModel.onSpeciesToggled("BIRCH")
        val formBefore = viewModel.editing().form
        alarms.createResult = Result.Failure(RuntimeException("Connection refused"))

        viewModel.save()
        advanceUntilIdle()

        val editing = viewModel.editing()
        assertEquals(formBefore, editing.form)
        assertEquals("Connection refused", editing.saveError)
        assertFalse(editing.isSaving)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `the save error clears when the next save starts`() = runTest {
        val viewModel = loadedViewModel()
        alarms.createResult = Result.Failure(RuntimeException("Connection refused"))
        viewModel.save()
        advanceUntilIdle()
        alarms.createResult = null
        alarms.createGate = CompletableDeferred()

        viewModel.save()
        runCurrent()

        assertNull(viewModel.editing().saveError)
        alarms.createGate!!.complete(Unit)
        advanceUntilIdle()
    }

    // --- Threshold alerts ---

    @Test
    fun `switching to a threshold alert and editing its window saves a threshold draft`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onTypeSelected(AlarmType.THRESHOLD)
        viewModel.onWindowStartSelected(LocalTime(6, 0))
        viewModel.onWindowEndSelected(LocalTime(20, 0))
        viewModel.save()
        advanceUntilIdle()

        val draft = alarms.createdDrafts.single()
        assertEquals(PollenSeverity.HIGH, draft.minSeverity)
        assertEquals(AlarmSchedule.Threshold(LocalTime(6, 0), LocalTime(20, 0)), draft.schedule)
    }

    @Test
    fun `a window whose end is not after its start cannot be saved`() = runTest {
        val viewModel = loadedViewModel()
        viewModel.onTypeSelected(AlarmType.THRESHOLD)

        viewModel.onWindowEndSelected(LocalTime(7, 0))
        assertFalse(viewModel.editing().canSave)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(alarms.createdDrafts.isEmpty())
        viewModel.onWindowEndSelected(LocalTime(7, 1))
        assertTrue(viewModel.editing().canSave)
    }

    // --- Leaving ---

    @Test
    fun `back without changes emits Done with no dialog`() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)

        viewModel.onBack()
        advanceUntilIdle()

        assertEquals(listOf<AlarmEditorEvent>(AlarmEditorEvent.Done), events)
        assertFalse(viewModel.editing().showDiscardDialog)
    }

    @Test
    fun `back with changes shows the discard dialog and emits nothing`() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)
        viewModel.onDayToggled(DayOfWeek.SUNDAY)

        viewModel.onBack()
        advanceUntilIdle()

        assertTrue(viewModel.editing().showDiscardDialog)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `discarding emits Done without saving`() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)
        viewModel.onDayToggled(DayOfWeek.SUNDAY)
        viewModel.onBack()

        viewModel.onDiscardConfirmed()
        advanceUntilIdle()

        assertEquals(listOf<AlarmEditorEvent>(AlarmEditorEvent.Done), events)
        assertEquals(emptyList(), alarms.createdDrafts)
    }

    @Test
    fun `keep editing closes the dialog and keeps the change`() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)
        viewModel.onDayToggled(DayOfWeek.SUNDAY)
        viewModel.onBack()

        viewModel.onDiscardDismissed()
        advanceUntilIdle()

        assertFalse(viewModel.editing().showDiscardDialog)
        assertFalse(DayOfWeek.SUNDAY in viewModel.editing().form.days)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `back from a failed load emits Done`() = runTest {
        stations.result = Result.Failure(RuntimeException("offline"))
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)

        viewModel.onBack()
        advanceUntilIdle()

        assertEquals(listOf<AlarmEditorEvent>(AlarmEditorEvent.Done), events)
    }

    // --- Editing an existing alarm ---

    @Test
    fun `edit mode starts in Loading and opens the alarm with its type locked`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm(), thresholdAlarm()))
        val viewModel = viewModel(alarmId = "threshold-1")
        assertEquals(AlarmEditorUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        val editing = viewModel.editing()
        assertEquals(AlarmFormState.fromAlarm(thresholdAlarm()), editing.form)
        assertTrue(editing.form.typeLocked)
        assertTrue(editing.canDelete)
        assertEquals(allStations, editing.stations)
    }

    @Test
    fun `a new alarm cannot be deleted`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onDeleteRequested()

        assertFalse(viewModel.editing().canDelete)
        assertFalse(viewModel.editing().showDeleteDialog)
    }

    @Test
    fun `the type of an existing alarm cannot be switched`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm()))
        val viewModel = viewModel(alarmId = "daily-1").also { advanceUntilIdle() }

        viewModel.onTypeSelected(AlarmType.THRESHOLD)

        assertEquals(AlarmType.DAILY, viewModel.editing().form.type)
        assertFalse(viewModel.editing().form.isDirty)
    }

    @Test
    fun `a failed edit load shows Error`() = runTest {
        alarms.alarmResult = Result.Failure(RuntimeException("This alarm no longer exists"))

        val viewModel = viewModel(alarmId = "gone").also { advanceUntilIdle() }

        assertEquals(AlarmEditorUiState.Error("This alarm no longer exists"), viewModel.uiState.value)
    }

    @Test
    fun `saving an edit updates the alarm and emits Done`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm()))
        val viewModel = viewModel(alarmId = "daily-1").also { advanceUntilIdle() }
        val events = collectEvents(viewModel)
        viewModel.onTimeSelected(LocalTime(6, 45))

        viewModel.save()
        advanceUntilIdle()

        val (id, draft) = alarms.updates.single()
        assertEquals("daily-1", id)
        assertEquals(dailyAlarm(at = LocalTime(6, 45)).toDraft(), draft)
        assertEquals(emptyList(), alarms.createdDrafts)
        assertEquals(listOf<AlarmEditorEvent>(AlarmEditorEvent.Done), events)
    }

    @Test
    fun `back with changes to an existing alarm shows the discard dialog`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm()))
        val viewModel = viewModel(alarmId = "daily-1").also { advanceUntilIdle() }
        viewModel.onStationSelected("PBE")

        viewModel.onBack()

        assertTrue(viewModel.editing().showDiscardDialog)
    }

    // --- Deleting ---

    @Test
    fun `delete asks for confirmation before deleting`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm()))
        val viewModel = viewModel(alarmId = "daily-1").also { advanceUntilIdle() }
        val events = collectEvents(viewModel)

        viewModel.onDeleteRequested()
        advanceUntilIdle()

        assertTrue(viewModel.editing().showDeleteDialog)
        assertEquals(emptyList(), alarms.deletedIds)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `confirming the delete deletes the alarm and emits Done exactly once`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm()))
        val viewModel = viewModel(alarmId = "daily-1").also { advanceUntilIdle() }
        val events = collectEvents(viewModel)
        viewModel.onDeleteRequested()

        viewModel.onDeleteConfirmed()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(listOf("daily-1"), alarms.deletedIds)
        assertEquals(listOf<AlarmEditorEvent>(AlarmEditorEvent.Done), events)
    }

    @Test
    fun `cancelling the delete keeps the editor open`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm()))
        val viewModel = viewModel(alarmId = "daily-1").also { advanceUntilIdle() }
        val events = collectEvents(viewModel)
        viewModel.onDeleteRequested()

        viewModel.onDeleteDismissed()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertFalse(viewModel.editing().showDeleteDialog)
        assertEquals(emptyList(), alarms.deletedIds)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `a failed delete stays in the editor with an error`() = runTest {
        alarms.result = Result.Success(listOf(dailyAlarm()))
        alarms.deleteResult = Result.Failure(RuntimeException("offline"))
        val viewModel = viewModel(alarmId = "daily-1").also { advanceUntilIdle() }
        val events = collectEvents(viewModel)
        viewModel.onDeleteRequested()

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        val editing = viewModel.editing()
        assertFalse(editing.isDeleting)
        assertEquals("offline", editing.saveError)
        assertEquals(emptyList(), events)
    }
}
