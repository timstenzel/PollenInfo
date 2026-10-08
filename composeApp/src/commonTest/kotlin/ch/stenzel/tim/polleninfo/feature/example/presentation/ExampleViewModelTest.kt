package ch.stenzel.tim.polleninfo.feature.example.presentation

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.example.FakeExampleRepository
import ch.stenzel.tim.polleninfo.feature.example.domain.usecase.GetPollenSnapshotUseCase
import ch.stenzel.tim.polleninfo.feature.example.pollenSnapshot
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleViewModelTest {

    private val repository = FakeExampleRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ExampleViewModel(GetPollenSnapshotUseCase(repository))

    @Test
    fun `starts in Loading before the first result arrives`() = runTest {
        val viewModel = viewModel()

        assertIs<ExampleUiState.Loading>(viewModel.uiState.value)
    }

    @Test
    fun `loads the snapshot for Zurich on init and emits Content`() = runTest {
        val snapshot = pollenSnapshot(location = "Zürich")
        repository.result = Result.Success(snapshot)

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<ExampleUiState.Content>(state)
        assertEquals(snapshot, state.snapshot)
        assertTrue(!state.isRefreshing)
        assertEquals(listOf(47.3769 to 8.5417), repository.calls)
    }

    @Test
    fun `emits Error with the exception message when loading fails`() = runTest {
        repository.result = Result.Failure(RuntimeException("no network"))

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<ExampleUiState.Error>(state)
        assertEquals("no network", state.message)
    }

    @Test
    fun `leaves the message empty for the screen to word when the exception has none`() = runTest {
        repository.result = Result.Failure(RuntimeException())

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<ExampleUiState.Error>(state)
        assertNull(state.message)
    }

    @Test
    fun `refreshing from Content keeps the current data visible and flags isRefreshing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val initial = assertIs<ExampleUiState.Content>(viewModel.uiState.value)

        viewModel.loadSnapshot()

        val refreshing = assertIs<ExampleUiState.Content>(viewModel.uiState.value)
        assertTrue(refreshing.isRefreshing, "isRefreshing should be set while a refresh is running")
        assertEquals(initial.snapshot, refreshing.snapshot, "previous data must stay on screen")
    }

    @Test
    fun `a completed refresh clears isRefreshing and swaps in the new data`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val updated = pollenSnapshot(location = "Bern", grassValue = 250f)
        repository.result = Result.Success(updated)
        viewModel.loadSnapshot()
        advanceUntilIdle()

        val state = assertIs<ExampleUiState.Content>(viewModel.uiState.value)
        assertEquals(updated, state.snapshot)
        assertTrue(!state.isRefreshing)
    }

    @Test
    fun `loadSnapshot targets the coordinates it is given`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.loadSnapshot(latitude = 46.0037, longitude = 8.9506)
        advanceUntilIdle()

        assertEquals(46.0037 to 8.9506, repository.calls.last())
    }

    @Test
    fun `retry re-requests the snapshot and recovers from Error`() = runTest {
        repository.result = Result.Failure(RuntimeException("no network"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertIs<ExampleUiState.Error>(viewModel.uiState.value)

        val snapshot = pollenSnapshot()
        repository.result = Result.Success(snapshot)
        viewModel.retry()
        advanceUntilIdle()

        val state = assertIs<ExampleUiState.Content>(viewModel.uiState.value)
        assertEquals(snapshot, state.snapshot)
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun `retrying from Error goes back through Loading`() = runTest {
        repository.result = Result.Failure(RuntimeException("no network"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.retry()

        assertIs<ExampleUiState.Loading>(viewModel.uiState.value)
    }

    @Test
    fun `a failed refresh replaces the displayed content with Error`() = runTest {
        // Documents current behaviour: an unsuccessful pull-to-refresh discards the data
        // that was already on screen.
        val viewModel = viewModel()
        advanceUntilIdle()
        assertIs<ExampleUiState.Content>(viewModel.uiState.value)

        repository.result = Result.Failure(RuntimeException("refresh failed"))
        viewModel.loadSnapshot()
        advanceUntilIdle()

        assertIs<ExampleUiState.Error>(viewModel.uiState.value)
    }
}
