package ch.stenzel.tim.polleninfo.core.startup

import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(stored: SelectedStation?) =
        StartupViewModel(FakeSelectedStationRepository(initial = stored))

    @Test
    fun `starts in Loading before the stored selection has been read`() = runTest {
        assertIs<StartupState.Loading>(viewModel(stored = null).state.value)
    }

    @Test
    fun `resolves to NeedsOnboarding when no station is stored`() = runTest {
        val viewModel = viewModel(stored = null)
        advanceUntilIdle()

        assertIs<StartupState.NeedsOnboarding>(viewModel.state.value)
    }

    @Test
    fun `resolves to Ready when a station is stored`() = runTest {
        val viewModel = viewModel(stored = SelectedStation(abbr = "PZH", name = "Zürich"))
        advanceUntilIdle()

        assertIs<StartupState.Ready>(viewModel.state.value)
    }

    @Test
    fun `passes through Loading before reaching Ready rather than starting there`() = runTest {
        // Pins the ordering the startup gate depends on: if the initial value were Ready, a first
        // launch would compose the graph at Home before the read had happened.
        val viewModel = viewModel(stored = SelectedStation(abbr = "PBE", name = "Bern"))
        assertIs<StartupState.Loading>(viewModel.state.value)

        advanceUntilIdle()

        assertIs<StartupState.Ready>(viewModel.state.value)
    }

    @Test
    fun `a station stored after the state resolved does not send the app back to onboarding`() =
        runTest {
            // The gate reads the first value only. Onboarding writes the selection and navigates
            // itself; re-resolving here would rebuild the graph underneath that navigation.
            val repository = FakeSelectedStationRepository(initial = null)
            val viewModel = StartupViewModel(repository)
            advanceUntilIdle()
            assertIs<StartupState.NeedsOnboarding>(viewModel.state.value)

            repository.select(SelectedStation(abbr = "PGE", name = "Genève"))
            advanceUntilIdle()

            assertIs<StartupState.NeedsOnboarding>(viewModel.state.value)
        }
}
