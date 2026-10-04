package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Two sets of fixtures, both under `src/test/resources` and both ISO-8859-1 with CRLF endings
 * exactly as the publisher emits them. `csv/` holds small hand-written files that isolate one
 * parsing rule each; `fixtures/ogd-pollen/` holds verbatim downloads of all 15 published files,
 * which is what keeps the rules honest against the real column layout. No test here contacts
 * MeteoSwiss.
 */
class PollenCsvParserTest {

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.classLoader.getResourceAsStream("csv/$name")) { "missing $name" }
            .use { it.readBytes() }

    @Test
    fun `decodes the publisher's Latin-1 bytes rather than reading them as UTF-8`() {
        val text = decodePublished(fixture("latin1_station_name.csv"))

        assertContains(text, "Münsterlingen")
        // Pins the failure mode rather than only the success: reading Latin-1 as UTF-8 does not
        // throw, it substitutes the replacement character, and nothing downstream would notice.
        assertTrue('�' !in text, "decoded text contains a Unicode replacement character")
    }

    @Test
    fun `reads the seven taxa from the latest row that has a value`() {
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("full_day.csv")))

        assertEquals(Instant.parse("2026-08-01T07:00:00Z"), reading.measuredAt)
        assertEquals(42, reading.concentrations[PollenSpecies.BIRCH])
        assertEquals(20, reading.concentrations[PollenSpecies.GRASSES])
        assertEquals(0, reading.concentrations[PollenSpecies.ALDER])
        assertEquals(7, reading.concentrations[PollenSpecies.BEECH])
        assertEquals(1, reading.concentrations[PollenSpecies.ASH])
        assertEquals(2, reading.concentrations[PollenSpecies.OAK])
    }

    @Test
    fun `a row of nothing but empty cells is not the latest reading`() {
        // The 08:00 row exists in the fixture and is entirely blank — the hour is published as soon
        // as it begins. Taking it would report the station as having stopped measuring.
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("full_day.csv")))

        assertEquals(Instant.parse("2026-08-01T07:00:00Z"), reading.measuredAt)
    }

    @Test
    fun `an empty cell in the chosen row reads as no measurement, not as zero`() {
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("full_day.csv")))

        assertNull(reading.concentrations[PollenSpecies.HAZEL])
        // Contrast: alder is present in the same row with an actual measurement of zero.
        assertEquals(0, reading.concentrations[PollenSpecies.ALDER])
    }

    @Test
    fun `a taxon whose column is absent reads as no measurement`() {
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("grasses_only.csv")))

        assertEquals(20, reading.concentrations[PollenSpecies.GRASSES])
        assertEquals(
            PollenSpecies.entries - PollenSpecies.GRASSES,
            reading.concentrations.filterValues { it == null }.keys.toList(),
        )
    }

    @Test
    fun `every taxon has an entry even when the file reports only one of them`() {
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("grasses_only.csv")))

        assertEquals(PollenSpecies.entries.toSet(), reading.concentrations.keys)
    }

    @Test
    fun `columns that are not pollen parameters are ignored`() {
        // `full_day.csv` carries `station_abbr` and an invented `kacloudh0`; neither may derail the
        // column-to-taxon mapping of the ones that follow it.
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("full_day.csv")))

        assertEquals(PollenSpecies.entries.toSet(), reading.concentrations.keys)
        assertEquals(42, reading.concentrations[PollenSpecies.BIRCH])
    }

    @Test
    fun `timestamps are read as UTC`() {
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("grasses_only.csv")))

        // `01.08.2026 09:00` in the file; UTC, so no offset is applied whatever the JVM's zone is.
        assertEquals(Instant.parse("2026-08-01T09:00:00Z"), reading.measuredAt)
    }

    @Test
    fun `the most recent row is chosen by timestamp, not by position in the file`() {
        val reading = checkNotNull(PollenCsvParser.parseHourly(fixture("unordered_rows.csv")))

        assertEquals(Instant.parse("2026-08-01T09:00:00Z"), reading.measuredAt)
        assertEquals(99, reading.concentrations[PollenSpecies.BIRCH])
    }

    @Test
    fun `returns null when no row in the file has any value`() {
        assertNull(PollenCsvParser.parseHourly(fixture("no_usable_row.csv")))
    }

    @Test
    fun `returns null for an empty file`() {
        assertNull(PollenCsvParser.parseHourly(ByteArray(0)))
    }

    @Test
    fun `returns null when the timestamp column is missing`() {
        val bytes = "station_abbr;kabetuh0\r\nPZH;42\r\n".toByteArray(Charsets.ISO_8859_1)

        assertNull(PollenCsvParser.parseHourly(bytes))
    }

    @Test
    fun `every station's real published sample parses to a reading`() {
        PollenStation.entries.forEach { station ->
            val reading = checkNotNull(PollenCsvParser.parseHourly(publishedSample(station))) {
                "no usable row in the checked-in sample for ${station.abbr}"
            }

            // Only that the real column layout maps to all seven taxa — the values themselves are
            // a frozen snapshot and are nobody's business to assert.
            assertEquals(PollenSpecies.entries.toSet(), reading.concentrations.keys, station.abbr)
            assertTrue(
                reading.concentrations.values.any { it != null },
                "every taxon reads as absent in the sample for ${station.abbr}",
            )
        }
    }

    @Test
    fun `each sample is filed under its own station's published path`() {
        PollenStation.entries.forEach { station ->
            // The first field of every row is the abbreviation, so a sample downloaded into the
            // wrong directory is visible in the content rather than only in a file name.
            val firstDataRow = decodePublished(publishedSample(station)).lineSequence().drop(1)

            assertTrue(
                firstDataRow.first().startsWith("${station.abbr};"),
                "the sample at ${station.hourlyNowPath} does not hold ${station.abbr}'s rows",
            )
        }
    }

    @Test
    fun `the published daily recent sample parses one entry per day with every taxon`() {
        val days = PollenCsvParser.parseDaily(publishedDailyRecent(PollenStation.ZUERICH))

        // The verbatim download runs from 1 January to the day before it was taken, one row per
        // published day.
        assertEquals(LocalDate.of(2026, 1, 1), days.keys.min())
        assertEquals(LocalDate.of(2026, 10, 3), days.keys.max())
        assertEquals(257, days.size)
        assertTrue(days.values.all { it.keys == PollenSpecies.entries.toSet() })
        // The publisher leaves days out altogether — 19 of them in this sample, among them most of
        // April — rather than publishing them empty. Filling those dates is the history's job.
        assertTrue(LocalDate.of(2026, 4, 7) !in days)
    }

    @Test
    fun `the daily sample is filed under its own station's daily recent path`() {
        val firstDataRow = decodePublished(publishedDailyRecent(PollenStation.ZUERICH))
            .lineSequence().drop(1).first()

        assertTrue(firstDataRow.startsWith("PZH;"))
    }

    @Test
    fun `daily values are read from the d0 columns and not from d1`() {
        // In the verbatim sample, 2 October 2026 has grasses 0 in `khpoacd0` and 2 in `khpoacd1`.
        val days = PollenCsvParser.parseDaily(publishedDailyRecent(PollenStation.ZUERICH))

        assertEquals(0, days.getValue(LocalDate.of(2026, 10, 2))[PollenSpecies.GRASSES])
    }

    @Test
    fun `a d1 column ahead of its d0 column does not take its place`() {
        val bytes = latin1(
            "station_abbr;reference_timestamp;khpoacd1;khpoacd0",
            "PZH;02.10.2026 00:00;2;0",
        )

        assertEquals(0, PollenCsvParser.parseDaily(bytes).getValue(LocalDate.of(2026, 10, 2))[PollenSpecies.GRASSES])
    }

    @Test
    fun `an empty daily cell reads as no value and not as zero`() {
        val bytes = dailyCsv(
            rows = listOf("02.10.2026" to mapOf(PollenSpecies.ALDER to 0)),
        )

        val day = PollenCsvParser.parseDaily(bytes).getValue(LocalDate.of(2026, 10, 2))

        assertEquals(0, day[PollenSpecies.ALDER])
        assertNull(day[PollenSpecies.GRASSES])
    }

    @Test
    fun `a taxon without a daily column reads as no value`() {
        val bytes = dailyCsv(
            rows = listOf("02.10.2026" to mapOf(PollenSpecies.GRASSES to 5)),
            columns = listOf(PollenSpecies.GRASSES),
        )

        val day = PollenCsvParser.parseDaily(bytes).getValue(LocalDate.of(2026, 10, 2))

        assertEquals(PollenSpecies.entries.toSet(), day.keys)
        assertEquals(5, day[PollenSpecies.GRASSES])
        assertTrue((PollenSpecies.entries - PollenSpecies.GRASSES).all { day[it] == null })
    }

    @Test
    fun `a daily row with a malformed date is skipped`() {
        val bytes = dailyCsv(
            rows = listOf(
                "31.02.2026" to mapOf(PollenSpecies.GRASSES to 5),
                "not a date" to mapOf(PollenSpecies.GRASSES to 6),
                "01.03.2026" to mapOf(PollenSpecies.GRASSES to 7),
            ),
        )

        val days = PollenCsvParser.parseDaily(bytes)

        assertEquals(setOf(LocalDate.of(2026, 3, 1)), days.keys)
    }

    @Test
    fun `a daily row without any value is kept with every taxon null`() {
        val bytes = dailyCsv(rows = listOf("02.10.2026" to emptyMap()))

        val day = PollenCsvParser.parseDaily(bytes).getValue(LocalDate.of(2026, 10, 2))

        assertTrue(day.values.all { it == null })
    }

    @Test
    fun `a daily file in Latin-1 is read without losing its columns`() {
        // The Latin-1 decoding itself is pinned by the first test of this class; this one pins that
        // daily parsing goes through it, with an `ü` that is not valid UTF-8 on its own in a cell.
        val bytes = latin1(
            "station_abbr;reference_timestamp;kabetud0",
            "Münsterlingen;02.10.2026 00:00;12",
        )

        assertEquals(12, PollenCsvParser.parseDaily(bytes).getValue(LocalDate.of(2026, 10, 2))[PollenSpecies.BIRCH])
    }

    @Test
    fun `an empty daily file or one without a timestamp column has no days`() {
        assertTrue(PollenCsvParser.parseDaily(ByteArray(0)).isEmpty())
        assertTrue(PollenCsvParser.parseDaily(latin1("station_abbr;kabetud0", "PZH;12")).isEmpty())
    }

    private fun latin1(vararg lines: String): ByteArray =
        lines.joinToString(separator = "\r\n", postfix = "\r\n").toByteArray(Charsets.ISO_8859_1)

    private fun publishedDailyRecent(station: PollenStation): ByteArray =
        checkNotNull(
            javaClass.classLoader.getResourceAsStream("$PUBLISHED_SAMPLES/${station.dailyRecentPath}"),
        ) { "no checked-in sample at ${station.dailyRecentPath}" }
            .use { it.readBytes() }

    /**
     * A verbatim download of a station's published file, resolved by the same relative path the
     * open-data service serves it under — so a change to [PollenStation.hourlyNowPath] that the
     * fixtures do not follow fails here.
     */
    private fun publishedSample(station: PollenStation): ByteArray =
        checkNotNull(
            javaClass.classLoader.getResourceAsStream("$PUBLISHED_SAMPLES/${station.hourlyNowPath}"),
        ) { "no checked-in sample at ${station.hourlyNowPath}" }
            .use { it.readBytes() }

    private companion object {
        const val PUBLISHED_SAMPLES = "fixtures/ogd-pollen"
    }
}
