package ch.stenzel.tim.polleninfo.server.pollen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PollenStationTest {

    @Test
    fun `the network has exactly fifteen stations`() {
        assertEquals(15, PollenStation.entries.size)
    }

    @Test
    fun `station abbreviations are unique and match the official four codes`() {
        val abbrs = PollenStation.entries.map { it.abbr }

        assertEquals(abbrs.size, abbrs.toSet().size, "abbreviations must be unique")
        assertEquals(
            setOf(
                "PBE", "PBS", "PBU", "PCF", "PDS", "PGE", "PLO", "PLS",
                "PLU", "PLZ", "PMU", "PNE", "PPY", "PSN", "PZH",
            ),
            abbrs.toSet(),
        )
    }

    @Test
    fun `fromAbbr resolves an official abbreviation`() {
        assertEquals(PollenStation.ZUERICH, PollenStation.fromAbbr("PZH"))
        assertEquals(PollenStation.BERN, PollenStation.fromAbbr("PBE"))
    }

    @Test
    fun `fromAbbr is case insensitive`() {
        assertEquals(PollenStation.ZUERICH, PollenStation.fromAbbr("pzh"))
        assertEquals(PollenStation.LUGANO, PollenStation.fromAbbr("pLu"))
    }

    @Test
    fun `fromAbbr returns null for an unknown abbreviation`() {
        assertNull(PollenStation.fromAbbr("XXX"))
        assertNull(PollenStation.fromAbbr(""))
    }

    @Test
    fun `download paths follow the MeteoSwiss naming scheme`() {
        assertEquals("pzh/ogd-pollen_pzh_h_now.csv", PollenStation.ZUERICH.hourlyNowPath)
        assertEquals("pzh/ogd-pollen_pzh_h_recent.csv", PollenStation.ZUERICH.hourlyRecentPath)
        assertEquals("pzh/ogd-pollen_pzh_d_recent.csv", PollenStation.ZUERICH.dailyRecentPath)
        assertEquals("pzh/ogd-pollen_pzh_d_historical.csv", PollenStation.ZUERICH.dailyHistoricalPath)
    }

    @Test
    fun `every station yields a lowercased hourly path`() {
        PollenStation.entries.forEach { station ->
            val id = station.abbr.lowercase()
            assertEquals("$id/ogd-pollen_${id}_h_now.csv", station.hourlyNowPath)
        }
    }

    @Test
    fun `all coordinates fall inside Switzerland`() {
        PollenStation.entries.forEach { station ->
            assertTrue(
                station.latitude in 45.8..47.9,
                "${station.abbr} latitude out of range: ${station.latitude}",
            )
            assertTrue(
                station.longitude in 5.9..10.6,
                "${station.abbr} longitude out of range: ${station.longitude}",
            )
            assertTrue(
                station.altitudeMasl in 190..2000,
                "${station.abbr} altitude out of range: ${station.altitudeMasl}",
            )
        }
    }

    @Test
    fun `station metadata is populated`() {
        PollenStation.entries.forEach { station ->
            assertTrue(station.displayName.isNotBlank(), "${station.abbr} has no name")
            assertEquals(2, station.canton.length, "${station.abbr} canton should be a 2-letter code")
        }
    }

    @Test
    fun `Davos is the highest station and Basel the lowest`() {
        assertEquals(PollenStation.DAVOS, PollenStation.entries.maxBy { it.altitudeMasl })
        assertEquals(PollenStation.BASEL, PollenStation.entries.minBy { it.altitudeMasl })
    }
}
