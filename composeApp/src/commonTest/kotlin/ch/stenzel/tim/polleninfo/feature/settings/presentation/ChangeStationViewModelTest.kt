package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.location.FakeCoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.stationpicker.domain.usecase.FindNearestStationUseCase
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.IOException

/**
 * What changing the default adds to the shared picker (pinned in `StationPickerTest`): starting on
 * the stored station, when Save is allowed, and what a save does.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangeStationViewModelTest {

    private val stationRepository = FakeStationRepository()
    private val selectedStationRepository =
        FakeSelectedStationRepository(initial = SelectedStation(abbr = "PZH", name = "Zürich"))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChangeStationViewModel(
        selectedStationRepository,
        stationRepository,
        FakeCoarseLocationProvider(),
        FindNearestStationUseCase(),
    )

    @Test
    fun `starts on the stored station`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("PZH", viewModel.picker().selected?.abbr)
        assertEquals("PZH", viewModel.uiState.value.storedAbbr)
    }

    @Test
    fun `cannot save while the stored station is still selected`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `can save after picking another station`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected(viewModel.station("PBE"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `picking the stored station again disables saving again`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected(viewModel.station("PBE"))
        viewModel.onStationSelected(viewModel.station("PZH"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving the unchanged station stores nothing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(emptyList(), selectedStationRepository.writes)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `cannot save again while a save is running`() = runTest {
        selectedStationRepository.writeGate = CompletableDeferred()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected(viewModel.station("PBE"))
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaving)
        assertFalse(state.canSave)

        viewModel.save()
        advanceUntilIdle()
        assertEquals(1, selectedStationRepository.writes.size, "a second tap must not write twice")
    }

    @Test
    fun `a pick made while a save is running is ignored`() = runTest {
        selectedStationRepository.writeGate = CompletableDeferred()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected(viewModel.station("PBE"))
        viewModel.save()
        viewModel.onStationSelected(viewModel.station("PGE"))
        advanceUntilIdle()

        assertEquals("PBE", viewModel.picker().selected?.abbr)
    }

    @Test
    fun `a successful save stores the new station and sends Done exactly once`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)

        viewModel.onStationSelected(viewModel.station("PBE"))
        viewModel.save()
        advanceUntilIdle()

        assertEquals(SelectedStation(abbr = "PBE", name = "Bern"), selectedStationRepository.stored)
        assertEquals(listOf(ChangeStationEvent.Done), events)
    }

    @Test
    fun `a failed save shows the save error and sends no Done`() = runTest {
        selectedStationRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)

        viewModel.onStationSelected(viewModel.station("PBE"))
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.saveError)
        assertFalse(state.isSaving)
        assertTrue(state.canSave, "the user can try again")
        assertEquals(emptyList(), events)
        assertEquals(SelectedStation(abbr = "PZH", name = "Zürich"), selectedStationRepository.stored)
    }

    @Test
    fun `picking another station clears the save error`() = runTest {
        selectedStationRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onStationSelected(viewModel.station("PBE"))
        viewModel.save()
        advanceUntilIdle()

        viewModel.onStationSelected(viewModel.station("PGE"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveError)
    }

    @Test
    fun `a failed station list is an Error and retry loads it`() = runTest {
        stationRepository.result = Result.Failure(IOException("Connection refused"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(StationPickerState.Error(AppError.Network), viewModel.uiState.value.picker)

        stationRepository.result = FakeStationRepository().result
        viewModel.retry()
        advanceUntilIdle()

        assertEquals("PZH", viewModel.picker().selected?.abbr, "still starts on the stored station")
    }

    @Test
    fun `starts on nothing when the stored station is no longer listed`() = runTest {
        selectedStationRepository.select(SelectedStation(abbr = "PXX", name = "Gone"))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(null, viewModel.picker().selected)
        assertFalse(viewModel.uiState.value.canSave)
    }
}

private fun ChangeStationViewModel.picker(): StationPickerState.Content =
    assertIs<StationPickerState.Content>(uiState.value.picker)

private fun ChangeStationViewModel.station(abbr: String) = picker().stations.single { it.abbr == abbr }

/** Drains the one-shot event channel; see `OnboardingViewModelTest.collectEvents`. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun ChangeStationViewModel.collectEvents(scope: TestScope): List<ChangeStationEvent> {
    val received = mutableListOf<ChangeStationEvent>()
    scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
        events.collect { received += it }
    }
    return received
}
