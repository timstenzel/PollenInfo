package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import java.io.IOException

/**
 * The production [PollenService]: fetches the published files from the MeteoSwiss open-data service
 * over HTTP.
 *
 * This is the only class in the project that contacts the upstream, and the only place the base
 * address and the per-station path convention are asserted against reality. Both are the risks this
 * class exists to carry: [PollenStation.hourlyNowPath] had no caller before it.
 *
 * [client] and [baseUrl] are constructor parameters, following the app's `StationApiService(client,
 * baseUrl)` precedent, so tests drive it against a mock engine and no test ever reaches the network.
 * Client configuration — timeouts, retries — belongs to whoever builds the client, not here.
 *
 * Returns the bytes undecoded: the ISO-8859-1 encoding is a property of the file format, so
 * [PollenCsvParser] owns it.
 */
class MeteoSwissPollenService(
    private val client: HttpClient,
    private val baseUrl: String = BASE_URL,
) : PollenService {

    override suspend fun hourlyNow(station: PollenStation): ByteArray {
        val url = "$baseUrl/${station.hourlyNowPath}"
        val response = client.get(url)
        // Checked here rather than left to the client's `expectSuccess`: an injected client may be
        // configured either way, and a 404 body must never reach the parser as if it were a file.
        if (!response.status.isSuccess()) {
            throw IOException("$url responded ${response.status}")
        }
        return response.readRawBytes()
    }

    companion object {
        /**
         * Root of the published pollen dataset, without a trailing slash.
         *
         * Public, no auth, no API key — see
         * <https://opendatadocs.meteoswiss.ch/a-data-groundbased/a7-pollen-stations>.
         */
        const val BASE_URL = "https://data.geo.admin.ch/ch.meteoschweiz.ogd-pollen"
    }
}
