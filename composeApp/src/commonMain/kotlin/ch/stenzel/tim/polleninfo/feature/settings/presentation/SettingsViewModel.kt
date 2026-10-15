package ch.stenzel.tim.polleninfo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.appinfo.AppInfo
import ch.stenzel.tim.polleninfo.core.language.AppLanguage
import ch.stenzel.tim.polleninfo.core.language.LanguageRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.domain.model.toSessionAppError
import ch.stenzel.tim.polleninfo.core.session.domain.repository.SessionRepository
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Settings tab. Holds no text: the Impressum and the data source are fixed wording that the
 * screen takes from string resources, so only the default station and what the platform reports
 * pass through here.
 *
 * The station is **observed**, so the row shows a new default as soon as the change-station screen
 * has stored it. Its name comes from the station list, fetched once; until that arrives, or if it
 * fails, the name stored with the selection stands in — never an error for the whole screen.
 *
 * The language is the platform's setting, read through [LanguageRepository]. It can also change
 * outside the app (Android's per-app language screen), so [onResume] reads it again.
 *
 * The account is **observed** too, from the login stored on the phone — no request: logging in or
 * out anywhere in the app shows here at once. Logging out asks for confirmation first; deleting the
 * account asks for the password, and a wrong one keeps the dialog open with its error.
 */
class SettingsViewModel(
    selectedStationRepository: SelectedStationRepository,
    private val stationRepository: StationRepository,
    private val languageRepository: LanguageRepository,
    appInfo: AppInfo,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(language = languageRepository.current, version = appInfo.version),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** `null` until the list has arrived, and for good if it failed. */
    private val stations = MutableStateFlow<List<Station>?>(null)

    init {
        viewModelScope.launch {
            val result = stationRepository.getStations()
            if (result is Result.Success) stations.value = result.data
        }
        viewModelScope.launch {
            combine(selectedStationRepository.selectedStation, stations) { selected, stations ->
                selected?.let { stored -> stations?.firstOrNull { it.abbr == stored.abbr }?.name ?: stored.name }
            }.collect { name -> _uiState.update { it.copy(stationName = name) } }
        }
        viewModelScope.launch {
            sessionRepository.session.collect { session ->
                _uiState.update {
                    it.copy(
                        accountEmail = session?.email,
                        accountChecked = true,
                        // Logged out elsewhere: there is nothing left to confirm.
                        showLogoutDialog = it.showLogoutDialog && session != null,
                    ).let { state -> if (session == null) state.withDeleteDialogClosed() else state }
                }
            }
        }
    }

    fun onLogoutClicked() {
        if (_uiState.value.accountEmail == null) return
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun onLogoutDismissed() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    /**
     * Closes the dialog and logs out. The session flow then shows the signed-out rows; logging out
     * only fails if the phone cannot clear its own storage, and the account row then simply stays.
     */
    fun onLogoutConfirmed() {
        _uiState.update { it.copy(showLogoutDialog = false) }
        viewModelScope.launch { sessionRepository.logout() }
    }

    fun onDeleteAccountClicked() {
        if (_uiState.value.accountEmail == null) return
        _uiState.update { it.withDeleteDialogClosed().copy(showDeleteDialog = true) }
    }

    /** Ignored while the deletion runs, which sends the password as it was when confirmed. */
    fun onDeletePasswordChanged(password: String) {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(deletePassword = password, deleteError = null) }
    }

    fun onDeletePasswordVisibilityToggled() {
        _uiState.update { it.copy(deletePasswordVisible = !it.deletePasswordVisible) }
    }

    /** Closes the dialog and forgets the password — unless the deletion is already running. */
    fun onDeleteDismissed() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.withDeleteDialogClosed() }
    }

    /**
     * Deletes the account. On success the session flow shows the signed-out rows and the dialog
     * closes; a failure — a wrong password, no connection — keeps it open with its error. A backend
     * that no longer knows the session has signed the phone out, which closes the dialog too.
     */
    fun onDeleteConfirmed() {
        val state = _uiState.value
        if (!state.showDeleteDialog || !state.canConfirmDelete) return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = sessionRepository.deleteAccount(state.deletePassword)) {
                is Result.Success -> _uiState.update { it.withDeleteDialogClosed() }
                is Result.Failure -> _uiState.update {
                    if (result.exception is SignedOutException) {
                        it.withDeleteDialogClosed()
                    } else {
                        it.copy(isDeleting = false, deleteError = result.exception.toSessionAppError())
                    }
                }
            }
        }
    }

    fun onLanguageRowClicked() {
        _uiState.update { it.copy(showLanguageDialog = true) }
    }

    fun onLanguageDialogDismissed() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }

    /** Closes the dialog and, unless [language] is already the current one, chooses it. */
    fun onLanguageSelected(language: AppLanguage) {
        val changed = language != _uiState.value.language
        _uiState.update { it.copy(showLanguageDialog = false) }
        if (!changed) return
        languageRepository.set(language)
        _uiState.update {
            it.copy(
                language = language,
                languageAppliesOnRestart = it.languageAppliesOnRestart || !languageRepository.appliesImmediately,
            )
        }
    }

    /** Reads the language again whenever the screen resumes — it may have been changed in system settings. */
    fun onResume() {
        _uiState.update { it.copy(language = languageRepository.current) }
    }
}

/** The deletion dialog closed, with nothing of it kept — the password least of all. */
private fun SettingsUiState.withDeleteDialogClosed() = copy(
    showDeleteDialog = false,
    deletePassword = "",
    deletePasswordVisible = false,
    isDeleting = false,
    deleteError = null,
)
