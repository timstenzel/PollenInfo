package ch.stenzel.tim.polleninfo.core.session.domain.model

/**
 * The account this phone is logged in to: [token] is the secret session token the backend issued,
 * sent as `Authorization: Bearer …` with every account call; [email] is the account's address as
 * the backend stores it (trimmed, lowercased), shown in Settings.
 */
data class Session(val token: String, val email: String) {

    /** Kept out of logs and crash reports by construction. */
    override fun toString(): String = "Session(…)"
}
