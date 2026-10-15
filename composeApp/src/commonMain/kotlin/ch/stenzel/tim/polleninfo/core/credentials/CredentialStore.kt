package ch.stenzel.tim.polleninfo.core.credentials

/**
 * The phone's password manager: offering to save a login, and offering a saved one back.
 *
 * A plain interface bound in `platformModule`, like `PushTokenProvider`. Both calls are offers the
 * user may decline, so neither ever fails: whatever the platform throws — the user cancelled, no
 * password manager, no saved login — is swallowed by the implementation (and logged on Android).
 */
interface CredentialStore {

    /** Offers to save [email] and [password]; returns once the user has answered (or nothing was shown). */
    suspend fun save(email: String, password: String)

    /** Offers this app's saved logins; the one the user picked, or `null`. */
    suspend fun offer(): SavedCredential?
}

/** A login from the password manager. [toString] leaves the password out. */
data class SavedCredential(val email: String, val password: String) {
    override fun toString(): String = "SavedCredential(email=$email, password=***)"
}
