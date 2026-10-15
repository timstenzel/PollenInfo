package ch.stenzel.tim.polleninfo.core.credentials

/**
 * iOS has no call to make here: its password autofill works from the text fields' content types
 * and the wrapper's Associated Domains (see CLAUDE.md, "iOS wrapper configuration"), so there is
 * nothing to save or offer from code. Compile-verified only.
 */
class NoCredentialStore : CredentialStore {
    override suspend fun save(email: String, password: String) = Unit

    override suspend fun offer(): SavedCredential? = null
}
