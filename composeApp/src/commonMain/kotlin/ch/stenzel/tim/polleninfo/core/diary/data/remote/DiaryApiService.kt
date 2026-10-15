package ch.stenzel.tim.polleninfo.core.diary.data.remote

import ch.stenzel.tim.polleninfo.core.diary.data.remote.dto.DiaryAnswerRequestDto
import ch.stenzel.tim.polleninfo.core.diary.data.remote.dto.DiaryEntryDto
import ch.stenzel.tim.polleninfo.core.network.checkSuccess
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * The account's diary endpoints, under `/users/me/diary`. [baseUrl] is injected, as on every API
 * service. Every call carries the session [token] as `Authorization: Bearer …`; a `401` is
 * [SignedOutException], mapped here before [checkSuccess].
 */
class DiaryApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    /** Sends today's answer; `201` (stored) and `200` (today had one already) both carry the stored answer. */
    suspend fun record(token: String, feeling: String): DiaryEntryDto {
        val response = client.post("$baseUrl/users/me/diary") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(DiaryAnswerRequestDto(feeling))
        }
        return response.checkSession().body()
    }

    /** Today's answer, or `null` for `204` — today has none yet. */
    suspend fun today(token: String): DiaryEntryDto? {
        val response = client.get("$baseUrl/users/me/diary/today") { bearerAuth(token) }.checkSession()
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }

    /** The answers from [from] to [until], both ISO dates and both included, oldest first. */
    suspend fun entries(token: String, from: String, until: String): List<DiaryEntryDto> {
        val response = client.get("$baseUrl/users/me/diary") {
            bearerAuth(token)
            parameter("from", from)
            parameter("until", until)
        }
        return response.checkSession().body()
    }

    private fun HttpResponse.checkSession(): HttpResponse {
        if (status == HttpStatusCode.Unauthorized) throw SignedOutException()
        return checkSuccess()
    }
}
