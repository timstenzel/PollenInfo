package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.appinfo.AppInfo
import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion
import ch.stenzel.tim.polleninfo.core.appinfo.FakeAppInfo
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val selectedStationRepository = FakeSelectedStationRepository(
        // A stored name that differs from the list's, so the tests can tell which one is shown.
        initial = SelectedStation(abbr = "PZH", name = "Zürich (stored)"),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        stationRepository: StationRepository = FakeStationRepository(),
        appInfo: AppInfo = FakeAppInfo(AppVersion(name = "1.0.0-debug", code = 1)),
    ) = SettingsViewModel(selectedStationRepository, stationRepository, appInfo)

    @Test
    fun `shows the version the platform reports`() {
        val viewModel = viewModel(appInfo = FakeAppInfo(AppVersion(name = "1.0.0-debug", code = 1)))

        assertEquals(AppVersion(name = "1.0.0-debug", code = 1), viewModel.uiState.value.version)
    }

    @Test
    fun `has no version when the platform reports none so the line is hidden`() {
        val viewModel = viewModel(appInfo = FakeAppInfo(version = null))

        assertNull(viewModel.uiState.value.version)
    }

    @Test
    fun `shows the stored name while the station list has not arrived`() = runTest {
        val stations = GatedStationRepository()
        val viewModel = viewModel(stationRepository = stations)
        advanceUntilIdle()

        assertEquals("Zürich (stored)", viewModel.uiState.value.stationName)

        stations.gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("Zürich", viewModel.uiState.value.stationName)
    }

    @Test
    fun `shows the name from the station list once it has arrived`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("Zürich", viewModel.uiState.value.stationName)
    }

    @Test
    fun `keeps the stored name when the station list fails`() = runTest {
        val viewModel = viewModel(FakeStationRepository(Result.Failure(RuntimeException())))
        advanceUntilIdle()

        assertEquals("Zürich (stored)", viewModel.uiState.value.stationName)
    }

    @Test
    fun `shows the new station after the selection changes`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        selectedStationRepository.select(SelectedStation(abbr = "PBE", name = "Bern"))
        advanceUntilIdle()

        assertEquals("Bern", viewModel.uiState.value.stationName)
    }

    @Test
    fun `fetches the station list once however often the selection changes`() = runTest {
        val stations = FakeStationRepository()
        viewModel(stationRepository = stations)
        advanceUntilIdle()

        selectedStationRepository.select(SelectedStation(abbr = "PBE", name = "Bern"))
        selectedStationRepository.select(SelectedStation(abbr = "PLU", name = "Lugano"))
        advanceUntilIdle()

        assertEquals(1, stations.callCount)
    }

    @Test
    fun `has no station name while nothing is stored`() = runTest {
        val viewModel = SettingsViewModel(
            FakeSelectedStationRepository(initial = null),
            FakeStationRepository(),
            FakeAppInfo(version = null),
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.stationName)
    }
}

/** Holds the station list until [gate] is completed. */
private class GatedStationRepository : StationRepository {
    val gate = CompletableDeferred<Unit>()
    private val delegate = FakeStationRepository()

    override suspend fun getStations(): Result<List<Station>> {
        gate.await()
        return delegate.getStations()
    }
}
