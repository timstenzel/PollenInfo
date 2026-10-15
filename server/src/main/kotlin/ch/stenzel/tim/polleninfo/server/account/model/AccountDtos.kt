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

/** Body of `POST /users/session`. */
@Serializable
class LoginDto(val email: String, val password: String) {

    /** Never prints the email or the password. */
    override fun toString(): String = "LoginDto(…)"
}

/**
 * Response of `POST /users` and `POST /users/session`: the session token the app sends as `Authorization: Bearer …` with
 * every later account call. Returned this once; the server keeps only its hash.
 */
@Serializable
data class SessionDto(val sessionToken: String)

/**
 * Body of `POST /users/me/diary`. [feeling] is a plain string so an unknown name reaches the route
 * and is refused as a `400` naming the field, not as a malformed body.
 */
@Serializable
data class DiaryAnswerDto(val feeling: String)

/** One diary answer on the wire: an ISO date (`2026-10-15`) and a `DiaryFeeling` name. */
@Serializable
data class DiaryEntryDto(val date: String, val feeling: String)

/** The `{error}` body of the account routes' `400`, `401` and `409`. */
@Serializable
data class ErrorDto(val error: String)
