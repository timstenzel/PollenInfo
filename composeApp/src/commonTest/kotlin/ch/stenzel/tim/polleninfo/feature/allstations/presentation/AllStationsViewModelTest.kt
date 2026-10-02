package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.feature.allstations.PerStationMeasurementRepository
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import ch.stenzel.tim.polleninfo.feature.allstations.domain.usecase.GetAllStationReadingsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AllStationsViewModelTest {

    private val stationRepository = FakeStationRepository()
    private val measurementRepository = PerStationMeasurementRepository()

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
    )

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
    fun `Content starts with no station selected and no refresh stamp`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.content().selectedAbbr)
        assertNull(viewModel.content().refreshedAt)
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
}
