package ch.stenzel.tim.polleninfo.core.session.data.remote.dto

import kotlinx.serialization.Serializable

/** Body of `POST /users`. Not a data class, so neither the email nor the password is ever printed. */
@Serializable
class RegisterRequestDto(val email: String, val password: String, val consent: Boolean) {
    override fun toString(): String = "RegisterRequestDto(…)"
}

/** Body of `POST /users/session`. Not a data class, for the same reason. */
@Serializable
class LoginRequestDto(val email: String, val password: String) {
    override fun toString(): String = "LoginRequestDto(…)"
}

/** Response of `POST /users` and `POST /users/session`: the session token for every later account call. */
@Serializable
class SessionResponseDto(val sessionToken: String) {
    override fun toString(): String = "SessionResponseDto(…)"
}
