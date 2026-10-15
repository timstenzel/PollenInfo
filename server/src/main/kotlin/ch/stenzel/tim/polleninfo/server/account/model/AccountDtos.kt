package ch.stenzel.tim.polleninfo.server.account.model

import kotlinx.serialization.Serializable

/**
 * Body of `POST /users`. [consent] defaults to `false`, so a body without it reaches validation and
 * is refused with a message saying consent is required rather than as malformed.
 */
@Serializable
class RegisterDto(val email: String, val password: String, val consent: Boolean = false) {

    /** Never prints the email or the password. */
    override fun toString(): String = "RegisterDto(…)"
}

/**
 * Response of `POST /users`: the session token the app sends as `Authorization: Bearer …` with
 * every later account call. Returned this once; the server keeps only its hash.
 */
@Serializable
data class SessionDto(val sessionToken: String)

/** The `{error}` body of the account routes' `400` and `409`. */
@Serializable
data class ErrorDto(val error: String)
