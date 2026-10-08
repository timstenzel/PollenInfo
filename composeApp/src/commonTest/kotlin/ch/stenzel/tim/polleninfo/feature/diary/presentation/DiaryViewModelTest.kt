package ch.stenzel.tim.polleninfo.feature.diary.presentation

import ch.stenzel.tim.polleninfo.core.diary.FakeDiaryRepository
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.history.FakeStationHistoryRepository
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.stationHistory
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.FakeSpeciesRepository
import ch.stenzel.tim.polleninfo.core.species.allSpecies
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.allStations
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {

    private val zurich = SelectedStation(abbr = "PZH", name = "Zürich")

    private val selectedStationRepository = FakeSelectedStationRepository(initial = zurich)
    private val stationRepository = FakeStationRepository()
    private val speciesRepository = FakeSpeciesRepository()
    private val historyRepository = FakeStationHistoryRepository()
    private val diaryRepository = FakeDiaryRepository()

    /**
     * 4 October 2026, 00:30 in Zürich but still 3 October in UTC — so "today" must be the Swiss date.
     * The default `stationHistory()` covers 4 September to 3 October: the month ending yesterday.
     */
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-03T22:30:00Z")
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(selected: FakeSelectedStationRepository = selectedStationRepository) =
        DiaryViewModel(selected, stationRepository, speciesRepository, historyRepository, diaryRepository, clock)

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
        assertEquals(allStations, content.stations)
        assertEquals(HistoryRange.MONTH, content.range)
        assertEquals(listOf("PZH" to HistoryRange.MONTH), historyRepository.requested)
        assertFalse(content.isLoading)
    }

    @Test
    fun `content carries the history and every pollen type in the backend order`() = runTest {
        val history = stationHistory()
        historyRepository.result = Result.Success(history)
        val viewModel = viewModel()

        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertEquals(history, content.history)
        assertEquals(allSpecies, content.species)
        // The default history reports birch and grasses with values and ash without any.
        assertEquals(listOf("BIRCH", "GRASSES"), content.shownSpeciesIds)
    }

    @Test
    fun `never writes the home station`() = runTest {
        viewModel()

        advanceUntilIdle()

        assertTrue(selectedStationRepository.writes.isEmpty())
    }

    @Test
    fun `a failed history request is Error with its kind`() = runTest {
        historyRepository.result = Result.Failure(IOException("backend unreachable"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(DiaryUiState.Error(AppError.Network), viewModel.uiState.value)
    }

    @Test
    fun `retry from Error reaches Content once the backend answers`() = runTest {
        historyRepository.result = Result.Failure(IOException("backend unreachable"))
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
        val viewModel = viewModel(FakeSelectedStationRepository(initial = null))

        advanceUntilIdle()

        assertEquals(DiaryUiState.Error(AppError.NoStationSelected), viewModel.uiState.value)
        assertTrue(historyRepository.requested.isEmpty())
    }

    @Test
    fun `selecting a week keeps the previous history while it loads and then replaces it`() = runTest {
        val month = stationHistory(days = 30)
        val week = stationHistory(from = LocalDate(2026, 9, 27), days = 7)
        historyRepository.result = Result.Success(month)
        val viewModel = viewModel()
        advanceUntilIdle()

        historyRepository.gate = CompletableDeferred()
        historyRepository.result = Result.Success(week)
        viewModel.onRangeSelected(HistoryRange.WEEK)
        runCurrent()

        val loading = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertTrue(loading.isLoading)
        assertEquals(month, loading.history)
        assertEquals(HistoryRange.WEEK, loading.range)
        // The chart stays laid out for the history it still shows.
        assertEquals(HistoryRange.MONTH, loading.historyRange)

        historyRepository.gate?.complete(Unit)
        advanceUntilIdle()

        val loaded = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertFalse(loaded.isLoading)
        assertEquals(week, loaded.history)
        assertEquals(HistoryRange.WEEK, loaded.range)
        assertEquals(HistoryRange.WEEK, loaded.historyRange)
        assertEquals(listOf("PZH" to HistoryRange.MONTH, "PZH" to HistoryRange.WEEK), historyRepository.requested)
    }

    @Test
    fun `selecting the range already shown requests nothing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onRangeSelected(HistoryRange.MONTH)
        advanceUntilIdle()

        assertEquals(listOf("PZH" to HistoryRange.MONTH), historyRepository.requested)
        assertFalse(assertIs<DiaryUiState.Content>(viewModel.uiState.value).isLoading)
    }

    @Test
    fun `a failed range change is Error and Retry loads the chosen range`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        historyRepository.result = Result.Failure(IOException("backend unreachable"))
        viewModel.onRangeSelected(HistoryRange.YEAR)
        advanceUntilIdle()
        assertEquals(DiaryUiState.Error(AppError.Network), viewModel.uiState.value)

        historyRepository.result = Result.Success(stationHistory(days = 365))
        viewModel.retry()
        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertEquals(HistoryRange.YEAR, content.range)
        assertEquals(HistoryRange.YEAR, content.historyRange)
        assertEquals(HistoryRange.YEAR, historyRepository.requested.last().second)
    }

    @Test
    fun `selecting a range never writes the home station`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onRangeSelected(HistoryRange.YEAR)
        advanceUntilIdle()

        assertTrue(selectedStationRepository.writes.isEmpty())
    }

    @Test
    fun `content carries only the entries on the days of the history`() = runTest {
        diaryRepository.record(LocalDate(2026, 9, 3), Feeling.BAD) // the day before the window
        diaryRepository.record(LocalDate(2026, 9, 4), Feeling.VERY_BAD) // its first day
        diaryRepository.record(LocalDate(2026, 10, 3), Feeling.GOOD) // its last day: yesterday
        diaryRepository.record(LocalDate(2026, 10, 4), Feeling.VERY_GOOD) // today
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(
            listOf(DiaryEntry(LocalDate(2026, 9, 4), Feeling.VERY_BAD), DiaryEntry(LocalDate(2026, 10, 3), Feeling.GOOD)),
            assertIs<DiaryUiState.Content>(viewModel.uiState.value).entries,
        )
    }

    @Test
    fun `the entry for today is left out even when the history reaches today`() = runTest {
        // A history whose last day is the Swiss today — the backend never sends one, but the rule
        // that today is never plotted must not depend on that.
        historyRepository.result = Result.Success(stationHistory(from = LocalDate(2026, 9, 5)))
        diaryRepository.record(LocalDate(2026, 10, 3), Feeling.GOOD)
        diaryRepository.record(LocalDate(2026, 10, 4), Feeling.BAD)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(
            listOf(DiaryEntry(LocalDate(2026, 10, 3), Feeling.GOOD)),
            assertIs<DiaryUiState.Content>(viewModel.uiState.value).entries,
        )
    }

    @Test
    fun `a new entry inside the window appears without a history reload`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        diaryRepository.record(LocalDate(2026, 9, 20), Feeling.BAD)
        advanceUntilIdle()

        assertEquals(
            listOf(DiaryEntry(LocalDate(2026, 9, 20), Feeling.BAD)),
            assertIs<DiaryUiState.Content>(viewModel.uiState.value).entries,
        )
        assertEquals(listOf("PZH" to HistoryRange.MONTH), historyRepository.requested)
    }

    @Test
    fun `an answer recorded today does not join the graph`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        diaryRepository.record(LocalDate(2026, 10, 4), Feeling.BAD)
        advanceUntilIdle()

        assertEquals(emptyList(), assertIs<DiaryUiState.Content>(viewModel.uiState.value).entries)
    }

    @Test
    fun `entries follow the history on screen while a range change loads`() = runTest {
        diaryRepository.record(LocalDate(2026, 9, 10), Feeling.BAD)
        val viewModel = viewModel()
        advanceUntilIdle()

        historyRepository.gate = CompletableDeferred()
        historyRepository.result = Result.Success(stationHistory(from = LocalDate(2026, 9, 27), days = 7))
        viewModel.onRangeSelected(HistoryRange.WEEK)
        runCurrent()

        // The month is still drawn, so its answer stays with it.
        assertEquals(1, assertIs<DiaryUiState.Content>(viewModel.uiState.value).entries.size)

        historyRepository.gate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(emptyList(), assertIs<DiaryUiState.Content>(viewModel.uiState.value).entries)
    }

    @Test
    fun `reports no entries when the window holds none`() = runTest {
        diaryRepository.record(LocalDate(2026, 9, 3), Feeling.BAD)
        diaryRepository.record(LocalDate(2026, 10, 4), Feeling.BAD)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(assertIs<DiaryUiState.Content>(viewModel.uiState.value).hasNoEntries)
    }

    @Test
    fun `reports entries once the window holds one`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(assertIs<DiaryUiState.Content>(viewModel.uiState.value).hasNoEntries)

        diaryRepository.record(LocalDate(2026, 10, 3), Feeling.GOOD)
        advanceUntilIdle()

        assertFalse(assertIs<DiaryUiState.Content>(viewModel.uiState.value).hasNoEntries)
    }

    // Station choice

    @Test
    fun `selecting another station keeps the previous graph while its history loads`() = runTest {
        val zurichHistory = stationHistory()
        val baselHistory = stationHistory(stationAbbr = "PBS")
        historyRepository.result = Result.Success(zurichHistory)
        val viewModel = viewModel()
        advanceUntilIdle()

        historyRepository.gate = CompletableDeferred()
        historyRepository.result = Result.Success(baselHistory)
        viewModel.onStationSelected("PBS")
        runCurrent()

        val loading = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertTrue(loading.isLoading)
        assertEquals("PBS", loading.stationAbbr)
        assertEquals(zurichHistory, loading.history)

        historyRepository.gate?.complete(Unit)
        advanceUntilIdle()

        val loaded = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertFalse(loaded.isLoading)
        assertEquals("PBS", loaded.stationAbbr)
        assertEquals(baselHistory, loaded.history)
        assertEquals(listOf("PZH" to HistoryRange.MONTH, "PBS" to HistoryRange.MONTH), historyRepository.requested)
    }

    @Test
    fun `selecting another station never writes the home station`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected("PBS")
        advanceUntilIdle()

        assertTrue(selectedStationRepository.writes.isEmpty())
        assertEquals(zurich, selectedStationRepository.stored)
    }

    @Test
    fun `selecting the station already shown requests nothing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected("PZH")
        advanceUntilIdle()

        assertEquals(listOf("PZH" to HistoryRange.MONTH), historyRepository.requested)
    }

    @Test
    fun `a range change keeps the chosen station`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onStationSelected("PBS")
        advanceUntilIdle()

        viewModel.onRangeSelected(HistoryRange.WEEK)
        advanceUntilIdle()

        assertEquals("PBS" to HistoryRange.WEEK, historyRepository.requested.last())
    }

    @Test
    fun `a home station the list no longer holds opens on the first listed station`() = runTest {
        val viewModel = viewModel(FakeSelectedStationRepository(initial = SelectedStation("PXX", "Gone")))

        advanceUntilIdle()

        assertEquals(allStations.first().abbr, assertIs<DiaryUiState.Content>(viewModel.uiState.value).stationAbbr)
    }

    @Test
    fun `a failed station list is Error and Retry fetches it again`() = runTest {
        stationRepository.result = Result.Failure(IOException("backend unreachable"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(DiaryUiState.Error(AppError.Network), viewModel.uiState.value)
        assertTrue(historyRepository.requested.isEmpty())

        stationRepository.result = Result.Success(allStations)
        viewModel.retry()
        advanceUntilIdle()

        assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertEquals(2, stationRepository.callCount)
    }

    @Test
    fun `a failed pollen type list is Error and Retry fetches it again`() = runTest {
        speciesRepository.result = Result.Failure(IOException("backend unreachable"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(DiaryUiState.Error(AppError.Network), viewModel.uiState.value)

        speciesRepository.result = Result.Success(allSpecies)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(allSpecies, assertIs<DiaryUiState.Content>(viewModel.uiState.value).species)
    }

    @Test
    fun `stations and pollen types are fetched once across reloads`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStationSelected("PBS")
        advanceUntilIdle()
        viewModel.onRangeSelected(HistoryRange.YEAR)
        advanceUntilIdle()

        assertEquals(1, stationRepository.callCount)
        assertEquals(1, speciesRepository.callCount)
    }

    @Test
    fun `a failed reload after a station change is Error and Retry resumes every choice`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onSpeciesToggled("BIRCH")
        viewModel.onRangeSelected(HistoryRange.WEEK)
        advanceUntilIdle()

        historyRepository.result = Result.Failure(IOException("backend unreachable"))
        viewModel.onStationSelected("PBS")
        advanceUntilIdle()
        assertEquals(DiaryUiState.Error(AppError.Network), viewModel.uiState.value)

        historyRepository.result = Result.Success(stationHistory(stationAbbr = "PBS"))
        viewModel.retry()
        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertEquals("PBS", content.stationAbbr)
        assertEquals(HistoryRange.WEEK, content.range)
        assertEquals("PBS" to HistoryRange.WEEK, historyRepository.requested.last())
        assertEquals(allSpecies.map { it.id }.toSet() - "BIRCH", content.checked)
    }

    // Pollen type filters

    @Test
    fun `every pollen type is checked at first`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(allSpecies.map { it.id }.toSet(), assertIs<DiaryUiState.Content>(viewModel.uiState.value).checked)
    }

    @Test
    fun `unchecking a pollen type removes its line without a history request`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSpeciesToggled("BIRCH")
        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertFalse("BIRCH" in content.checked)
        assertEquals(listOf("GRASSES"), content.shownSpeciesIds)
        assertEquals(listOf("PZH" to HistoryRange.MONTH), historyRepository.requested)
    }

    @Test
    fun `checking it again brings the line back`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSpeciesToggled("BIRCH")
        viewModel.onSpeciesToggled("BIRCH")

        assertEquals(listOf("BIRCH", "GRASSES"), assertIs<DiaryUiState.Content>(viewModel.uiState.value).shownSpeciesIds)
    }

    @Test
    fun `checked types survive a station change`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onSpeciesToggled("BIRCH")

        historyRepository.gate = CompletableDeferred()
        viewModel.onStationSelected("PBS")
        runCurrent()
        assertFalse("BIRCH" in assertIs<DiaryUiState.Content>(viewModel.uiState.value).checked)
        historyRepository.gate?.complete(Unit)
        advanceUntilIdle()

        assertFalse("BIRCH" in assertIs<DiaryUiState.Content>(viewModel.uiState.value).checked)
    }

    @Test
    fun `checked types survive a range change`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onSpeciesToggled("BIRCH")

        viewModel.onRangeSelected(HistoryRange.YEAR)
        advanceUntilIdle()

        assertFalse("BIRCH" in assertIs<DiaryUiState.Content>(viewModel.uiState.value).checked)
    }

    @Test
    fun `a type without a value on any day is not measured here`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        // ASH is null on every day; the four types the fixture leaves out entirely count as well.
        assertEquals(setOf("ALDER", "HAZEL", "BEECH", "ASH", "OAK"), content.notMeasured)
        // It stays checked, so a station that reports it draws it again.
        assertTrue("ASH" in content.checked)
        assertFalse("ASH" in content.shownSpeciesIds)
    }

    @Test
    fun `a type with a value on a single day is measured`() = runTest {
        val days = stationHistory().days
        val oneAshDay = days.mapIndexed { index, day ->
            if (index == 10) day.copy(levels = day.levels + ("ASH" to PollenSeverity.NONE)) else day
        }
        historyRepository.result = Result.Success(stationHistory().copy(days = oneAshDay))
        val viewModel = viewModel()

        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertFalse("ASH" in content.notMeasured)
        assertTrue("ASH" in content.shownSpeciesIds)
    }

    @Test
    fun `a type is measured again at a station that reports it`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue("ASH" in assertIs<DiaryUiState.Content>(viewModel.uiState.value).notMeasured)

        historyRepository.result = Result.Success(
            stationHistory(stationAbbr = "PBS", levels = mapOf("BIRCH" to PollenSeverity.LOW, "ASH" to PollenSeverity.MODERATE)),
        )
        viewModel.onStationSelected("PBS")
        advanceUntilIdle()

        val content = assertIs<DiaryUiState.Content>(viewModel.uiState.value)
        assertFalse("ASH" in content.notMeasured)
        assertEquals(listOf("BIRCH", "ASH"), content.shownSpeciesIds)
    }
}
