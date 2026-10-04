package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.io.IOException

/**
 * Every request is intercepted by a mock engine, so nothing here contacts the real service. What
 * these tests can pin is the shape of the request and the handling of the response; that the
 * address is actually served is checked by hand against the live service (task 02's quality gate).
 */
class MeteoSwissPollenServiceTest {

    private val requestedUrls = mutableListOf<String>()

    private fun serviceWith(
        baseUrl: String = TEST_BASE_URL,
        body: ByteArray = ByteArray(0),
        status: HttpStatusCode = HttpStatusCode.OK,
    ): MeteoSwissPollenService {
        val engine = MockEngine { request ->
            requestedUrls += request.url.toString()
            if (status.value >= 400) respondError(status) else respond(body, status)
        }
        return MeteoSwissPollenService(HttpClient(engine), baseUrl)
    }

    @Test
    fun `the requested path is derived from the station's own abbreviation`() = runTest {
        val service = serviceWith()

        service.hourlyNow(PollenStation.ZUERICH)
        service.hourlyNow(PollenStation.BERN)

        assertEquals(
            listOf(
                "$TEST_BASE_URL/pzh/ogd-pollen_pzh_h_now.csv",
                "$TEST_BASE_URL/pbe/ogd-pollen_pbe_h_now.csv",
            ),
            requestedUrls,
        )
    }

    @Test
    fun `the default base URL is the published open-data root`() = runTest {
        val service = serviceWith(baseUrl = MeteoSwissPollenService.BASE_URL)

        service.hourlyNow(PollenStation.MUENSTERLINGEN)

        // Pinned in full: base address plus path convention is exactly the URL a browser can open.
        assertEquals(
            listOf(
                "https://data.geo.admin.ch/ch.meteoschweiz.ogd-pollen/" +
                    "pmu/ogd-pollen_pmu_h_now.csv",
            ),
            requestedUrls,
        )
    }

    @Test
    fun `the published bytes are returned undecoded`() = runTest {
        // 0xFC is `ü` in ISO-8859-1 and not valid UTF-8 on its own: it survives only if nothing
        // between the socket and the caller tries to read the payload as text.
        val published = byteArrayOf(0x4D, 0xFC.toByte(), 0x6E)
        val service = serviceWith(body = published)

        assertContentEquals(published, service.hourlyNow(PollenStation.ZUERICH))
    }

    @Test
    fun `a not found response fails rather than yielding an empty file`() = runTest {
        val service = serviceWith(status = HttpStatusCode.NotFound)

        // Returning the error body would reach the parser as a file with no usable row, which the
        // route reports as "this station has nothing to report" — a different fact entirely.
        assertFailsWith<IOException> { service.hourlyNow(PollenStation.ZUERICH) }
    }

    @Test
    fun `an upstream server error fails rather than yielding partial content`() = runTest {
        val service = serviceWith(status = HttpStatusCode.InternalServerError)

        assertFailsWith<IOException> { service.hourlyNow(PollenStation.ZUERICH) }
    }

    @Test
    fun `an upstream gateway error fails rather than yielding partial content`() = runTest {
        val service = serviceWith(status = HttpStatusCode.BadGateway)

        assertFailsWith<IOException> { service.hourlyNow(PollenStation.ZUERICH) }
    }

    @Test
    fun `the daily recent file is requested from the station's daily recent path`() = runTest {
        val service = serviceWith(baseUrl = MeteoSwissPollenService.BASE_URL)

        service.dailyRecent(PollenStation.ZUERICH)

        assertEquals(
            listOf(
                "https://data.geo.admin.ch/ch.meteoschweiz.ogd-pollen/" +
                    "pzh/ogd-pollen_pzh_d_recent.csv",
            ),
            requestedUrls,
        )
    }

    @Test
    fun `the daily recent bytes are returned undecoded`() = runTest {
        val published = byteArrayOf(0x4D, 0xFC.toByte(), 0x6E)
        val service = serviceWith(body = published)

        assertContentEquals(published, service.dailyRecent(PollenStation.ZUERICH))
    }

    @Test
    fun `a daily recent request that is not found fails`() = runTest {
        val service = serviceWith(status = HttpStatusCode.NotFound)

        assertFailsWith<IOException> { service.dailyRecent(PollenStation.ZUERICH) }
    }

    @Test
    fun `a daily recent request answered with a server error fails`() = runTest {
        val service = serviceWith(status = HttpStatusCode.InternalServerError)

        assertFailsWith<IOException> { service.dailyRecent(PollenStation.ZUERICH) }
    }

    @Test
    fun `the daily historical file is requested from the station's daily historical path`() = runTest {
        val service = serviceWith(baseUrl = MeteoSwissPollenService.BASE_URL)

        service.dailyHistorical(PollenStation.ZUERICH)

        assertEquals(
            listOf(
                "https://data.geo.admin.ch/ch.meteoschweiz.ogd-pollen/" +
                    "pzh/ogd-pollen_pzh_d_historical.csv",
            ),
            requestedUrls,
        )
    }

    @Test
    fun `the daily historical bytes are returned undecoded`() = runTest {
        val published = byteArrayOf(0x4D, 0xFC.toByte(), 0x6E)
        val service = serviceWith(body = published)

        assertContentEquals(published, service.dailyHistorical(PollenStation.ZUERICH))
    }

    @Test
    fun `a daily historical request that is not found fails`() = runTest {
        val service = serviceWith(status = HttpStatusCode.NotFound)

        assertFailsWith<IOException> { service.dailyHistorical(PollenStation.ZUERICH) }
    }

    @Test
    fun `a daily historical request answered with a server error fails`() = runTest {
        val service = serviceWith(status = HttpStatusCode.InternalServerError)

        assertFailsWith<IOException> { service.dailyHistorical(PollenStation.ZUERICH) }
    }

    private companion object {
        const val TEST_BASE_URL = "https://example.invalid/ogd-pollen"
    }
}
