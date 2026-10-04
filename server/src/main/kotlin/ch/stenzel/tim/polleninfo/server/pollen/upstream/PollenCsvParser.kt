package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * One row of a station's hourly file: when it was measured, and what each of the seven taxa read.
 *
 * [concentrations] always has an entry for every [PollenSpecies]. A `null` value means **no
 * reading** — the station does not report that taxon, or its cell in this row was empty. That is a
 * different fact from `0`, which is a measurement of no pollen, and the two must not collapse.
 */
data class ParsedReading(
    val measuredAt: Instant,
    val concentrations: Map<PollenSpecies, Int?>,
)

/**
 * Reads the MeteoSwiss OGD pollen CSV formats, hourly and daily.
 *
 * The format, from
 * [the OGD documentation](https://opendatadocs.meteoswiss.ch/a-data-groundbased/a7-pollen-stations):
 * `;`-separated, ISO-8859-1 encoded, CRLF line endings, a header row of parameter short names, and
 * `dd.MM.yyyy HH:mm` timestamps in UTC. Daily files share the layout, with one `00:00` row per day.
 */
object PollenCsvParser {

    private const val SEPARATOR = ';'
    private const val TIMESTAMP_COLUMN = "reference_timestamp"
    private val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    // `uuuu` with STRICT, so an impossible date such as 31.02. is rejected instead of being
    // resolved to the last day of the month.
    private val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

    /**
     * The single most recent row in which at least one taxon has a value, or `null` when the file
     * has no such row.
     *
     * All seven taxa are then read from that **one** row. The alternative — taking each taxon's
     * latest non-empty value independently — yields up to seven different timestamps, one of which
     * could be half a day older than another with nothing on screen to distinguish them.
     */
    fun parseHourly(bytes: ByteArray): ParsedReading? {
        val lines = decodePublished(bytes).lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()
        val header = lines.firstOrNull()?.split(SEPARATOR) ?: return null

        val timestampAt = header.indexOf(TIMESTAMP_COLUMN).takeIf { it >= 0 } ?: return null
        // Columns that are not one of the seven hourly pollen parameters — `station_abbr`, and
        // anything the publisher adds later — simply do not appear here and are ignored.
        val speciesAt: Map<PollenSpecies, Int> = header.withIndex()
            .mapNotNull { (index, name) -> PollenSpecies.fromHourlyCode(name)?.to(index) }
            .toMap()

        return lines.drop(1)
            .mapNotNull { line -> readingOf(line.split(SEPARATOR), timestampAt, speciesAt) }
            .maxByOrNull { it.measuredAt }
    }

    /**
     * `null` when the row is unusable: no parseable timestamp, or not one taxon with a value.
     *
     * A row of nothing but empty cells is real in these files — the hour is published as soon as it
     * begins, before the count for it exists — and must not be mistaken for the latest reading.
     */
    private fun readingOf(
        cells: List<String>,
        timestampAt: Int,
        speciesAt: Map<PollenSpecies, Int>,
    ): ParsedReading? {
        val measuredAt = cells.getOrNull(timestampAt)?.let(::parseTimestamp) ?: return null
        val concentrations = PollenSpecies.entries.associateWith { species ->
            speciesAt[species]?.let { cells.getOrNull(it) }?.trim()?.toIntOrNull()
        }
        return if (concentrations.values.any { it != null }) {
            ParsedReading(measuredAt, concentrations)
        } else {
            null
        }
    }

    /**
     * Every row of a station's daily file, by date: what each of the seven taxa averaged that day.
     *
     * Only the `d0` columns ([PollenSpecies.dailyCode], 06–06 UTC) are read; the `d1` variant in the
     * same file is ignored. The date part of `reference_timestamp` is the day's date and is used as
     * it stands. Every [PollenSpecies] has an entry for every date, `null` for an empty cell or a
     * missing column — as in [parseHourly], "no value" is not `0`. A row with no value at all is
     * kept as all-`null`, which is what a missing day means to every caller anyway. A row whose date
     * does not parse is skipped.
     */
    fun parseDaily(bytes: ByteArray): Map<LocalDate, Map<PollenSpecies, Int?>> {
        val lines = decodePublished(bytes).lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()
        val header = lines.firstOrNull()?.split(SEPARATOR) ?: return emptyMap()

        val timestampAt = header.indexOf(TIMESTAMP_COLUMN).takeIf { it >= 0 } ?: return emptyMap()
        val speciesAt: Map<PollenSpecies, Int> = header.withIndex()
            .mapNotNull { (index, name) -> PollenSpecies.fromDailyCode(name)?.to(index) }
            .toMap()

        return lines.drop(1)
            .mapNotNull { line ->
                val cells = line.split(SEPARATOR)
                val date = cells.getOrNull(timestampAt)?.let(::parseDate) ?: return@mapNotNull null
                date to PollenSpecies.entries.associateWith { species ->
                    speciesAt[species]?.let { cells.getOrNull(it) }?.trim()?.toIntOrNull()
                }
            }
            .toMap()
    }

    /** The `dd.MM.yyyy` part of a daily row's `dd.MM.yyyy 00:00` timestamp. */
    private fun parseDate(cell: String): LocalDate? = try {
        LocalDate.parse(cell.trim().substringBefore(' '), DATE_FORMAT)
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }

    private fun parseTimestamp(cell: String): Instant? = try {
        LocalDateTime.parse(cell.trim(), TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC)
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }
}

/**
 * Decodes bytes published by MeteoSwiss.
 *
 * **The files are ISO-8859-1, not UTF-8.** Reading them as UTF-8 does not throw — it silently turns
 * `Münsterlingen` into `M?nsterlingen` — so nothing downstream would ever notice the mistake. This
 * is `internal` rather than private so a test can pin it against a Latin-1 fixture directly; the
 * corruption is invisible through [PollenCsvParser.parseHourly], whose output is all numbers.
 */
internal fun decodePublished(bytes: ByteArray): String = String(bytes, StandardCharsets.ISO_8859_1)
