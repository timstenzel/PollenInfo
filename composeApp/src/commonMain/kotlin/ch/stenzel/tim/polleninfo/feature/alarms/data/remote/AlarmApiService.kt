package ch.stenzel.tim.polleninfo.feature.alarms.data.remote

import ch.stenzel.tim.polleninfo.core.network.checkSuccess
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmInputDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.ErrorDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.RegisterDeviceRequestDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.RegisterDeviceResponseDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.UpdateTokenRequestDto
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmLimitReachedException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmNotFoundException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.InvalidAlarmException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.UnknownDeviceException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException

/**
 * The backend's device and alarm endpoints. [baseUrl] is injected, as on every API service, so a
 * test can point it at its `MockEngine`.
 *
 * Every device call sends the install's device token as `Authorization: Bearer …` to a path under
 * `/devices/me`. Status codes are checked here rather than left to `body()`, which would try to read
 * an error response as if it were the expected payload. A `401` becomes [UnknownDeviceException],
 * the signal the repository re-registers on.
 */
class AlarmApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun registerDevice(fcmToken: String): RegisterDeviceResponseDto =
        client.post("$baseUrl/devices") {
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceRequestDto(fcmToken))
        }.checkSuccess().body()

    /** `401` is [UnknownDeviceException], as on every device call. */
    suspend fun updateToken(deviceToken: String, fcmToken: String) {
        client.put("$baseUrl/devices/me/fcm-token") {
            bearerAuth(deviceToken)
            contentType(ContentType.Application.Json)
            setBody(UpdateTokenRequestDto(fcmToken))
        }.checkedForDevice()
    }

    suspend fun getAlarms(deviceToken: String): List<AlarmDto> =
        client.get("$baseUrl/devices/me/alarms") {
            bearerAuth(deviceToken)
        }.checkedForDevice().body()

    suspend fun createAlarm(deviceToken: String, input: AlarmInputDto): AlarmDto =
        client.post("$baseUrl/devices/me/alarms") {
            bearerAuth(deviceToken)
            contentType(ContentType.Application.Json)
            setBody(input)
        }.checkedForDevice().body()

    suspend fun updateAlarm(deviceToken: String, alarmId: String, input: AlarmInputDto): AlarmDto =
        client.put("$baseUrl/devices/me/alarms/$alarmId") {
            bearerAuth(deviceToken)
            contentType(ContentType.Application.Json)
            setBody(input)
        }.checkedForSingleAlarm().body()

    suspend fun deleteAlarm(deviceToken: String, alarmId: String) {
        client.delete("$baseUrl/devices/me/alarms/$alarmId") {
            bearerAuth(deviceToken)
        }.checkedForSingleAlarm()
    }

    /**
     * On a single alarm's path a `404` only ever means the alarm — an unknown device is a `401` — so
     * it is [AlarmNotFoundException] without asking anything else.
     */
    private suspend fun HttpResponse.checkedForSingleAlarm(): HttpResponse {
        if (status == HttpStatusCode.NotFound) throw AlarmNotFoundException()
        return checkedForDevice()
    }

    /**
     * A `400` from a device path carries the backend's reason, which becomes [InvalidAlarmException];
     * a `409` is the alarm limit, [AlarmLimitReachedException].
     */
    private suspend fun HttpResponse.checkedForDevice(): HttpResponse {
        if (status == HttpStatusCode.Unauthorized) throw UnknownDeviceException()
        if (status == HttpStatusCode.Conflict) throw AlarmLimitReachedException()
        if (status == HttpStatusCode.BadRequest) {
            val reason = try {
                body<ErrorDto>().error
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A 400 without the usual body is still a refusal; only the reason is missing.
                null
            }
            throw InvalidAlarmException(reason ?: "The backend refused this alarm")
        }
        return checkSuccess()
    }
}
