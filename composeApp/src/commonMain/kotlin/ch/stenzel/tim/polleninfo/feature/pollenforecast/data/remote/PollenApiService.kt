package ch.stenzel.tim.polleninfo.feature.pollenforecast.data.remote

import ch.stenzel.tim.polleninfo.feature.pollenforecast.data.remote.dto.AirQualityResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

private const val BASE_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"

class PollenApiService(private val client: HttpClient) {

    suspend fun getAirQuality(latitude: Double, longitude: Double): AirQualityResponseDto =
        client.get(BASE_URL) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter(
                "hourly",
                "birch_pollen,grass_pollen,mugwort_pollen,alder_pollen,olive_pollen,ragweed_pollen",
            )
            parameter("timezone", "auto")
            parameter("forecast_days", 3)
        }.body()
}
