package ch.stenzel.tim.polleninfo.core.species.data.remote

import ch.stenzel.tim.polleninfo.core.network.checkSuccess
import ch.stenzel.tim.polleninfo.core.species.data.remote.dto.SpeciesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * [baseUrl] is injected so a test can point this at its `MockEngine`, as on `StationApiService`. A
 * non-2xx status is an `HttpStatusException`.
 */
class SpeciesApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun getSpecies(): List<SpeciesDto> = client.get("$baseUrl/pollen/species").checkSuccess().body()
}
