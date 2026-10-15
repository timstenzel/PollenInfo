package ch.stenzel.tim.polleninfo.feature.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.credentials.CredentialStore
import ch.stenzel.tim.polleninfo.core.credentials.SavedCredential
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.domain.model.toSessionAppError
import ch.stenzel.tim.polleninfo.core.session.domain.repository.SessionRepository
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountFormState
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Logging in or creating an account. Opens in [initialMode]; the user can switch between the two
 * forms, keeping the email typed.
 *
 * Field changes are ignored while a submit runs, so what is sent is the form as it was when tapped.
 * Every edit clears the last error. A success sends [AccountEvent.Done] once — the session itself
 * reaches every screen through [SessionRepository.session].
 *
 * The phone's password manager ([CredentialStore]) is offered the login after every success, before
 * [AccountEvent.Done] — which follows whatever the user answers. Opened in [AccountMode.LOGIN], the
 * screen first asks it for a saved login; one the user picks fills the form and is submitted.
 */
class AccountViewModel(
    initialMode: AccountMode,
    private val sessionRepository: SessionRepository,
    private val credentialStore: CredentialStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(AccountFormState(mode = initialMode)))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _events = Channel<AccountEvent>(Channel.BUFFERED)
    val events: Flow<AccountEvent> = _events.receiveAsFlow()

    /** The login the password manager handed over, so logging in with it is not offered for saving again. */
    private var offered: SavedCredential? = null

    init {
        if (initialMode == AccountMode.LOGIN) viewModelScope.launch { useSavedLogin() }
    }

    fun onModeChanged(mode: AccountMode) = edit { it.withMode(mode) }

    fun onEmailChanged(email: String) = edit { it.withEmail(email) }

    fun onPasswordChanged(password: String) = edit { it.withPassword(password) }

    fun onConfirmationChanged(confirmation: String) = edit { it.withConfirmation(confirmation) }

    fun onConsentChanged(consent: Boolean) = edit { it.withConsent(consent) }

    /** Only what is shown changes, so it is allowed while submitting and keeps the error. */
    fun onPasswordVisibilityToggled() = _uiState.update { it.copy(form = it.form.togglePasswordVisible()) }

    fun onConfirmationVisibilityToggled() =
        _uiState.update { it.copy(form = it.form.toggleConfirmationVisible()) }

    /** Ignored unless the form can be submitted and nothing is being submitted yet. */
    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val form = state.form

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = when (form.mode) {
                AccountMode.LOGIN -> sessionRepository.login(form.email, form.password)
                AccountMode.REGISTER -> sessionRepository.register(form.email, form.password)
            }
            when (result) {
                is Result.Success -> {
                    if (SavedCredential(form.email, form.password) != offered) saveLogin(form.email, form.password)
                    _events.send(AccountEvent.Done)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.exception.toSessionAppError())
                }
            }
        }
    }

    private suspend fun useSavedLogin() {
        val credential = try {
            credentialStore.offer()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return
        // The user may have switched to registering while the sheet was up; then it is not a login.
        if (_uiState.value.isSubmitting || _uiState.value.form.mode != AccountMode.LOGIN) return
        offered = credential
        _uiState.update {
            it.copy(form = it.form.withEmail(credential.email).withPassword(credential.password), error = null)
        }
        submit()
    }

    /** The store promises never to throw; should one break that, the user is still logged in and leaves. */
    private suspend fun saveLogin(email: String, password: String) {
        try {
            credentialStore.save(email, password)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Nothing to show: saving was only an offer.
        }
    }

    private fun edit(change: (AccountFormState) -> AccountFormState) {
        _uiState.update { state ->
            if (state.isSubmitting) state else state.copy(form = change(state.form), error = null)
        }
    }
}
