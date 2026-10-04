package ch.stenzel.tim.polleninfo.feature.alarms.data.remote

import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmInputDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.ErrorDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.RegisterDeviceRequestDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.RegisterDeviceResponseDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.UpdateTokenRequestDto
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmLimitReachedException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmNotFoundException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.InvalidAlarmException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/**
 * The backend's device and alarm endpoints. [baseUrl] is injected, as on every API service, so a
 * test can point it at its `MockEngine`.
 *
 * Status codes are checked here rather than left to `body()`, which would try to read an error
 * response as if it were the expected payload. A `404` on a device path becomes
 * [UnknownDeviceException], the signal the repository re-registers on.
 */
class AlarmApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun registerDevice(fcmToken: String): RegisterDeviceResponseDto =
        client.post("$baseUrl/devices") {
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceRequestDto(fcmToken))
        }.checked().body()

    /** `404` is [UnknownDeviceException], as on every device path. */
    suspend fun updateToken(deviceId: String, fcmToken: String) {
        val response = client.put("$baseUrl/devices/$deviceId/token") {
            contentType(ContentType.Application.Json)
            setBody(UpdateTokenRequestDto(fcmToken))
        }
        if (response.status == HttpStatusCode.NotFound) throw UnknownDeviceException()
        response.checked()
    }

    suspend fun getAlarms(deviceId: String): List<AlarmDto> =
        client.get("$baseUrl/devices/$deviceId/alarms").checkedForDevice().body()

    suspend fun createAlarm(deviceId: String, input: AlarmInputDto): AlarmDto =
        client.post("$baseUrl/devices/$deviceId/alarms") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.checkedForDevice().body()

    suspend fun updateAlarm(deviceId: String, alarmId: String, input: AlarmInputDto): AlarmDto =
        client.put("$baseUrl/devices/$deviceId/alarms/$alarmId") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.checkedForAlarm(deviceId).body()

    suspend fun deleteAlarm(deviceId: String, alarmId: String) {
        client.delete("$baseUrl/devices/$deviceId/alarms/$alarmId").checkedForAlarm(deviceId)
    }

    /**
     * On a single alarm's path the backend answers `404` alike for an unknown device and for an alarm
     * the device does not have, so the alarm's id reveals nothing to anyone else. The two need
     * different handling here — re-registering because of an alarm deleted elsewhere would cut this
     * install off from all its other alarms — so a `404` asks the device's list which one it was.
     */
    private suspend fun HttpResponse.checkedForAlarm(deviceId: String): HttpResponse {
        if (status == HttpStatusCode.NotFound) {
            getAlarms(deviceId) // Throws UnknownDeviceException if it is the device that is unknown.
            throw AlarmNotFoundException()
        }
        return checkedForDevice()
    }

    /**
     * A `400` from an alarm path carries the backend's reason, which becomes [InvalidAlarmException];
     * a `409` is the alarm limit, [AlarmLimitReachedException].
     */
    private suspend fun HttpResponse.checkedForDevice(): HttpResponse {
        if (status == HttpStatusCode.NotFound) throw UnknownDeviceException()
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
        return checked()
    }

    private fun HttpResponse.checked(): HttpResponse {
        if (!status.isSuccess()) throw BackendStatusException(status)
        return this
    }
}

/** The backend does not know the stored device id — its database was reset, or the id is stale. */
class UnknownDeviceException : Exception("The backend does not know this device")

class BackendStatusException(status: HttpStatusCode) :
    Exception("The backend answered ${status.value} ${status.description}")
