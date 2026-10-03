package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.preferences.FakeNotificationPermissionPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmsViewModelTest {

    private val preferences = FakeNotificationPermissionPreferences()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AlarmsViewModel(preferences)

    @Test
    fun `starts in Loading before any permission state arrives`() {
        assertEquals(AlarmsUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `CAN_REQUEST maps to PermissionRequired with that state`() {
        val viewModel = viewModel()

        viewModel.onPermissionState(NotificationPermissionState.CAN_REQUEST)

        assertEquals(
            AlarmsUiState.PermissionRequired(NotificationPermissionState.CAN_REQUEST),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `MUST_OPEN_SETTINGS maps to PermissionRequired with that state`() {
        val viewModel = viewModel()

        viewModel.onPermissionState(NotificationPermissionState.MUST_OPEN_SETTINGS)

        assertEquals(
            AlarmsUiState.PermissionRequired(NotificationPermissionState.MUST_OPEN_SETTINGS),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `ENABLED maps to Content`() {
        val viewModel = viewModel()

        viewModel.onPermissionState(NotificationPermissionState.ENABLED)

        assertEquals(AlarmsUiState.Content, viewModel.uiState.value)
    }

    @Test
    fun `a permission granted after the prompt moves from PermissionRequired to Content`() {
        val viewModel = viewModel()
        viewModel.onPermissionState(NotificationPermissionState.CAN_REQUEST)

        viewModel.onPermissionState(NotificationPermissionState.ENABLED)

        assertEquals(AlarmsUiState.Content, viewModel.uiState.value)
    }

    @Test
    fun `revoking notifications later returns to PermissionRequired`() {
        val viewModel = viewModel()
        viewModel.onPermissionState(NotificationPermissionState.ENABLED)

        viewModel.onPermissionState(NotificationPermissionState.MUST_OPEN_SETTINGS)

        assertEquals(
            AlarmsUiState.PermissionRequired(NotificationPermissionState.MUST_OPEN_SETTINGS),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `a revoked permission that can be requested again returns to PermissionRequired`() {
        val viewModel = viewModel()
        viewModel.onPermissionState(NotificationPermissionState.ENABLED)

        viewModel.onPermissionState(NotificationPermissionState.CAN_REQUEST)

        assertEquals(
            AlarmsUiState.PermissionRequired(NotificationPermissionState.CAN_REQUEST),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `askedBefore is unknown until the preference has been read`() = runTest {
        val viewModel = viewModel()

        assertNull(viewModel.askedBefore.value)

        advanceUntilIdle()
        assertEquals(false, viewModel.askedBefore.value)
    }

    @Test
    fun `answering the prompt marks it as asked`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPermissionRequested()
        advanceUntilIdle()

        assertTrue(preferences.stored)
        assertEquals(true, viewModel.askedBefore.value)
    }

    @Test
    fun `reading the permission state never marks the prompt as asked`() = runTest {
        val viewModel = viewModel()

        viewModel.onPermissionState(NotificationPermissionState.CAN_REQUEST)
        advanceUntilIdle()

        assertFalse(preferences.stored)
    }
}
