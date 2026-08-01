package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation

/**
 * Hand-written stand-in for the published file service — the project adds no mocking framework.
 *
 * [failure], when set, is thrown instead of returning bytes, so the "the file could not be
 * obtained" path can be driven without an unreachable host.
 */
class FakePollenFileSource(
    var bytes: ByteArray = hourlyCsv(),
    var failure: Exception? = null,
) : PollenFileSource {

    /** Every station asked for, in order. */
    val requested = mutableListOf<PollenStation>()

    override suspend fun hourlyNow(station: PollenStation): ByteArray {
        requested += station
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
