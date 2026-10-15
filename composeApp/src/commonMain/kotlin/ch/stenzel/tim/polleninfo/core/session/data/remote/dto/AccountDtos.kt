package ch.stenzel.tim.polleninfo.core.session.data.remote.dto

import kotlinx.serialization.Serializable

/** Body of `POST /users`. Not a data class, so neither the email nor the password is ever printed. */
@Serializable
class RegisterRequestDto(val email: String, val password: String, val consent: Boolean) {
    override fun toString(): String = "RegisterRequestDto(…)"
}

/** Response of `POST /users`: the session token for every later account call. */
@Serializable
class SessionResponseDto(val sessionToken: String) {
    override fun toString(): String = "SessionResponseDto(…)"
}
