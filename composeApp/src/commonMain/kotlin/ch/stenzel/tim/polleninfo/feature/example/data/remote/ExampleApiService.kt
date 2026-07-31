package ch.stenzel.tim.polleninfo.feature.example.data.remote

import ch.stenzel.tim.polleninfo.feature.example.data.remote.dto.ExampleResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Placeholder host — `example.com` is reserved for documentation (RFC 2606), so nothing is served
 * here and this call cannot succeed against a live network. The reference feature exists to show how
 * a Ktor service is shaped and tested.
 *
 * A real feature talks to our own backend instead: see
 * [StationApiService][ch.stenzel.tim.polleninfo.feature.onboarding.data.remote.StationApiService],
 * which takes its base URL as a constructor parameter and is handed
 * [apiBaseUrl][ch.stenzel.tim.polleninfo.core.network.apiBaseUrl] by Koin.
 */
private const val BASE_URL = "https://api.example.com/v1/pollen"

class ExampleApiService(private val client: HttpClient) {

    suspend fun getSnapshot(latitude: Double, longitude: Double): ExampleResponseDto =
        client.get(BASE_URL) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("species", "birch,grass,mugwort,alder,olive,ragweed")
            parameter("timezone", "auto")
            parameter("hours", 72)
        }.body()
}
