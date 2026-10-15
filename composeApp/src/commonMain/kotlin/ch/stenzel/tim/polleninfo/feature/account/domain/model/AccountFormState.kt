package ch.stenzel.tim.polleninfo.feature.account.domain.model

import kotlinx.serialization.Serializable

/** Which form the account screen shows. Part of its route, so it is serializable. */
@Serializable
enum class AccountMode { LOGIN, REGISTER }

/** The backend's password limits, in code points. */
const val MIN_PASSWORD_LENGTH = 8
const val MAX_PASSWORD_LENGTH = 128

/** The backend's limit on an email address. */
const val MAX_EMAIL_LENGTH = 254

/**
 * Everything typed into the account form, and every rule about it. Pure: field changes return a new
 * state, and what the screen derives — hints, whether the button is enabled — is computed here, so
 * it is tested without a ViewModel.
 *
 * The two password fields each have their own show/hide toggle.
 */
data class AccountFormState(
    val mode: AccountMode,
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val consent: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmationVisible: Boolean = false,
) {

    /** Kept out of logs and crash reports: neither the email nor a password is printed. */
    override fun toString(): String = "AccountFormState(mode=$mode)"

    /** The password's length in code points, as the backend counts it, so an emoji counts once. */
    val passwordLength: Int
        get() = password.length - password.indices.count { i ->
            i > 0 && password[i].isLowSurrogate() && password[i - 1].isHighSurrogate()
        }

    /** Below [MIN_PASSWORD_LENGTH]; the hint names the minimum from the start. */
    val passwordTooShort: Boolean get() = passwordLength < MIN_PASSWORD_LENGTH

    val passwordTooLong: Boolean get() = passwordLength > MAX_PASSWORD_LENGTH

    /** Only once something has been typed into the confirmation, so an empty field is not an error. */
    val passwordsMismatch: Boolean get() = confirmation.isNotEmpty() && confirmation != password

    /**
     * The same light check as the backend: at most [MAX_EMAIL_LENGTH] characters once trimmed and
     * exactly one `@` with something on each side.
     */
    val isEmailPlausible: Boolean
        get() {
            val trimmed = email.trim()
            val parts = trimmed.split('@')
            return trimmed.length <= MAX_EMAIL_LENGTH && parts.size == 2 && parts.none { it.isEmpty() }
        }

    /**
     * Logging in needs an address and a password. Registering needs an address, a password of
     * [MIN_PASSWORD_LENGTH] to [MAX_PASSWORD_LENGTH] characters, the same password again and the
     * consent — exactly what the backend would accept.
     */
    val canSubmit: Boolean
        get() = when (mode) {
            AccountMode.LOGIN -> isEmailPlausible && password.isNotEmpty()
            AccountMode.REGISTER ->
                isEmailPlausible && !passwordTooShort && !passwordTooLong &&
                    confirmation == password && consent
        }

    /** The other form, keeping the email typed so far; passwords and consent start over. */
    fun withMode(mode: AccountMode): AccountFormState =
        if (mode == this.mode) this else AccountFormState(mode = mode, email = email)

    fun withEmail(email: String) = copy(email = email)

    fun withPassword(password: String) = copy(password = password)

    fun withConfirmation(confirmation: String) = copy(confirmation = confirmation)

    fun withConsent(consent: Boolean) = copy(consent = consent)

    fun togglePasswordVisible() = copy(passwordVisible = !passwordVisible)

    fun toggleConfirmationVisible() = copy(confirmationVisible = !confirmationVisible)
}
