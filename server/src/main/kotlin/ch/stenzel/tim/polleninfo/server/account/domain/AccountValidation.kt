package ch.stenzel.tim.polleninfo.server.account.domain

/**
 * A checked registration: [email] normalised by [AccountValidation.normalizeEmail], [password]
 * exactly as typed. Only ever hashed, never stored or logged.
 */
class AccountSpec(val email: String, val password: String) {

    /** Kept out of logs and exception messages by construction: neither field is printed. */
    override fun toString(): String = "AccountSpec(…)"
}

sealed interface AccountValidationResult {
    data class Valid(val spec: AccountSpec) : AccountValidationResult

    /** [message] is meant for the `400` body, so it names what is wrong — never the value itself. */
    data class Invalid(val message: String) : AccountValidationResult
}

/**
 * The rules every account satisfies, applied to whatever a client sends. Pure: the routes call it
 * before hashing anything or touching the store.
 */
object AccountValidation {

    /** The longest address SMTP can deliver to. */
    const val MAX_EMAIL_LENGTH = 254

    /** In code points, so an emoji counts once. */
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 128

    /**
     * [raw] trimmed and lowercased, or `null` if it cannot be an address: longer than
     * [MAX_EMAIL_LENGTH] once trimmed, or not exactly one `@` with something on each side. Deliberately
     * light — there is no email delivery to prove an address, so a stricter check would only refuse
     * real ones.
     */
    fun normalizeEmail(raw: String): String? {
        val email = raw.trim().lowercase()
        if (email.length > MAX_EMAIL_LENGTH) return null
        val parts = email.split('@')
        if (parts.size != 2 || parts.any { it.isEmpty() }) return null
        return email
    }

    /**
     * Checks a registration. The password is never trimmed — a space is a character like any other —
     * and has no character-type rules, only a length between [MIN_PASSWORD_LENGTH] and
     * [MAX_PASSWORD_LENGTH] code points. [consent] to storing the diary must be given.
     */
    fun validateRegistration(email: String, password: String, consent: Boolean): AccountValidationResult {
        val normalized = normalizeEmail(email)
            ?: return AccountValidationResult.Invalid("email must be an email address of at most $MAX_EMAIL_LENGTH characters")
        val length = password.codePointCount(0, password.length)
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            return AccountValidationResult.Invalid(
                "password must be $MIN_PASSWORD_LENGTH to $MAX_PASSWORD_LENGTH characters long",
            )
        }
        if (!consent) {
            return AccountValidationResult.Invalid("consent to storing the diary is required")
        }
        return AccountValidationResult.Valid(AccountSpec(normalized, password))
    }
}
