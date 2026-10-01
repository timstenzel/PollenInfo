package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.home.FakeStationMeasurementRepository
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.feature.home.MEASURED_AT
import ch.stenzel.tim.polleninfo.feature.home.measurement
import ch.stenzel.tim.polleninfo.feature.home.reading
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val zurich = SelectedStation(abbr = "PZH", name = "Zürich")
    private val lugano = SelectedStation(abbr = "PLU", name = "Lugano")

    private val selectedStationRepository = FakeSelectedStationRepository(initial = zurich)
    private val measurementRepository = FakeStationMeasurementRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(
        selectedStationRepository,
        GetStationMeasurementUseCase(measurementRepository),
    )

    @Test
    fun `starts in Loading before the readings arrive`() {
        assertIs<HomeUiState.Loading>(viewModel().uiState.value)
    }

    @Test
    fun `resolves to Content with the overall severity`() = runTest {
        measurementRepository.result = Result.Success(
            measurement(
                species = listOf(
                    reading("Birch", 42, PollenSeverity.MODERATE),
                    reading("Grasses", 250, PollenSeverity.VERY_HIGH),
                ),
            ),
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertEquals(PollenSeverity.VERY_HIGH, state.overallSeverity)
    }

    @Test
    fun `Content carries the ordered taxa the unit and the responsible taxon`() = runTest {
        measurementRepository.result = Result.Success(
            measurement(
                species = listOf(
                    reading("Ash"),
                    reading("Birch", 42, PollenSeverity.MODERATE),
                    reading("Grasses", 20, PollenSeverity.HIGH),
                ),
            ),
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("Grasses", "Birch", "Ash"), state.species.map { it.name })
        assertEquals("Grasses", state.drivenBy)
        assertEquals("grains/m3", state.unit)
    }

    @Test
    fun `Content carries the reading's timestamp unchanged`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(MEASURED_AT, assertIs<HomeUiState.Content>(viewModel.uiState.value).measuredAt)
    }

    @Test
    fun `requests the readings for the stored station`() = runTest {
        viewModel()

        advanceUntilIdle()

        assertEquals(listOf("PZH"), measurementRepository.requested)
    }

    @Test
    fun `names the station while the readings are still loading`() = runTest {
        val viewModel = viewModel()

        // Enough for the stored selection to arrive, but the load is still in flight: the top bar
        // has to be able to label itself already.
        advanceUntilIdle()

        assertEquals("Zürich", viewModel.uiState.value.stationName)
    }

    @Test
    fun `names the station on every state including the failure`() = runTest {
        measurementRepository.result = Result.Failure(RuntimeException("no network"))
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Error>(viewModel.uiState.value)
        assertEquals("Zürich", state.stationName)
    }

    @Test
    fun `a repository failure resolves to Error carrying its message`() = runTest {
        measurementRepository.result = Result.Failure(RuntimeException("no network"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals("no network", assertIs<HomeUiState.Error>(viewModel.uiState.value).message)
    }

    @Test
    fun `an exception with no message still produces a readable Error`() = runTest {
        measurementRepository.result = Result.Failure(RuntimeException())
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(
            "An unexpected error occurred",
            assertIs<HomeUiState.Error>(viewModel.uiState.value).message,
        )
    }

    @Test
    fun `reloads when the stored station changes`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        selectedStationRepository.select(lugano)
        advanceUntilIdle()

        // The station name and the readings must never come from different stations.
        assertEquals(listOf("PZH", "PLU"), measurementRepository.requested)
        assertEquals("Lugano", viewModel.uiState.value.stationName)
    }

    @Test
    fun `does not reload when the stored station is written again unchanged`() = runTest {
        viewModel()
        advanceUntilIdle()

        selectedStationRepository.select(zurich)
        advanceUntilIdle()

        assertEquals(listOf("PZH"), measurementRepository.requested)
    }
}
