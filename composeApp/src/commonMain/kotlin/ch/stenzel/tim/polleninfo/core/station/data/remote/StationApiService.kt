package ch.stenzel.tim.polleninfo.core.station.data.remote

import ch.stenzel.tim.polleninfo.core.network.checkSuccess
import ch.stenzel.tim.polleninfo.core.station.data.remote.dto.StationDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Talks to our own backend. [baseUrl] is injected rather than read from `apiBaseUrl` directly so a
 * test can point this at whatever host its [MockEngine][io.ktor.client.engine.mock.MockEngine]
 * answers for. A non-2xx status is an `HttpStatusException`, never read as a station list.
 */
class StationApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun getStations(): List<StationDto> = client.get("$baseUrl/pollen/stations").checkSuccess().body()
}
