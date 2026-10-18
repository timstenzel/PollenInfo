package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.appinfo.AppInfo
import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion
import ch.stenzel.tim.polleninfo.core.appinfo.FakeAppInfo
import ch.stenzel.tim.polleninfo.core.crashreporting.CrashReporter
import ch.stenzel.tim.polleninfo.core.crashreporting.FakeCrashReporter
import ch.stenzel.tim.polleninfo.core.language.AppLanguage
import ch.stenzel.tim.polleninfo.core.language.FakeLanguageRepository
import ch.stenzel.tim.polleninfo.core.language.LanguageRepository
import ch.stenzel.tim.polleninfo.core.preferences.FakeCrashReportingPreferences
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.FakeSessionRepository
import ch.stenzel.tim.polleninfo.core.session.domain.model.InvalidCredentialsException
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.testSession
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        languageRepository: LanguageRepository = FakeLanguageRepository(),
        crashReporter: CrashReporter = this.crashReporter,
    ) = SettingsViewModel(
        selectedStationRepository,
        stationRepository,
        languageRepository,
        appInfo,
        sessions,
        crashReportingPreferences,
        crashReporter,
    )

    private val sessions = FakeSessionRepository()
    private val crashReportingPreferences = FakeCrashReportingPreferences()
    private val crashReporter = FakeCrashReporter()

    // --- Account ---

    @Test
    fun `the account rows wait until the stored login has been read`() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.accountChecked)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.accountChecked)
    }

    @Test
    fun `signed out shows no account`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.accountEmail)
    }

    @Test
    fun `signed in shows the email of the account`() = runTest {
        sessions.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("tim@example.com", viewModel.uiState.value.accountEmail)
    }

    @Test
    fun `logging in elsewhere shows the account at once`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        sessions.current = testSession.copy(email = "anna@example.com")
        advanceUntilIdle()

        assertEquals("anna@example.com", viewModel.uiState.value.accountEmail)
    }

    @Test
    fun `log out asks first and dismissing keeps the session`() = runTest {
        sessions.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onLogoutClicked()
        assertTrue(viewModel.uiState.value.showLogoutDialog)
        viewModel.onLogoutDismissed()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showLogoutDialog)
        assertEquals(0, sessions.logoutCount)
        assertEquals("tim@example.com", viewModel.uiState.value.accountEmail)
    }

    @Test
    fun `confirming logs out and shows the signed-out rows`() = runTest {
        sessions.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onLogoutClicked()
        viewModel.onLogoutConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showLogoutDialog)
        assertEquals(1, sessions.logoutCount)
        assertNull(state.accountEmail)
    }

    @Test
    fun `log out does nothing while signed out`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onLogoutClicked()

        assertFalse(viewModel.uiState.value.showLogoutDialog)
    }

    @Test
    fun `the logout dialog closes when the session ends elsewhere`() = runTest {
        sessions.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onLogoutClicked()

        sessions.current = null
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showLogoutDialog)
    }

    // --- Deleting the account ---

    private fun TestScope.signedInWithDeleteDialog(): SettingsViewModel {
        sessions.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onDeleteAccountClicked()
        return viewModel
    }

    @Test
    fun `delete account opens an empty dialog that cannot be confirmed yet`() = runTest {
        val viewModel = signedInWithDeleteDialog()

        val state = viewModel.uiState.value
        assertTrue(state.showDeleteDialog)
        assertEquals("", state.deletePassword)
        assertFalse(state.canConfirmDelete)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()
        assertEquals(emptyList(), sessions.deletions)
    }

    @Test
    fun `delete account does nothing while signed out`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDeleteAccountClicked()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `the password visibility toggles`() = runTest {
        val viewModel = signedInWithDeleteDialog()

        viewModel.onDeletePasswordVisibilityToggled()
        assertTrue(viewModel.uiState.value.deletePasswordVisible)
        viewModel.onDeletePasswordVisibilityToggled()
        assertFalse(viewModel.uiState.value.deletePasswordVisible)
    }

    @Test
    fun `dismissing closes the dialog forgets the password and deletes nothing`() = runTest {
        val viewModel = signedInWithDeleteDialog()
        viewModel.onDeletePasswordChanged("correct horse")

        viewModel.onDeleteDismissed()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertEquals("", viewModel.uiState.value.deletePassword)
        assertEquals(emptyList(), sessions.deletions)
        assertEquals(testSession.email, viewModel.uiState.value.accountEmail)
        // Opening it again starts empty.
        viewModel.onDeleteAccountClicked()
        assertEquals("", viewModel.uiState.value.deletePassword)
    }

    @Test
    fun `isDeleting is set while the deletion runs and edits and dismissing are ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        sessions.deleteGate = gate
        val viewModel = signedInWithDeleteDialog()
        viewModel.onDeletePasswordChanged("correct horse")

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isDeleting)
        assertFalse(state.canConfirmDelete)
        viewModel.onDeletePasswordChanged("other")
        viewModel.onDeleteDismissed()
        viewModel.onDeleteConfirmed()
        assertEquals("correct horse", viewModel.uiState.value.deletePassword)
        assertTrue(viewModel.uiState.value.showDeleteDialog)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("correct horse"), sessions.deletions)
    }

    @Test
    fun `a successful deletion closes the dialog and shows the signed-out rows`() = runTest {
        val viewModel = signedInWithDeleteDialog()
        viewModel.onDeletePasswordChanged("correct horse")

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("correct horse"), sessions.deletions)
        assertFalse(state.showDeleteDialog)
        assertFalse(state.isDeleting)
        assertEquals("", state.deletePassword)
        assertNull(state.accountEmail)
        assertTrue(state.accountChecked)
    }

    @Test
    fun `a wrong password keeps the dialog with its error until the next edit`() = runTest {
        sessions.deleteResult = Result.Failure(InvalidCredentialsException())
        val viewModel = signedInWithDeleteDialog()
        viewModel.onDeletePasswordChanged("wrong horse")

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state.showDeleteDialog)
        assertFalse(state.isDeleting)
        assertEquals(AppError.InvalidCredentials, state.deleteError)
        assertEquals("wrong horse", state.deletePassword)
        assertEquals(testSession.email, state.accountEmail)

        viewModel.onDeletePasswordChanged("correct horse")
        state = viewModel.uiState.value
        assertNull(state.deleteError)
        assertTrue(state.canConfirmDelete)
    }

    @Test
    fun `a transport failure keeps the dialog with Network`() = runTest {
        sessions.deleteResult = Result.Failure(IOException("offline"))
        val viewModel = signedInWithDeleteDialog()
        viewModel.onDeletePasswordChanged("correct horse")

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(AppError.Network, viewModel.uiState.value.deleteError)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `a session the backend no longer knows closes the dialog`() = runTest {
        sessions.deleteResult = Result.Failure(SignedOutException())
        val viewModel = signedInWithDeleteDialog()
        viewModel.onDeletePasswordChanged("correct horse")

        viewModel.onDeleteConfirmed()
        sessions.current = null
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showDeleteDialog)
        assertNull(state.deleteError)
        assertNull(state.accountEmail)
    }

    @Test
    fun `the delete dialog closes when the session ends elsewhere`() = runTest {
        val viewModel = signedInWithDeleteDialog()
        viewModel.onDeletePasswordChanged("correct horse")

        sessions.current = null
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertEquals("", viewModel.uiState.value.deletePassword)
    }

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
            FakeLanguageRepository(),
            FakeAppInfo(version = null),
            sessions,
            crashReportingPreferences,
            crashReporter,
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.stationName)
    }

    @Test
    fun `shows the language the platform reports`() {
        val viewModel = viewModel(languageRepository = FakeLanguageRepository(current = AppLanguage.IT))

        assertEquals(AppLanguage.IT, viewModel.uiState.value.language)
    }

    @Test
    fun `opens and closes the language dialog`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.showLanguageDialog)

        viewModel.onLanguageRowClicked()
        assertTrue(viewModel.uiState.value.showLanguageDialog)

        viewModel.onLanguageDialogDismissed()
        assertFalse(viewModel.uiState.value.showLanguageDialog)
    }

    @Test
    fun `selecting a language sets it on the platform and closes the dialog`() {
        val languages = FakeLanguageRepository(current = AppLanguage.SYSTEM)
        val viewModel = viewModel(languageRepository = languages)
        viewModel.onLanguageRowClicked()

        viewModel.onLanguageSelected(AppLanguage.FR)

        assertEquals(listOf(AppLanguage.FR), languages.setCalls)
        assertEquals(AppLanguage.FR, viewModel.uiState.value.language)
        assertFalse(viewModel.uiState.value.showLanguageDialog)
    }

    @Test
    fun `selecting the current language closes the dialog without setting it`() {
        val languages = FakeLanguageRepository(current = AppLanguage.DE)
        val viewModel = viewModel(languageRepository = languages)
        viewModel.onLanguageRowClicked()

        viewModel.onLanguageSelected(AppLanguage.DE)

        assertEquals(emptyList(), languages.setCalls)
        assertFalse(viewModel.uiState.value.showLanguageDialog)
    }

    @Test
    fun `shows no restart note where a change applies immediately`() {
        val viewModel = viewModel(languageRepository = FakeLanguageRepository(appliesImmediately = true))

        viewModel.onLanguageSelected(AppLanguage.DE)

        assertFalse(viewModel.uiState.value.languageAppliesOnRestart)
    }

    @Test
    fun `shows the restart note only after a change where it applies at the next launch`() {
        val viewModel = viewModel(languageRepository = FakeLanguageRepository(appliesImmediately = false))
        assertFalse(viewModel.uiState.value.languageAppliesOnRestart)

        viewModel.onLanguageSelected(AppLanguage.DE)

        assertTrue(viewModel.uiState.value.languageAppliesOnRestart)
    }

    @Test
    fun `reads the language again on resume after it changed outside the app`() {
        val languages = FakeLanguageRepository(current = AppLanguage.FR)
        val viewModel = viewModel(languageRepository = languages)

        languages.current = AppLanguage.DE
        viewModel.onResume()

        assertEquals(AppLanguage.DE, viewModel.uiState.value.language)
    }

    // --- Crash reports ---

    @Test
    fun `an available reporter shows the crash-report switch`() {
        val viewModel = viewModel(crashReporter = FakeCrashReporter(isAvailable = true))

        assertTrue(viewModel.uiState.value.crashReportsAvailable)
    }

    @Test
    fun `an unavailable reporter hides the crash-report switch`() {
        val viewModel = viewModel(crashReporter = FakeCrashReporter(isAvailable = false))

        assertFalse(viewModel.uiState.value.crashReportsAvailable)
    }

    @Test
    fun `crash reports show as on while nothing is stored`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.crashReportsEnabled)
    }

    @Test
    fun `the crash-report switch follows the stored choice`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        crashReportingPreferences.setEnabled(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.crashReportsEnabled)
    }

    @Test
    fun `switching crash reports off stores the choice and shows it`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCrashReportsToggled(false)
        assertFalse(viewModel.uiState.value.crashReportsEnabled)
        advanceUntilIdle()

        assertEquals(listOf(false), crashReportingPreferences.writes)
        assertFalse(crashReportingPreferences.stored)
        assertFalse(viewModel.uiState.value.crashReportsEnabled)
    }

    @Test
    fun `switching crash reports back on stores the choice and shows it`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCrashReportsToggled(false)
        advanceUntilIdle()
        viewModel.onCrashReportsToggled(true)
        advanceUntilIdle()

        assertEquals(listOf(false, true), crashReportingPreferences.writes)
        assertTrue(viewModel.uiState.value.crashReportsEnabled)
    }

    @Test
    fun `crash reports can be switched while signed in`() = runTest {
        sessions.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCrashReportsToggled(false)
        advanceUntilIdle()

        assertFalse(crashReportingPreferences.stored)
        assertFalse(viewModel.uiState.value.crashReportsEnabled)
    }

    @Test
    fun `the view model never applies the choice to the reporter itself`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCrashReportsToggled(false)
        viewModel.onCrashReportsToggled(true)
        advanceUntilIdle()

        assertTrue(crashReporter.applied.isEmpty())
    }

    /** Collects every event the ViewModel sends for the rest of the test. */
    private fun TestScope.collectEvents(viewModel: SettingsViewModel): List<SettingsEvent> {
        val events = mutableListOf<SettingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    @Test
    fun `a failed write puts the switch back and keeps the stored choice`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        crashReportingPreferences.failNextWrite()
        viewModel.onCrashReportsToggled(false)
        assertFalse(viewModel.uiState.value.crashReportsEnabled)
        advanceUntilIdle()

        assertEquals(listOf(false), crashReportingPreferences.writes)
        assertTrue(crashReportingPreferences.stored)
        assertTrue(viewModel.uiState.value.crashReportsEnabled)
    }

    @Test
    fun `a failed write of switching back on puts the switch back off`() = runTest {
        crashReportingPreferences.setEnabled(false)
        val viewModel = viewModel()
        advanceUntilIdle()

        crashReportingPreferences.failNextWrite()
        viewModel.onCrashReportsToggled(true)
        advanceUntilIdle()

        assertFalse(crashReportingPreferences.stored)
        assertFalse(viewModel.uiState.value.crashReportsEnabled)
    }

    @Test
    fun `a failed write says so exactly once`() = runTest {
        val viewModel = viewModel()
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        crashReportingPreferences.failNextWrite()
        viewModel.onCrashReportsToggled(false)
        advanceUntilIdle()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.CrashReportsSaveFailed), events)
    }

    @Test
    fun `a successful write sends no event`() = runTest {
        val viewModel = viewModel()
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.onCrashReportsToggled(false)
        advanceUntilIdle()
        viewModel.onCrashReportsToggled(true)
        advanceUntilIdle()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `the switch works again after a failed write`() = runTest {
        val viewModel = viewModel()
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        crashReportingPreferences.failNextWrite()
        viewModel.onCrashReportsToggled(false)
        advanceUntilIdle()
        viewModel.onCrashReportsToggled(false)
        advanceUntilIdle()

        assertFalse(crashReportingPreferences.stored)
        assertFalse(viewModel.uiState.value.crashReportsEnabled)
        assertEquals(1, events.size)
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
