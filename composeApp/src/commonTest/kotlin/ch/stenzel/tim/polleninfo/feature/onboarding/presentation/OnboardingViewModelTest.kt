package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import ch.stenzel.tim.polleninfo.core.location.CoarseLocationResult
import ch.stenzel.tim.polleninfo.core.location.FakeCoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.location.LOCATION_TIMEOUT
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.onboarding.FakeStationRepository
import ch.stenzel.tim.polleninfo.feature.onboarding.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.feature.onboarding.station
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.usecase.FindNearestStationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlin.time.Duration.Companion.seconds

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

    // --- the location shortcut -------------------------------------------------------------

    @Test
    fun `a granted permission fills in the nearest station`() = runTest {
        // The fake answers with a position near Winterthur.
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.content()
        assertEquals("PZH", state.selected?.abbr)
        assertNull(state.locationError)
        assertTrue(!state.isLocating)
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
    fun `the in-progress flag is set while the lookup runs and cleared when it ends`() = runTest {
        locationProvider.answerDelay = 5.seconds
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = true)
        assertTrue(viewModel.content().isLocating, "set as soon as the lookup starts")

        advanceTimeBy(1.seconds)
        assertTrue(viewModel.content().isLocating, "and still set while the fix is outstanding")

        advanceUntilIdle()
        assertTrue(!viewModel.content().isLocating)
        assertEquals("PZH", viewModel.content().selected?.abbr)
    }

    @Test
    fun `a refused permission produces the permission error and asks for no position`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = false)
        advanceUntilIdle()

        val state = viewModel.content()
        assertEquals(LocationError.PERMISSION_DENIED, state.locationError)
        assertTrue(!state.isLocating)
        assertNull(state.selected)
        assertEquals(0, locationProvider.callCount, "a refusal must not reach the platform")
    }

    @Test
    fun `a permission revoked below the prompt produces the permission error`() = runTest {
        locationProvider.result = CoarseLocationResult.PermissionDenied
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(LocationError.PERMISSION_DENIED, viewModel.content().locationError)
    }

    @Test
    fun `an unavailable position produces the could-not-determine error`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.content()
        assertEquals(LocationError.UNAVAILABLE, state.locationError)
        assertTrue(!state.isLocating)
        assertNull(state.selected)
    }

    @Test
    fun `a lookup that never answers produces the could-not-determine error once it times out`() =
        runTest {
            // Virtual time, not a real sleep: the ten seconds pass instantly.
            locationProvider.neverAnswers = true
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onPermissionResult(granted = true)
            advanceTimeBy(LOCATION_TIMEOUT - 1.seconds)
            assertTrue(viewModel.content().isLocating, "still waiting just before the deadline")
            assertNull(viewModel.content().locationError)

            advanceUntilIdle()

            val state = viewModel.content()
            assertEquals(LocationError.UNAVAILABLE, state.locationError)
            assertTrue(!state.isLocating)
        }

    @Test
    fun `a successful lookup clears a location error left by an earlier attempt`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(LocationError.UNAVAILABLE, viewModel.content().locationError)

        locationProvider.result = CoarseLocationResult.Success(46.2000, 7.3000)
        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.content()
        assertNull(state.locationError)
        assertEquals("PSN", state.selected?.abbr)
    }

    @Test
    fun `a lookup leaves the offered station list untouched`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = viewModel.content().stations

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(before, viewModel.content().stations)
    }

    // --- recovering from a failed lookup -----------------------------------------------------

    @Test
    fun `picking a station clears the location error it replaces`() = runTest {
        locationProvider.result = CoarseLocationResult.PermissionDenied
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(LocationError.PERMISSION_DENIED, viewModel.content().locationError)

        viewModel.onStationSelected(viewModel.content().stations.first())

        val state = viewModel.content()
        assertNull(state.locationError, "the message must not outlive the pick that answers it")
        assertEquals(state.stations.first(), state.selected)
    }

    @Test
    fun `retrying the lookup clears the previous error as soon as the attempt starts`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(LocationError.UNAVAILABLE, viewModel.content().locationError)

        locationProvider.answerDelay = 5.seconds
        viewModel.onPermissionResult(granted = true)

        // Not "once the retry succeeds" — the stale message would otherwise read as this attempt
        // having already failed.
        val state = viewModel.content()
        assertNull(state.locationError)
        assertTrue(state.isLocating)
    }

    @Test
    fun `the location shortcut still works after the permission was refused`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = false)
        advanceUntilIdle()
        // Nothing about the refusal blocks another attempt: the action is bound to isLocating
        // alone, so the device may still prompt if it permits one.
        assertTrue(!viewModel.content().isLocating)

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.content()
        assertEquals(1, locationProvider.callCount)
        assertEquals("PZH", state.selected?.abbr)
        assertNull(state.locationError)
    }

    @Test
    fun `a repeated refusal leaves the permission message in place`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = false)
        viewModel.onPermissionResult(granted = false)
        advanceUntilIdle()

        assertEquals(LocationError.PERMISSION_DENIED, viewModel.content().locationError)
    }

    // --- the manual path is never overruled by a lookup ---------------------------------------

    @Test
    fun `a fix that lands after a manual pick leaves the picked station in place`() = runTest {
        // The fake would answer with a position near Winterthur — i.e. propose PZH — but only
        // after the user has already picked Bern.
        locationProvider.answerDelay = 5.seconds
        val viewModel = viewModel()
        advanceUntilIdle()
        val bern = viewModel.content().stations.single { it.abbr == "PBE" }

        viewModel.onPermissionResult(granted = true)
        advanceTimeBy(1.seconds)
        viewModel.onStationSelected(bern)
        advanceUntilIdle()

        val state = viewModel.content()
        assertEquals(bern, state.selected, "a late fix must not silently re-pick for the user")
        assertTrue(!state.isLocating)
        assertNull(state.locationError)
    }

    @Test
    fun `a manual pick stops the lookup rather than letting it run on`() = runTest {
        locationProvider.answerDelay = 5.seconds
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = true)
        advanceTimeBy(1.seconds)
        viewModel.onStationSelected(viewModel.content().stations.first())
        advanceUntilIdle()

        assertTrue(locationProvider.wasCancelled, "cancellation must reach the platform request")
    }

    @Test
    fun `a failed lookup that lands after a manual pick raises no error`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        locationProvider.answerDelay = 5.seconds
        val viewModel = viewModel()
        advanceUntilIdle()
        val sion = viewModel.content().stations.single { it.abbr == "PSN" }

        viewModel.onPermissionResult(granted = true)
        viewModel.onStationSelected(sion)
        advanceUntilIdle()

        val state = viewModel.content()
        assertNull(state.locationError, "the lookup was called off; it has nothing left to report")
        assertEquals(sion, state.selected)
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

    @Test
    fun `a permission answer arriving before the stations are loaded is ignored`() = runTest {
        repository.result = Result.Failure(RuntimeException("Connection refused"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        // There is no list to find a nearest station in, so there is nothing to say.
        assertIs<OnboardingUiState.Error>(viewModel.uiState.value)
        assertEquals(0, locationProvider.callCount)
    }
}

private fun OnboardingViewModel.content(): OnboardingUiState.Content =
    assertIs<OnboardingUiState.Content>(uiState.value)

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
