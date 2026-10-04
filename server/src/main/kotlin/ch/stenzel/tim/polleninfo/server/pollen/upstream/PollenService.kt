package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation

/**
 * Where a station's published measurements come from.
 *
 * The seam exists so everything above it — parsing, classification, the endpoint — can be exercised
 * without the network, and so the one class that does contact MeteoSwiss stays replaceable. Nothing
 * above this interface knows whether the bytes arrived over HTTP or out of a checked-in sample.
 *
 * **Raw, undecoded bytes.** The character encoding is a property of the file format, not of the
 * transport, so decoding is [PollenCsvParser]'s job and no implementation of this interface may
 * pre-empt it.
 *
 * Throws rather than returning a nullable or empty result when the measurements cannot be obtained:
 * "we could not reach the publisher" and "the publisher says there is no pollen" are different
 * facts and must not collapse into the same value.
 */
interface PollenService {

    /** Bytes of [station]'s current-day hourly CSV. */
    suspend fun hourlyNow(station: PollenStation): ByteArray

    /** Bytes of [station]'s year-to-date daily CSV, one row per day up to yesterday. */
    suspend fun dailyRecent(station: PollenStation): ByteArray

    /** Bytes of [station]'s daily CSV for every earlier year, ending 31 December of last year. */
    suspend fun dailyHistorical(station: PollenStation): ByteArray
}
