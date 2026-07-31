package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.onboarding.FakeStationRepository
import ch.stenzel.tim.polleninfo.feature.onboarding.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.feature.onboarding.station
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val repository = FakeStationRepository()
    private val selectedStationRepository = FakeSelectedStationRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OnboardingViewModel(repository, selectedStationRepository)

    @Test
    fun `starts in Loading before the station list arrives`() = runTest {
        assertIs<OnboardingUiState.Loading>(viewModel().uiState.value)
    }

    @Test
    fun `loads the stations on init and emits them alphabetically`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = assertIs<OnboardingUiState.Content>(viewModel.uiState.value)
        assertEquals(expectedStationNamesAlphabetical, state.stations.map { it.name })
        assertEquals(1, repository.callCount)
    }

    @Test
    fun `has nothing selected when the screen has just loaded`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(assertIs<OnboardingUiState.Content>(viewModel.uiState.value).selected)
    }

    @Test
    fun `emits Error with the exception message when the station list cannot be loaded`() = runTest {
        repository.result = Result.Failure(RuntimeException("Connection refused"))

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = assertIs<OnboardingUiState.Error>(viewModel.uiState.value)
        assertEquals("Connection refused", state.message)
    }

    @Test
    fun `falls back to a generic message when the exception has none`() = runTest {
        repository.result = Result.Failure(RuntimeException())

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(
            "An unexpected error occurred",
            assertIs<OnboardingUiState.Error>(viewModel.uiState.value).message,
        )
    }

    @Test
    fun `retrying from Error goes back through Loading`() = runTest {
        repository.result = Result.Failure(RuntimeException("Connection refused"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.retry()

        assertIs<OnboardingUiState.Loading>(viewModel.uiState.value)
    }

    @Test
    fun `retry re-requests the stations and recovers from Error`() = runTest {
        repository.result = Result.Failure(RuntimeException("Connection refused"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertIs<OnboardingUiState.Error>(viewModel.uiState.value)

        repository.result = Result.Success(listOf(station()))
        viewModel.retry()
        advanceUntilIdle()

        val state = assertIs<OnboardingUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("Zürich"), state.stations.map { it.name })
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `records the station the user picks`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val bern = assertIs<OnboardingUiState.Content>(viewModel.uiState.value)
            .stations.single { it.abbr == "PBE" }

        viewModel.onStationSelected(bern)

        assertEquals(bern, assertIs<OnboardingUiState.Content>(viewModel.uiState.value).selected)
    }

    @Test
    fun `picking a second station replaces the first`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val stations = assertIs<OnboardingUiState.Content>(viewModel.uiState.value).stations

        viewModel.onStationSelected(stations.first())
        viewModel.onStationSelected(stations.last())

        assertEquals(
            stations.last(),
            assertIs<OnboardingUiState.Content>(viewModel.uiState.value).selected,
        )
    }

    @Test
    fun `selecting a station leaves the offered list untouched`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = assertIs<OnboardingUiState.Content>(viewModel.uiState.value).stations

        viewModel.onStationSelected(before[3])

        assertEquals(before, assertIs<OnboardingUiState.Content>(viewModel.uiState.value).stations)
    }

    @Test
    fun `a selection made while the screen is in Error is ignored`() = runTest {
        repository.result = Result.Failure(RuntimeException("Connection refused"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected(station())

        assertIs<OnboardingUiState.Error>(viewModel.uiState.value)
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
        val geneve = assertIs<OnboardingUiState.Content>(viewModel.uiState.value)
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
        val state = assertIs<OnboardingUiState.Content>(viewModel.uiState.value)
        assertTrue(state.saveError)
        assertNull(selectedStationRepository.stored)
    }

    @Test
    fun `a failed write leaves the selection on screen so confirming again is possible`() = runTest {
        selectedStationRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()
        val events = viewModel.collectEvents(this)
        val bern = assertIs<OnboardingUiState.Content>(viewModel.uiState.value)
            .stations.single { it.abbr == "PBE" }

        viewModel.onStationSelected(bern)
        viewModel.onConfirm()
        advanceUntilIdle()
        assertEquals(bern, assertIs<OnboardingUiState.Content>(viewModel.uiState.value).selected)

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
        val stations = assertIs<OnboardingUiState.Content>(viewModel.uiState.value).stations

        viewModel.onStationSelected(stations.first())
        viewModel.onConfirm()
        advanceUntilIdle()
        assertTrue(assertIs<OnboardingUiState.Content>(viewModel.uiState.value).saveError)

        viewModel.onStationSelected(stations.last())

        assertTrue(!assertIs<OnboardingUiState.Content>(viewModel.uiState.value).saveError)
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
}

/**
 * Drains the one-shot event channel into a list for the duration of [scope].
 *
 * The collector is started before the action under test so nothing is missed, and it is cancelled
 * with the test scope. Asserting on list *size* is what proves "exactly once" — a state flag would
 * replay on every re-emission and show up here as duplicates.
 */
private fun OnboardingViewModel.collectEvents(scope: TestScope): List<OnboardingEvent> {
    val received = mutableListOf<OnboardingEvent>()
    // UnconfinedTestDispatcher so the collector is already subscribed when this returns, rather
    // than only after the next advanceUntilIdle().
    scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
        events.collect { received += it }
    }
    return received
}
