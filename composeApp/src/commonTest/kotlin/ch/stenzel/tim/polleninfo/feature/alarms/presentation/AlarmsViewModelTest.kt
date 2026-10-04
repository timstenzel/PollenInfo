package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.preferences.FakeNotificationPermissionPreferences
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.feature.alarms.FakeAlarmRepository
import ch.stenzel.tim.polleninfo.feature.alarms.dailyAlarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.PushUnavailableException
import ch.stenzel.tim.polleninfo.feature.alarms.thresholdAlarm
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmsViewModelTest {

    private val preferences = FakeNotificationPermissionPreferences()
    private val alarms = FakeAlarmRepository()
    private val stations = FakeStationRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AlarmsViewModel(preferences, alarms, stations)

    /** A ViewModel whose list has loaded with [alarmsToList] after notifications were enabled. */
    private fun TestScope.loadedViewModel(
        alarmsToList: List<Alarm> = listOf(dailyAlarm()),
    ): AlarmsViewModel {
        alarms.result = Result.Success(alarmsToList)
        val viewModel = viewModel()
        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        advanceUntilIdle()
        return viewModel
    }

    // --- Permission ---

    @Test
    fun `starts in CheckingPermission before any permission state arrives`() {
        assertEquals(AlarmsUiState.CheckingPermission, viewModel().uiState.value)
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
    fun `PermissionRequired makes no repository call`() = runTest {
        val viewModel = viewModel()

        viewModel.onPermissionState(NotificationPermissionState.CAN_REQUEST)
        viewModel.onPermissionState(NotificationPermissionState.MUST_OPEN_SETTINGS)
        advanceUntilIdle()

        assertEquals(0, alarms.callCount)
        assertEquals(0, stations.callCount)
    }

    @Test
    fun `a permission granted after the prompt moves from PermissionRequired to the list`() = runTest {
        val viewModel = viewModel()
        viewModel.onPermissionState(NotificationPermissionState.CAN_REQUEST)

        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        advanceUntilIdle()

        assertIs<AlarmsUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun `revoking notifications later returns to PermissionRequired`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onPermissionState(NotificationPermissionState.MUST_OPEN_SETTINGS)

        assertEquals(
            AlarmsUiState.PermissionRequired(NotificationPermissionState.MUST_OPEN_SETTINGS),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `a revoked permission that can be requested again returns to PermissionRequired`() = runTest {
        val viewModel = loadedViewModel()

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

    // --- Loading the list ---

    @Test
    fun `ENABLED shows Loading and then Content`() = runTest {
        alarms.gate = CompletableDeferred()
        val viewModel = viewModel()

        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        runCurrent()
        assertEquals(AlarmsUiState.Loading, viewModel.uiState.value)

        alarms.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(AlarmsUiState.Content(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun `rows name the station and summarise the alarm`() = runTest {
        val viewModel = loadedViewModel(
            listOf(dailyAlarm(stationAbbr = "PZH"), thresholdAlarm(stationAbbr = "PBE")),
        )

        val content = assertIs<AlarmsUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("Zürich", "Bern"), content.alarms.map { it.stationName })
        assertEquals(
            listOf("Daily report at 08:00", "Threshold alert 07:00–21:00"),
            content.alarms.map { it.summary },
        )
    }

    @Test
    fun `a failing station list falls back to the abbreviation`() = runTest {
        stations.result = Result.Failure(RuntimeException("offline"))

        val viewModel = loadedViewModel(listOf(dailyAlarm(stationAbbr = "PZH")))

        val content = assertIs<AlarmsUiState.Content>(viewModel.uiState.value)
        assertEquals("PZH", content.alarms.single().stationName)
    }

    @Test
    fun `an empty list fetches no stations`() = runTest {
        loadedViewModel(emptyList())

        assertEquals(0, stations.callCount)
    }

    @Test
    fun `every later resume with ENABLED keeps the list without reloading`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        advanceUntilIdle()

        assertEquals(1, alarms.callCount)
        assertIs<AlarmsUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun `revoking and re-enabling notifications shows the same list again`() = runTest {
        val viewModel = loadedViewModel()
        val before = viewModel.uiState.value

        viewModel.onPermissionState(NotificationPermissionState.MUST_OPEN_SETTINGS)
        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        advanceUntilIdle()

        assertEquals(before, viewModel.uiState.value)
        assertEquals(1, alarms.callCount)
    }

    @Test
    fun `a failing load shows Error and retry shows Content`() = runTest {
        alarms.result = Result.Failure(RuntimeException("Connection refused"))
        val viewModel = viewModel()
        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        advanceUntilIdle()

        assertEquals(
            AlarmsUiState.Error(message = "Connection refused", pushUnavailable = false),
            viewModel.uiState.value,
        )

        alarms.result = Result.Success(listOf(dailyAlarm()))
        alarms.gate = CompletableDeferred()
        viewModel.retry()
        runCurrent()
        assertEquals(AlarmsUiState.Loading, viewModel.uiState.value)

        alarms.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, assertIs<AlarmsUiState.Content>(viewModel.uiState.value).alarms.size)
    }

    @Test
    fun `PushUnavailable shows Error with the push-unavailable flag`() = runTest {
        alarms.result = Result.Failure(PushUnavailableException())
        val viewModel = viewModel()

        viewModel.onPermissionState(NotificationPermissionState.ENABLED)
        advanceUntilIdle()

        assertTrue(assertIs<AlarmsUiState.Error>(viewModel.uiState.value).pushUnavailable)
    }

    // --- Refresh ---

    @Test
    fun `a refresh keeps the previous alarms on screen with isRefreshing`() = runTest {
        val viewModel = loadedViewModel(listOf(dailyAlarm()))
        val before = assertIs<AlarmsUiState.Content>(viewModel.uiState.value)
        alarms.gate = CompletableDeferred()
        alarms.result = Result.Success(listOf(dailyAlarm(), thresholdAlarm()))

        viewModel.refresh()
        runCurrent()

        assertEquals(before.copy(isRefreshing = true), viewModel.uiState.value)

        alarms.gate!!.complete(Unit)
        advanceUntilIdle()
        val after = assertIs<AlarmsUiState.Content>(viewModel.uiState.value)
        assertFalse(after.isRefreshing)
        assertEquals(2, after.alarms.size)
    }

    @Test
    fun `a failed refresh replaces the list with Error`() = runTest {
        val viewModel = loadedViewModel()
        alarms.result = Result.Failure(RuntimeException("offline"))

        viewModel.refresh()
        advanceUntilIdle()

        assertIs<AlarmsUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `the station list is fetched once across refreshes`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, alarms.callCount)
        assertEquals(1, stations.callCount)
    }
}
