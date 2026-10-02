package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.allStations
import ch.stenzel.tim.polleninfo.core.station.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.feature.allstations.PerStationMeasurementRepository
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import ch.stenzel.tim.polleninfo.feature.allstations.domain.usecase.GetAllStationReadingsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AllStationsViewModelTest {

    private val stationRepository = FakeStationRepository()
    private val measurementRepository = PerStationMeasurementRepository()

    /** Wall-clock time the ViewModel stamps on each completed round; moved by hand, never ticks. */
    private val clock = object : Clock {
        var now = Instant.parse("2026-08-01T09:12:00Z")
        override fun now() = now
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AllStationsViewModel(
        stationRepository,
        GetAllStationReadingsUseCase(GetStationMeasurementUseCase(measurementRepository)),
        clock,
    )

    private fun failEveryStation() = measurementRepository.failFor(*allStations.map { it.abbr }.toTypedArray())

    private fun AllStationsViewModel.content() = assertIs<AllStationsUiState.Content>(uiState.value)

    @Test
    fun `starts in Loading before the station list arrives`() {
        assertIs<AllStationsUiState.Loading>(viewModel().uiState.value)
    }

    @Test
    fun `resolves to Content with every station available in alphabetical order`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        val stations = viewModel.content().stations
        assertEquals(expectedStationNamesAlphabetical, stations.map { it.station.name })
        assertTrue(stations.all { it is StationReading.Available })
    }

    @Test
    fun `Content starts with no station selected`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.content().selectedAbbr)
    }

    @Test
    fun `with every reading held the state is Content with every station pending`() = runTest {
        measurementRepository.gateAll = CompletableDeferred()
        val viewModel = viewModel()

        advanceUntilIdle()

        val stations = viewModel.content().stations
        assertEquals(expectedStationNamesAlphabetical, stations.map { it.station.name })
        assertTrue(stations.all { it is StationReading.Pending })
    }

    @Test
    fun `rows fill in as their readings arrive while a held one stays pending`() = runTest {
        val gate = CompletableDeferred<Unit>()
        measurementRepository.gates["PZH"] = gate
        val viewModel = viewModel()

        advanceUntilIdle()

        val meanwhile = viewModel.content().stations
        assertIs<StationReading.Pending>(meanwhile.first { it.station.abbr == "PZH" })
        assertEquals(14, meanwhile.count { it is StationReading.Available })

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.content().stations.all { it is StationReading.Available })
    }

    @Test
    fun `partial failure gives Content with unavailable entries for the failing stations`() = runTest {
        measurementRepository.failFor("PLU", "PGE")
        val viewModel = viewModel()

        advanceUntilIdle()

        val stations = viewModel.content().stations
        assertEquals(
            setOf("PLU", "PGE"),
            stations.filterIsInstance<StationReading.Unavailable>().map { it.station.abbr }.toSet(),
        )
        assertEquals(13, stations.count { it is StationReading.Available })
    }

    @Test
    fun `a failing station list gives Error with its message`() = runTest {
        stationRepository.result = Result.Failure(RuntimeException("backend unreachable"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals("backend unreachable", assertIs<AllStationsUiState.Error>(viewModel.uiState.value).message)
    }

    @Test
    fun `fetches the station list once and every station's reading once`() = runTest {
        viewModel()

        advanceUntilIdle()

        assertEquals(1, stationRepository.callCount)
        assertEquals(15, measurementRepository.requested.size)
    }

    @Test
    fun `retry after a failing station list loads the stations`() = runTest {
        stationRepository.result = Result.Failure(RuntimeException("backend unreachable"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertIs<AllStationsUiState.Error>(viewModel.uiState.value)

        stationRepository.result = Result.Success(allStations)
        viewModel.retry()

        assertIs<AllStationsUiState.Loading>(viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(expectedStationNamesAlphabetical, viewModel.content().stations.map { it.station.name })
        assertEquals(2, stationRepository.callCount)
    }

    @Test
    fun `every reading failing gives Error rather than a list of empty rows`() = runTest {
        failEveryStation()
        val viewModel = viewModel()

        advanceUntilIdle()

        assertIs<AllStationsUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `a single answered station is enough to keep the list`() = runTest {
        failEveryStation()
        measurementRepository.results.remove("PZH")
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(14, viewModel.content().stations.count { it is StationReading.Unavailable })
    }

    @Test
    fun `retry after every reading failed gives Content once the readings recover`() = runTest {
        failEveryStation()
        val viewModel = viewModel()
        advanceUntilIdle()
        assertIs<AllStationsUiState.Error>(viewModel.uiState.value)

        measurementRepository.results.clear()
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.content().stations.all { it is StationReading.Available })
        assertEquals(clock.now, viewModel.content().refreshedAt)
    }

    @Test
    fun `refreshedAt stays absent until the first round completes and then equals the clock`() = runTest {
        val gate = CompletableDeferred<Unit>()
        measurementRepository.gates["PZH"] = gate
        val viewModel = viewModel()
        advanceUntilIdle()

        // Fourteen rows are in but the round is not complete — there is no refresh to report yet.
        assertNull(viewModel.content().refreshedAt)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(clock.now, viewModel.content().refreshedAt)
    }

    @Test
    fun `during a held refresh the previous readings stay on screen flagged as refreshing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = viewModel.content()

        measurementRepository.gateAll = CompletableDeferred()
        viewModel.refresh()
        advanceUntilIdle()

        // Not reset to pending: the rows shown are exactly the ones from before, only flagged.
        assertEquals(before.copy(isRefreshing = true), viewModel.uiState.value)
    }

    @Test
    fun `a completed refresh replaces the readings and moves refreshedAt`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        clock.now += 5.minutes
        measurementRepository.failFor("PBE")
        viewModel.refresh()
        advanceUntilIdle()

        val after = viewModel.content()
        assertFalse(after.isRefreshing)
        assertEquals(clock.now, after.refreshedAt)
        assertIs<StationReading.Unavailable>(after.stations.first { it.station.abbr == "PBE" })
        assertEquals(expectedStationNamesAlphabetical, after.stations.map { it.station.name })
    }

    @Test
    fun `a refresh shows only the final result of its round and never a pending row`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = viewModel.content()

        val gate = CompletableDeferred<Unit>()
        measurementRepository.gates["PZH"] = gate
        measurementRepository.failFor("PBE")
        viewModel.refresh()
        advanceUntilIdle()

        // Fourteen stations have answered, one of them differently, but the round is incomplete.
        assertEquals(before.copy(isRefreshing = true), viewModel.uiState.value)

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.content().stations.none { it is StationReading.Pending })
        assertIs<StationReading.Unavailable>(viewModel.content().stations.first { it.station.abbr == "PBE" })
    }

    @Test
    fun `a refresh reads every station again without fetching the station list`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, stationRepository.callCount)
        assertEquals(30, measurementRepository.requested.size)
    }

    @Test
    fun `every reading failing on refresh gives Error`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        failEveryStation()
        viewModel.refresh()
        advanceUntilIdle()

        assertIs<AllStationsUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `refresh does nothing outside Content`() = runTest {
        stationRepository.result = Result.Failure(RuntimeException("backend unreachable"))
        val viewModel = viewModel()
        advanceUntilIdle()
        val error = viewModel.uiState.value

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(error, viewModel.uiState.value)
        assertEquals(1, stationRepository.callCount)
    }

    @Test
    fun `an instant refresh stays flagged for the minimum indicator time`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceTimeBy(AllStationsViewModel.MIN_REFRESH_INDICATOR - 1.milliseconds)
        runCurrent()
        assertTrue(viewModel.content().isRefreshing)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertFalse(viewModel.content().isRefreshing)
    }
}
