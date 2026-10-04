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
 *
 * All of those concern the hourly file. Each daily file has its own set — bytes, a failure and a
 * record of requests — so a test can fail one file while the other answers, and see which file a
 * caller actually asked for.
 */
class FakePollenService(
    var bytes: ByteArray = hourlyCsv(),
    var failure: Exception? = null,
    var dailyRecentBytes: ByteArray = dailyCsv(),
    var dailyHistoricalBytes: ByteArray = dailyCsv(),
) : PollenService {

    /** Thrown by [dailyRecent] instead of returning [dailyRecentBytes]. */
    var dailyRecentFailure: Exception? = null

    /** Every station whose daily recent file was asked for, in order. */
    val dailyRecentRequested = mutableListOf<PollenStation>()

    /** Thrown by [dailyHistorical] instead of returning [dailyHistoricalBytes]. */
    var dailyHistoricalFailure: Exception? = null

    /** Every station whose daily historical file was asked for, in order. */
    val dailyHistoricalRequested = mutableListOf<PollenStation>()

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

    override suspend fun dailyRecent(station: PollenStation): ByteArray {
        dailyRecentRequested += station
        dailyRecentFailure?.let { throw it }
        return dailyRecentBytes
    }

    override suspend fun dailyHistorical(station: PollenStation): ByteArray {
        dailyHistoricalRequested += station
        dailyHistoricalFailure?.let { throw it }
        return dailyHistoricalBytes
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

/**
 * Builds a daily file in the published shape, with the `d0` columns only: one row per date
 * (`dd.MM.yyyy`, written with the publisher's `00:00` time), the same empty-cell and missing-column
 * conventions as [hourlyCsv].
 */
fun dailyCsv(
    abbr: String = PollenStation.ZUERICH.abbr,
    rows: List<Pair<String, Map<PollenSpecies, Int>>> = emptyList(),
    columns: List<PollenSpecies> = PollenSpecies.entries,
): ByteArray {
    val header = (listOf("station_abbr", "reference_timestamp") + columns.map { it.dailyCode })
    val lines = listOf(header.joinToString(";")) + rows.map { (date, values) ->
        (listOf(abbr, "$date 00:00") + columns.map { values[it]?.toString() ?: "" }).joinToString(";")
    }
    return lines.joinToString(separator = "\r\n", postfix = "\r\n").toByteArray(Charsets.ISO_8859_1)
}
