package ch.stenzel.tim.polleninfo.core.credentials

/** Records what was saved and how often a login was asked for; [offered] is the login it returns. */
class FakeCredentialStore(var offered: SavedCredential? = null) : CredentialStore {

    val saved = mutableListOf<Pair<String, String>>()
    var offerCalls = 0
        private set

    /** Thrown by [save] after recording it — an implementation breaking its never-fails contract. */
    var saveFailure: Throwable? = null

    override suspend fun save(email: String, password: String) {
        saved += email to password
        saveFailure?.let { throw it }
    }

    override suspend fun offer(): SavedCredential? {
        offerCalls++
        return offered
    }
}
