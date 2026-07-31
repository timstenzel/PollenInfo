package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.onboarding.FakeStationRepository
import ch.stenzel.tim.polleninfo.feature.onboarding.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.feature.onboarding.station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val repository = FakeStationRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OnboardingViewModel(repository)

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
}
