package ch.stenzel.tim.polleninfo.server.pollen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PollenSpeciesTest {

    @Test
    fun `MeteoSwiss measures exactly seven taxa`() {
        assertEquals(7, PollenSpecies.entries.size)
    }

    @Test
    fun `the seven taxa are the SwissPollen set`() {
        assertEquals(
            setOf("Alder", "Ash", "Beech", "Birch", "Grasses", "Hazel", "Oak"),
            PollenSpecies.entries.map { it.displayName }.toSet(),
        )
    }

    @Test
    fun `hourly parameter codes match the OGD column headers`() {
        assertEquals("kaalnuh0", PollenSpecies.ALDER.hourlyCode)
        assertEquals("kabetuh0", PollenSpecies.BIRCH.hourlyCode)
        assertEquals("kacoryh0", PollenSpecies.HAZEL.hourlyCode)
        assertEquals("kafaguh0", PollenSpecies.BEECH.hourlyCode)
        assertEquals("kafraxh0", PollenSpecies.ASH.hourlyCode)
        assertEquals("kaquerh0", PollenSpecies.OAK.hourlyCode)
        assertEquals("khpoach0", PollenSpecies.GRASSES.hourlyCode)
    }

    @Test
    fun `daily codes are the d0 variant of the hourly codes`() {
        PollenSpecies.entries.forEach { species ->
            assertEquals(
                species.hourlyCode.dropLast(2) + "d0",
                species.dailyCode,
                "for $species",
            )
        }
    }

    @Test
    fun `parameter codes are unique across species and granularities`() {
        val codes = PollenSpecies.entries.flatMap { listOf(it.hourlyCode, it.dailyCode) }

        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `fromHourlyCode resolves a CSV column header`() {
        assertEquals(PollenSpecies.GRASSES, PollenSpecies.fromHourlyCode("khpoach0"))
        assertEquals(PollenSpecies.BIRCH, PollenSpecies.fromHourlyCode("kabetuh0"))
    }

    @Test
    fun `fromHourlyCode returns null for non-pollen columns`() {
        assertNull(PollenSpecies.fromHourlyCode("station_abbr"))
        assertNull(PollenSpecies.fromHourlyCode("reference_timestamp"))
        assertNull(PollenSpecies.fromHourlyCode("kabetud0"), "daily code is not an hourly code")
    }

    @Test
    fun `fromDailyCode resolves a daily CSV column header`() {
        assertEquals(PollenSpecies.OAK, PollenSpecies.fromDailyCode("kaquerd0"))
        assertNull(PollenSpecies.fromDailyCode("kaquerh0"))
    }

    @Test
    fun `latin names are populated and distinct`() {
        val latin = PollenSpecies.entries.map { it.latinName }

        assertTrue(latin.all { it.isNotBlank() })
        assertEquals(latin.size, latin.toSet().size)
        assertEquals("Poaceae", PollenSpecies.GRASSES.latinName)
        assertEquals("Betula", PollenSpecies.BIRCH.latinName)
    }

    @Test
    fun `no taxa outside the MeteoSwiss set leaked into the enum`() {
        // MeteoSwiss does not publish mugwort, olive or ragweed for these stations, so these must
        // never appear here even though other pollen providers report them.
        val names = PollenSpecies.entries.map { it.name }

        assertTrue(names.none { it in setOf("MUGWORT", "OLIVE", "RAGWEED") }, "was: $names")
    }
}
