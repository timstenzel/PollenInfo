package ch.stenzel.tim.polleninfo.core.session.data.remote

import ch.stenzel.tim.polleninfo.core.network.checkSuccess
import ch.stenzel.tim.polleninfo.core.session.data.remote.dto.LoginRequestDto
import ch.stenzel.tim.polleninfo.core.session.data.remote.dto.RegisterRequestDto
import ch.stenzel.tim.polleninfo.core.session.data.remote.dto.SessionResponseDto
import ch.stenzel.tim.polleninfo.core.session.domain.model.EmailTakenException
import ch.stenzel.tim.polleninfo.core.session.domain.model.InvalidCredentialsException
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * The backend's account endpoints. [baseUrl] is injected, as on every API service, so a test can
 * point it at its `MockEngine`. The email and password travel only in the body.
 *
 * Account statuses are mapped here before [checkSuccess]: a `409` on registration is
 * [EmailTakenException], a `401` on login [InvalidCredentialsException], and a `401` on a call made
 * with a session token [SignedOutException]. The token travels only in the `Authorization` header.
 */
class AccountApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun register(email: String, password: String): SessionResponseDto {
        val response = client.post("$baseUrl/users") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(email = email, password = password, consent = true))
        }
        if (response.status == HttpStatusCode.Conflict) throw EmailTakenException()
        return response.checkSuccess().body()
    }

    suspend fun login(email: String, password: String): SessionResponseDto {
        val response = client.post("$baseUrl/users/session") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email = email, password = password))
        }
        if (response.status == HttpStatusCode.Unauthorized) throw InvalidCredentialsException()
        return response.checkSuccess().body()
    }

    /** Ends the session [token] belongs to on the backend — this phone's only. */
    suspend fun logout(token: String) {
        val response = client.delete("$baseUrl/users/me/session") { bearerAuth(token) }
        if (response.status == HttpStatusCode.Unauthorized) throw SignedOutException()
        response.checkSuccess()
    }
}
