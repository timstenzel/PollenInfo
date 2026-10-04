package ch.stenzel.tim.polleninfo.feature.diary.presentation

import ch.stenzel.tim.polleninfo.core.history.FakeStationHistoryRepository
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.stationHistory
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {

    private val zurich = SelectedStation(abbr = "PZH", name = "Zürich")

    private val selectedStationRepository = FakeSelectedStationRepository(initial = zurich)
    private val historyRepository = FakeStationHistoryRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DiaryViewModel(selectedStationRepository, historyRepository)

    @Test
    fun `starts in Loading`() = runTest {
        val viewModel = viewModel()

        assertIs<DiaryUiState.Loading>(viewModel.uiState.value)
    }

    @Test
    fun `stays in Loading while the history request is in flight`() = runTest {
        historyRepository.gate = CompletableDeferred()
        val viewModel = viewModel()

        runCurrent()

        assertIs<DiaryUiState.Loading>(viewModel.uiState.value)
        historyRepository.gate?.complete(Unit)
        advanceUntilIdle()
        assertIs<DiaryUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun `opens on the stored home station over the last month`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertEquals("PZH", content.stationAbbr)
        assertEquals("Zürich", content.stationName)
        assertEquals(HistoryRange.MONTH, content.range)
        assertEquals(listOf("PZH" to HistoryRange.MONTH), historyRepository.requested)
        assertFalse(content.isLoading)
    }

    @Test
    fun `content carries the history and draws every species it reports in order`() = runTest {
        val history = stationHistory()
        historyRepository.result = Result.Success(history)
        val viewModel = viewModel()

        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertEquals(history, content.history)
        assertEquals(listOf("BIRCH", "GRASSES", "ASH"), content.speciesIds)
    }

    @Test
    fun `never writes the home station`() = runTest {
        viewModel()

        advanceUntilIdle()

        assertTrue(selectedStationRepository.writes.isEmpty())
    }

    @Test
    fun `a failed history request is Error with its message`() = runTest {
        historyRepository.result = Result.Failure(RuntimeException("backend unreachable"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(DiaryUiState.Error("backend unreachable"), viewModel.uiState.value)
    }

    @Test
    fun `retry from Error reaches Content once the backend answers`() = runTest {
        historyRepository.result = Result.Failure(RuntimeException("backend unreachable"))
        val viewModel = viewModel()
        advanceUntilIdle()

        historyRepository.result = Result.Success(stationHistory())
        viewModel.retry()
        assertIs<DiaryUiState.Loading>(viewModel.uiState.value)
        advanceUntilIdle()

        assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("PZH" to HistoryRange.MONTH, "PZH" to HistoryRange.MONTH), historyRepository.requested)
    }

    @Test
    fun `no stored station is an error rather than an endless spinner`() = runTest {
        val viewModel = DiaryViewModel(FakeSelectedStationRepository(initial = null), historyRepository)

        advanceUntilIdle()

        assertIs<DiaryUiState.Error>(viewModel.uiState.value)
        assertTrue(historyRepository.requested.isEmpty())
    }
}
