package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation

/**
 * Hand-written stand-in for the published service — the project adds no mocking framework.
 *
 * Programmable, which is the whole reason it exists alongside the real samples under
 * `src/test/resources/fixtures`: a caller hands it exactly the file it wants to parse, built with
 * [hourlyCsv], so band boundaries and the "no usable row" case can be driven from bytes no station
 * happens to be publishing today.
 *
 * [failure], when set, is thrown instead of returning bytes, so the "the measurements could not be
 * obtained" path can be driven without an unreachable host. [failures] does the same for single
 * stations, so one station can fail while the others answer. [requested] records what was asked for,
 * so a caller can assert the station reached the service unchanged.
 */
class FakePollenService(
    var bytes: ByteArray = hourlyCsv(),
    var failure: Exception? = null,
) : PollenService {

    /** Thrown for these stations only, ahead of [failure]. */
    val failures = mutableMapOf<PollenStation, Exception>()

    /** Every station asked for, in order. */
    val requested = mutableListOf<PollenStation>()

    override suspend fun hourlyNow(station: PollenStation): ByteArray {
        requested += station
        failures[station]?.let { throw it }
        failure?.let { throw it }
        return bytes
    }
}

/**
 * Builds a file in the published shape: `;`-separated, ISO-8859-1, CRLF, one header row of
 * parameter short names and one row per reading.
 *
 * A taxon absent from a row's map has an empty cell; a taxon absent from [columns] altogether has
 * no column at all. The two are different upstream facts that must reach the same "no reading".
 */
fun hourlyCsv(
    abbr: String = PollenStation.ZUERICH.abbr,
    rows: List<Pair<String, Map<PollenSpecies, Int>>> = listOf(
        "01.08.2026 09:00" to mapOf(PollenSpecies.BIRCH to 42, PollenSpecies.GRASSES to 20),
    ),
    columns: List<PollenSpecies> = PollenSpecies.entries,
): ByteArray {
    val header = (listOf("station_abbr", "reference_timestamp") + columns.map { it.hourlyCode })
    val lines = listOf(header.joinToString(";")) + rows.map { (timestamp, values) ->
        (listOf(abbr, timestamp) + columns.map { values[it]?.toString() ?: "" }).joinToString(";")
    }
    return lines.joinToString(separator = "\r\n", postfix = "\r\n").toByteArray(Charsets.ISO_8859_1)
}
