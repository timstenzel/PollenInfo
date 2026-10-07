package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion
import ch.stenzel.tim.polleninfo.core.appinfo.FakeAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the version the platform reports`() {
        val viewModel = SettingsViewModel(FakeAppInfo(AppVersion(name = "1.0.0-debug", code = 1)))

        assertEquals(AppVersion(name = "1.0.0-debug", code = 1), viewModel.uiState.value.version)
    }

    @Test
    fun `has no version when the platform reports none so the line is hidden`() {
        val viewModel = SettingsViewModel(FakeAppInfo(version = null))

        assertNull(viewModel.uiState.value.version)
    }
}
