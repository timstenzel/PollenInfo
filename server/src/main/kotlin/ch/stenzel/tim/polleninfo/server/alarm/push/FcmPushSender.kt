package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.alarm.domain.PushMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Sends through the FCM HTTP v1 API: one `POST /v1/projects/{projectId}/messages:send` per message.
 *
 * [client] and [baseUrl] are parameters so tests drive it with a mock engine; timeouts belong to
 * whoever builds the client. [accessToken] supplies a current OAuth token for every request, which
 * keeps Google's credential handling out of this class and out of its tests.
 *
 * The body is encoded here rather than through the client's content negotiation, so the wire shape
 * does not depend on how the injected client is configured.
 *
 * The device's push address is a Firebase Installation ID (FID) for an app on firebase-messaging
 * 26+, and a registration token for an install that has not updated yet; [isInstallationId] tells
 * them apart, so each goes in the field FCM expects ([FcmMessage.fid] or [FcmMessage.token]).
 */
class FcmPushSender(
    private val client: HttpClient,
    private val projectId: String,
    private val accessToken: suspend () -> String,
    private val baseUrl: String = BASE_URL,
) : PushSender {

    override suspend fun send(token: String, message: PushMessage): PushResult = try {
        val response = client.post("$baseUrl/v1/projects/$projectId/messages:send") {
            bearerAuth(accessToken())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SendRequest.serializer(), SendRequest(message.toFcm(token))))
        }
        if (response.status.isSuccess()) {
            PushResult.Sent
        } else {
            val body = response.bodyAsText()
            if (isUnregistered(response.status, body)) {
                PushResult.Unregistered
            } else {
                PushResult.Failed(FcmException("FCM responded ${response.status}: $body"))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PushResult.Failed(e)
    }

    /**
     * `404 UNREGISTERED` is FCM's answer for a token whose app is gone. A `400 INVALID_ARGUMENT` is
     * what a token that was never valid gets; the rest of the message is ours and fixed, so the token
     * is the argument at fault. Either way the token is dead.
     */
    private fun isUnregistered(status: HttpStatusCode, body: String): Boolean {
        val error = runCatching { json.decodeFromString(ErrorResponse.serializer(), body).error }.getOrNull()
            ?: return false
        val codes = error.details.mapNotNull { it.errorCode } + listOfNotNull(error.status)
        return when (status) {
            HttpStatusCode.NotFound -> UNREGISTERED in codes
            HttpStatusCode.BadRequest -> INVALID_ARGUMENT in codes
            else -> false
        }
    }

    /**
     * Data-only: with no `notification` block FCM never shows anything itself, so the app writes
     * every notification, in its own language, whether it is in the foreground or not. High priority
     * is what lets FCM start the app's process for it promptly.
     */
    private fun PushMessage.toFcm(address: String) = FcmMessage(
        token = address.takeUnless(::isInstallationId),
        fid = address.takeIf(::isInstallationId),
        android = FcmAndroid(priority = PRIORITY_HIGH),
        data = toData(),
    )

    companion object {
        const val BASE_URL = "https://fcm.googleapis.com"

        /** The OAuth scope an access token needs for [FcmPushSender]. */
        const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

        private const val PRIORITY_HIGH = "HIGH"
        private const val UNREGISTERED = "UNREGISTERED"
        private const val INVALID_ARGUMENT = "INVALID_ARGUMENT"

        // explicitNulls = false: exactly one of `token` / `fid` is sent.
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    }
}

class FcmException(message: String) : Exception(message)

/**
 * Whether a stored push address is a Firebase Installation ID: exactly 22 base64url characters.
 * A registration token is far longer and contains a `:`, so the two never overlap.
 */
fun isInstallationId(address: String): Boolean = INSTALLATION_ID.matches(address)

private val INSTALLATION_ID = Regex("[A-Za-z0-9_-]{22}")

@Serializable
private data class SendRequest(val message: FcmMessage)

@Serializable
private data class FcmMessage(
    val token: String? = null,
    val fid: String? = null,
    val android: FcmAndroid,
    val data: Map<String, String>,
)

@Serializable
private data class FcmAndroid(val priority: String)

@Serializable
private data class ErrorResponse(val error: ErrorBody)

@Serializable
private data class ErrorBody(val status: String? = null, val details: List<ErrorDetail> = emptyList())

@Serializable
private data class ErrorDetail(val errorCode: String? = null)
