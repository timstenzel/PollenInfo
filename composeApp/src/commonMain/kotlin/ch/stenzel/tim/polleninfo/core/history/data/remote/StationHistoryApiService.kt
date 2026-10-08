package ch.stenzel.tim.polleninfo.core.history.data.remote

import ch.stenzel.tim.polleninfo.core.network.checkSuccess
import ch.stenzel.tim.polleninfo.core.history.data.remote.dto.StationHistoryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * `GET /pollen/stations/{abbr}/history?range=…` on our own backend. [range] is the wire value —
 * `week`, `month` or `year`.
 *
 * Checks the status before reading the body, as every API service does: a `400 {error}` body must
 * not be read as a history, and becomes an `HttpStatusException`.
 */
class StationHistoryApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun getHistory(stationAbbr: String, range: String): StationHistoryDto =
        client.get("$baseUrl/pollen/stations/$stationAbbr/history") {
            parameter("range", range)
        }.checkSuccess().body()
}
