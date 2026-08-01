package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation

/**
 * Where a station's published measurement file comes from.
 *
 * The seam exists so the walking skeleton can run off checked-in samples while the HTTP
 * implementation is written; nothing above it knows whether the bytes arrived over the network.
 *
 * **Raw, undecoded bytes.** The character encoding is a property of the file format, not of the
 * transport, so decoding is [PollenCsvParser]'s job and no implementation of this interface may
 * pre-empt it.
 *
 * Throws rather than returning a nullable or empty result when the file cannot be obtained: "the
 * file is missing" and "the file says there is no pollen" are different facts and must not collapse
 * into the same value.
 */
interface PollenFileSource {

    /** Bytes of [station]'s current-day hourly CSV. */
    suspend fun hourlyNow(station: PollenStation): ByteArray
}
