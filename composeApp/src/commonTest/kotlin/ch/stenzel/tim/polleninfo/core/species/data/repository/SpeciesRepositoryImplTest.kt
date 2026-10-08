package ch.stenzel.tim.polleninfo.core.species.data.repository

import ch.stenzel.tim.polleninfo.core.network.HttpStatusException
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.allSpecies
import ch.stenzel.tim.polleninfo.core.species.data.remote.SpeciesApiService
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.species.speciesJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

/** Through a real Ktor client backed by [MockEngine], as `StationRepositoryImplTest` does. */
class SpeciesRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))

    private val recorded = mutableListOf<HttpRequestData>()

    private fun repository(handler: MockRequestHandler): SpeciesRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        return SpeciesRepositoryImpl(SpeciesApiService(createHttpClient(engine), "http://backend.test:8080"))
    }

    @Test
    fun `maps the seven species in the server's order`() = runTest {
        val repository = repository { respond(speciesJson, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getSpecies()

        assertEquals(allSpecies, assertIs<Result.Success<List<Species>>>(result).data)
    }

    @Test
    fun `requests the species endpoint on the base url it was given`() = runTest {
        val repository = repository { respond(speciesJson, HttpStatusCode.OK, jsonHeaders) }

        repository.getSpecies()

        assertEquals("http://backend.test:8080/pollen/species", recorded.single().url.toString())
    }

    @Test
    fun `returns Failure when the backend responds with a server error`() = runTest {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.getSpecies())
    }

    @Test
    fun `a 502 fails with HttpStatusException rather than a deserialization error`() = runTest {
        // A JSON error body, so a service that skipped the status check would try to read it.
        val repository = repository {
            respond("""{"error":"upstream failed"}""", HttpStatusCode.BadGateway, jsonHeaders)
        }

        val failure = assertIs<Result.Failure>(repository.getSpecies())
        assertEquals(502, assertIs<HttpStatusException>(failure.exception).status)
    }

    @Test
    fun `returns Failure instead of throwing when the backend is unreachable`() = runTest {
        val repository = repository { throw RuntimeException("Connection refused") }

        assertIs<Result.Failure>(repository.getSpecies())
    }
}
