package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixtures are under `src/test/resources/csv`, written in ISO-8859-1 with CRLF endings exactly as
 * the publisher emits them. No test here contacts MeteoSwiss.
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
    fun `parses the real published file checked in as a sample`() {
        val bytes = checkNotNull(
            javaClass.classLoader.getResourceAsStream(
                "${ClasspathPollenFileSource.ROOT}/pzh/ogd-pollen_pzh_h_now.csv",
            ),
        ).use { it.readBytes() }

        val reading = checkNotNull(PollenCsvParser.parseHourly(bytes))

        // Only that the real column layout maps to all seven taxa — the values themselves are a
        // frozen snapshot and are nobody's business to assert.
        assertEquals(PollenSpecies.entries.toSet(), reading.concentrations.keys)
        assertTrue(reading.concentrations.values.any { it != null })
    }
}
