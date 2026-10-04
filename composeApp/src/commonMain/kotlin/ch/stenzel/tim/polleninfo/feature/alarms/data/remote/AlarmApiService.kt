package ch.stenzel.tim.polleninfo.feature.alarms.data.remote

import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.RegisterDeviceRequestDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.RegisterDeviceResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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

    suspend fun getAlarms(deviceId: String): List<AlarmDto> =
        client.get("$baseUrl/devices/$deviceId/alarms").checkedForDevice().body()

    private fun HttpResponse.checkedForDevice(): HttpResponse {
        if (status == HttpStatusCode.NotFound) throw UnknownDeviceException()
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
