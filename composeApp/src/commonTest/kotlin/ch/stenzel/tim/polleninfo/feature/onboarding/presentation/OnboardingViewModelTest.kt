package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import ch.stenzel.tim.polleninfo.core.location.FakeCoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.core.station.station
import ch.stenzel.tim.polleninfo.core.stationpicker.domain.usecase.FindNearestStationUseCase
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
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

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val repository = FakeStationRepository()
    private val selectedStationRepository = FakeSelectedStationRepository()
    private val locationProvider = FakeCoarseLocationProvider()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OnboardingViewModel(
        repository,
        selectedStationRepository,
        locationProvider,
        FindNearestStationUseCase(),
    )

    // The picking itself — list, initial selection, location shortcut, timeout — is
    // StationPicker's and is pinned in StationPickerTest. These tests pin what onboarding adds.

    @Test
    fun `wraps the picker which loads the stations on init`() = runTest {
        val viewModel = viewModel()
        assertIs<StationPickerState.Loading>(viewModel.uiState.value.picker)
        advanceUntilIdle()

        val picker = assertIs<StationPickerState.Content>(viewModel.uiState.value.picker)
        assertEquals(expectedStationNamesAlphabetical, picker.stations.map { it.name })
        assertNull(picker.selected, "onboarding starts on nothing")
    }

    @Test
    fun `retry reloads a station list that failed`() = runTest {
        repository.result = Result.Failure(IOException("Connection refused"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(StationPickerState.Error(AppError.Network), viewModel.uiState.value.picker)

        repository.result = Result.Success(listOf(station()))
        viewModel.retry()
        advanceUntilIdle()

        assertIs<StationPickerState.Content>(viewModel.uiState.value.picker)
        assertEquals(2, repository.callCount)
    }

    // --- confirming and persisting ---------------------------------------------------------

    @Test
    fun `confirming without a selection persists nothing and emits nothing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)

        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(emptyList(), selectedStationRepository.writes)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `confirming stores the abbreviation and the display name of the selected station`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val geneve = viewModel.content()
            .stations.single { it.abbr == "PGE" }

        viewModel.onStationSelected(geneve)
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(SelectedStation(abbr = "PGE", name = "Genève"), selectedStationRepository.stored)
    }

    @Test
    fun `confirming emits the completion event exactly once`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)

        viewModel.onStationSelected(station())
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(listOf(OnboardingEvent.Completed), events)
    }

    @Test
    fun `a failed write emits no completion event and flags the save error`() = runTest {
        selectedStationRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)

        viewModel.onStationSelected(station())
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(emptyList(), events, "a failed write must not navigate away")
        assertTrue(viewModel.uiState.value.saveError)
        assertNull(selectedStationRepository.stored)
    }

    @Test
    fun `a failed write leaves the selection on screen so confirming again is possible`() = runTest {
        selectedStationRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)
        val bern = viewModel.content()
            .stations.single { it.abbr == "PBE" }

        viewModel.onStationSelected(bern)
        viewModel.onConfirm()
        advanceUntilIdle()
        assertEquals(bern, viewModel.content().selected)

        selectedStationRepository.failWrite = false
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(SelectedStation(abbr = "PBE", name = "Bern"), selectedStationRepository.stored)
        assertEquals(listOf(OnboardingEvent.Completed), events)
    }

    @Test
    fun `picking another station clears a previous save error`() = runTest {
        selectedStationRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()
        val stations = viewModel.content().stations

        viewModel.onStationSelected(stations.first())
        viewModel.onConfirm()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saveError)

        viewModel.onStationSelected(stations.last())
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.saveError)
    }

    @Test
    fun `confirming twice emits one event per successful write rather than replaying the first`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            val events = viewModel.collectEvents(this)

            viewModel.onStationSelected(station())
            viewModel.onConfirm()
            advanceUntilIdle()
            assertEquals(1, events.size, "one confirm must produce exactly one event")

            viewModel.onConfirm()
            advanceUntilIdle()

            assertEquals(2, events.size)
        }

    @Test
    fun `a successful lookup proposes a station without completing onboarding`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(emptyList(), events, "the shortcut proposes; only Continue may commit")
        assertNull(selectedStationRepository.stored)
    }

    @Test
    fun `picking and confirming while a lookup is outstanding persists the picked station`() = runTest {
        locationProvider.answerDelay = 5.seconds
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)
        val geneve = viewModel.content().stations.single { it.abbr == "PGE" }

        viewModel.onPermissionResult(granted = true)
        viewModel.onStationSelected(geneve)
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(SelectedStation(abbr = "PGE", name = "Genève"), selectedStationRepository.stored)
        assertEquals(listOf(OnboardingEvent.Completed), events)
    }

}

private fun OnboardingViewModel.content(): StationPickerState.Content =
    assertIs<StationPickerState.Content>(uiState.value.picker)

/**
 * Drains the one-shot event channel into a list for the duration of [scope].
 *
 * The collector is started before the action under test so nothing is missed, and it is cancelled
 * with the test scope. Asserting on list *size* is what proves "exactly once" — a state flag would
 * replay on every re-emission and show up here as duplicates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun OnboardingViewModel.collectEvents(scope: TestScope): List<OnboardingEvent> {
    val received = mutableListOf<OnboardingEvent>()
    // UnconfinedTestDispatcher so the collector is already subscribed when this returns, rather
    // than only after the next advanceUntilIdle().
    scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
        events.collect { received += it }
    }
    return received
}
