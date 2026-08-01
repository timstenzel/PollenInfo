package ch.stenzel.tim.polleninfo.feature.home.data.remote

import ch.stenzel.tim.polleninfo.feature.home.data.remote.dto.StationMeasurementDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Talks to our own backend, never to MeteoSwiss — all upstream contact, parsing and classification
 * stays on the server.
 *
 * [baseUrl] is injected rather than read from `apiBaseUrl` directly so a test can point this at
 * whatever host its [MockEngine][io.ktor.client.engine.mock.MockEngine] answers for.
 */
class StationMeasurementApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun getMeasurement(stationAbbr: String): StationMeasurementDto =
        client.get("$baseUrl/pollen/stations/$stationAbbr/measurements").body()
}
