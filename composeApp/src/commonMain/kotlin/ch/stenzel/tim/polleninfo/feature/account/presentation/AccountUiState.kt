package ch.stenzel.tim.polleninfo.feature.account.presentation

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountFormState

/**
 * The account form. [isSubmitting] is set from the tap until the answer arrives — and stays set
 * after a success, so the form cannot be sent twice on the way out. [error] is the last attempt's
 * failure, cleared by the next edit or submit.
 */
data class AccountUiState(
    val form: AccountFormState,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
) {
    val canSubmit: Boolean get() = form.canSubmit && !isSubmitting
}

/** What the account screen should do once. */
sealed interface AccountEvent {
    /** Logged in: go back to where the form was opened from. */
    data object Done : AccountEvent
}
