package ch.stenzel.tim.polleninfo.core.history.data.remote

import ch.stenzel.tim.polleninfo.core.history.data.remote.dto.StationHistoryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/**
 * `GET /pollen/stations/{abbr}/history?range=…` on our own backend. [range] is the wire value —
 * `week`, `month` or `year`.
 *
 * Checks the status itself, as `AlarmApiService` does: a `400 {error}` body must not be read as a
 * history.
 */
class StationHistoryApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun getHistory(stationAbbr: String, range: String): StationHistoryDto {
        val response: HttpResponse = client.get("$baseUrl/pollen/stations/$stationAbbr/history") {
            parameter("range", range)
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("history for $stationAbbr responded ${response.status}")
        }
        return response.body()
    }
}
