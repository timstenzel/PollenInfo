package ch.stenzel.tim.polleninfo.feature.account.presentation

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.FakeSessionRepository
import ch.stenzel.tim.polleninfo.core.session.domain.model.EmailTakenException
import ch.stenzel.tim.polleninfo.core.session.domain.model.InvalidCredentialsException
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val sessions = FakeSessionRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(mode: AccountMode = AccountMode.REGISTER) = AccountViewModel(mode, sessions)

    private fun AccountViewModel.fillValidRegistration() {
        onEmailChanged("tim@example.com")
        onPasswordChanged("correct horse")
        onConfirmationChanged("correct horse")
        onConsentChanged(true)
    }

    /** Drains the one-shot event channel; see `OnboardingViewModelTest.collectEvents`. */
    private fun TestScope.events(viewModel: AccountViewModel): List<AccountEvent> {
        val received = mutableListOf<AccountEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { received += it }
        }
        return received
    }

    @Test
    fun `opens in the mode it was given with an empty form`() {
        assertEquals(AccountMode.REGISTER, viewModel(AccountMode.REGISTER).uiState.value.form.mode)
        val login = viewModel(AccountMode.LOGIN).uiState.value
        assertEquals(AccountMode.LOGIN, login.form.mode)
        assertEquals("", login.form.email)
        assertFalse(login.canSubmit)
    }

    @Test
    fun `submitting a valid registration registers with the email and password`() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidRegistration()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("tim@example.com" to "correct horse"), sessions.registrations)
    }

    @Test
    fun `isSubmitting holds while the registration is in flight and the fields are frozen`() = runTest {
        sessions.registerGate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.fillValidRegistration()

        viewModel.submit()
        runCurrent()

        val submitting = viewModel.uiState.value
        assertTrue(submitting.isSubmitting)
        assertFalse(submitting.canSubmit)
        viewModel.onEmailChanged("other@example.com")
        viewModel.onConsentChanged(false)
        assertEquals("tim@example.com", viewModel.uiState.value.form.email)
        assertTrue(viewModel.uiState.value.form.consent)

        sessions.registerGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `a second submit while one is in flight sends nothing more`() = runTest {
        sessions.registerGate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.fillValidRegistration()

        viewModel.submit()
        runCurrent()
        viewModel.submit()
        sessions.registerGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, sessions.registrations.size)
    }

    @Test
    fun `a successful registration sends Done exactly once and stays submitting`() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.fillValidRegistration()

        viewModel.submit()
        advanceUntilIdle()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf<AccountEvent>(AccountEvent.Done), events)
        assertTrue(viewModel.uiState.value.isSubmitting)
        assertEquals(1, sessions.registrations.size)
    }

    @Test
    fun `an incomplete form is not submitted`() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.fillValidRegistration()
        viewModel.onConsentChanged(false)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(emptyList(), sessions.registrations)
        assertEquals(emptyList(), events)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `a taken email shows EmailTaken and keeps the form`() = runTest {
        sessions.registerResult = Result.Failure(EmailTakenException())
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.fillValidRegistration()

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppError.EmailTaken, state.error)
        assertFalse(state.isSubmitting)
        assertEquals("tim@example.com", state.form.email)
        assertTrue(state.canSubmit)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `no connection shows Network`() = runTest {
        sessions.registerResult = Result.Failure(IOException("offline"))
        val viewModel = viewModel()
        viewModel.fillValidRegistration()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AppError.Network, viewModel.uiState.value.error)
    }

    @Test
    fun `the error is cleared by the next edit`() = runTest {
        sessions.registerResult = Result.Failure(EmailTakenException())
        val viewModel = viewModel()
        viewModel.fillValidRegistration()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.onEmailChanged("anna@example.com")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `the error is cleared by the next submit`() = runTest {
        sessions.registerResult = Result.Failure(EmailTakenException())
        val viewModel = viewModel()
        viewModel.fillValidRegistration()
        viewModel.submit()
        advanceUntilIdle()
        sessions.registerGate = CompletableDeferred()

        viewModel.submit()
        runCurrent()

        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isSubmitting)
        sessions.registerGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `toggling the password visibility keeps the error`() = runTest {
        sessions.registerResult = Result.Failure(EmailTakenException())
        val viewModel = viewModel()
        viewModel.fillValidRegistration()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.onPasswordVisibilityToggled()
        viewModel.onConfirmationVisibilityToggled()

        val state = viewModel.uiState.value
        assertEquals(AppError.EmailTaken, state.error)
        assertTrue(state.form.passwordVisible)
        assertTrue(state.form.confirmationVisible)
    }

    @Test
    fun `switching to log in keeps the email`() {
        val viewModel = viewModel()
        viewModel.fillValidRegistration()

        viewModel.onModeChanged(AccountMode.LOGIN)

        val form = viewModel.uiState.value.form
        assertEquals(AccountMode.LOGIN, form.mode)
        assertEquals("tim@example.com", form.email)
        assertEquals("", form.password)
    }

    // --- Logging in ---

    private fun AccountViewModel.fillLogin() {
        onEmailChanged("tim@example.com")
        onPasswordChanged("correct horse")
    }

    @Test
    fun `submitting the login form logs in with the email and password and never registers`() = runTest {
        val viewModel = viewModel(AccountMode.LOGIN)
        viewModel.fillLogin()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("tim@example.com" to "correct horse"), sessions.logins)
        assertEquals(emptyList(), sessions.registrations)
    }

    @Test
    fun `isSubmitting holds while the login is in flight`() = runTest {
        sessions.loginGate = CompletableDeferred()
        val viewModel = viewModel(AccountMode.LOGIN)
        viewModel.fillLogin()

        viewModel.submit()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.onPasswordChanged("other")
        assertEquals("correct horse", viewModel.uiState.value.form.password)
        sessions.loginGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `a successful login sends Done exactly once`() = runTest {
        val viewModel = viewModel(AccountMode.LOGIN)
        val events = events(viewModel)
        viewModel.fillLogin()

        viewModel.submit()
        advanceUntilIdle()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf<AccountEvent>(AccountEvent.Done), events)
        assertEquals(1, sessions.logins.size)
        assertEquals("tim@example.com", sessions.current?.email)
    }

    @Test
    fun `wrong credentials show InvalidCredentials keep the form and send nothing`() = runTest {
        sessions.loginResult = Result.Failure(InvalidCredentialsException())
        val viewModel = viewModel(AccountMode.LOGIN)
        val events = events(viewModel)
        viewModel.fillLogin()

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppError.InvalidCredentials, state.error)
        assertFalse(state.isSubmitting)
        assertEquals("tim@example.com", state.form.email)
        assertEquals(emptyList(), events)

        viewModel.onPasswordChanged("correct horse!")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `no connection on login shows Network`() = runTest {
        sessions.loginResult = Result.Failure(IOException("offline"))
        val viewModel = viewModel(AccountMode.LOGIN)
        viewModel.fillLogin()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AppError.Network, viewModel.uiState.value.error)
    }

    @Test
    fun `a login switched to from registration logs in`() = runTest {
        val viewModel = viewModel(AccountMode.REGISTER)
        viewModel.fillValidRegistration()
        viewModel.onModeChanged(AccountMode.LOGIN)
        viewModel.onPasswordChanged("correct horse")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("tim@example.com" to "correct horse"), sessions.logins)
        assertEquals(emptyList(), sessions.registrations)
    }
}
