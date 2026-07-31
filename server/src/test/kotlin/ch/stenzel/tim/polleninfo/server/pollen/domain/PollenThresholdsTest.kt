package ch.stenzel.tim.polleninfo.server.pollen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeciesThresholdsTest {

    private val thresholds = SpeciesThresholds(moderate = 15, high = 90, veryHigh = 1500)

    @Test
    fun `zero and negative concentrations are NONE`() {
        assertEquals(PollenSeverity.NONE, thresholds.severityOf(0))
        assertEquals(PollenSeverity.NONE, thresholds.severityOf(-1))
    }

    @Test
    fun `one grain is already LOW`() {
        assertEquals(PollenSeverity.LOW, thresholds.severityOf(1))
    }

    @Test
    fun `each band is inclusive at its lower bound and exclusive at the next`() {
        assertEquals(PollenSeverity.LOW, thresholds.severityOf(14))
        assertEquals(PollenSeverity.MODERATE, thresholds.severityOf(15))
        assertEquals(PollenSeverity.MODERATE, thresholds.severityOf(89))
        assertEquals(PollenSeverity.HIGH, thresholds.severityOf(90))
        assertEquals(PollenSeverity.HIGH, thresholds.severityOf(1499))
        assertEquals(PollenSeverity.VERY_HIGH, thresholds.severityOf(1500))
    }

    @Test
    fun `very large concentrations stay VERY_HIGH`() {
        assertEquals(PollenSeverity.VERY_HIGH, thresholds.severityOf(Int.MAX_VALUE))
    }

    @Test
    fun `bounds must be strictly increasing`() {
        assertFailsWith<IllegalArgumentException> {
            SpeciesThresholds(moderate = 90, high = 90, veryHigh = 1500)
        }
        assertFailsWith<IllegalArgumentException> {
            SpeciesThresholds(moderate = 100, high = 90, veryHigh = 1500)
        }
        assertFailsWith<IllegalArgumentException> {
            SpeciesThresholds(moderate = 15, high = 1500, veryHigh = 90)
        }
    }

    @Test
    fun `the moderate bound must leave room for a LOW band`() {
        assertFailsWith<IllegalArgumentException> {
            SpeciesThresholds(moderate = 0, high = 90, veryHigh = 1500)
        }
    }
}

class PollenThresholdsTest {

    private val thresholds = PollenThresholds()

    @Test
    fun `every species has thresholds configured`() {
        PollenSpecies.entries.forEach { species ->
            // Must not throw.
            thresholds.forSpecies(species)
        }
        assertEquals(PollenSpecies.entries.size, thresholds.asMap().size)
    }

    @Test
    fun `construction fails when a species is left unconfigured`() {
        val incomplete = PollenThresholds.DEFAULTS - PollenSpecies.GRASSES

        val error = assertFailsWith<IllegalArgumentException> { PollenThresholds(incomplete) }

        assertTrue(
            error.message.orEmpty().contains("GRASSES"),
            "error should name the missing species, was: ${error.message}",
        )
    }

    @Test
    fun `thresholds are per species, so the same concentration classifies differently`() {
        // 20 grains/m3 is only LOW for a tree but already HIGH for grasses.
        assertEquals(PollenSeverity.MODERATE, thresholds.severityOf(PollenSpecies.BIRCH, 20))
        assertEquals(PollenSeverity.HIGH, thresholds.severityOf(PollenSpecies.GRASSES, 20))
    }

    @Test
    fun `grasses reach every band at lower concentrations than trees`() {
        val grass = thresholds.forSpecies(PollenSpecies.GRASSES)
        val birch = thresholds.forSpecies(PollenSpecies.BIRCH)

        assertTrue(grass.moderate < birch.moderate)
        assertTrue(grass.high < birch.high)
        assertTrue(grass.veryHigh < birch.veryHigh)
    }

    @Test
    fun `the six tree taxa share the seeded tree bounds`() {
        val trees = listOf(
            PollenSpecies.ALDER,
            PollenSpecies.BIRCH,
            PollenSpecies.HAZEL,
            PollenSpecies.BEECH,
            PollenSpecies.ASH,
            PollenSpecies.OAK,
        )

        trees.forEach { species ->
            assertEquals(PollenThresholds.TREE, thresholds.forSpecies(species), "for $species")
        }
    }

    @Test
    fun `grasses use the dedicated grass bounds`() {
        assertEquals(PollenThresholds.GRASS, thresholds.forSpecies(PollenSpecies.GRASSES))
    }

    @Test
    fun `seeded grass bounds match the Swiss exposure classes`() {
        assertEquals(SpeciesThresholds(moderate = 5, high = 20, veryHigh = 200), PollenThresholds.GRASS)
    }

    @Test
    fun `seeded tree bounds match the Swiss exposure classes`() {
        assertEquals(SpeciesThresholds(moderate = 15, high = 90, veryHigh = 1500), PollenThresholds.TREE)
    }

    @Test
    fun `a single species can be retuned without affecting the others`() {
        val custom = PollenThresholds(
            PollenThresholds.DEFAULTS + (PollenSpecies.BIRCH to SpeciesThresholds(3, 10, 40)),
        )

        assertEquals(PollenSeverity.HIGH, custom.severityOf(PollenSpecies.BIRCH, 12))
        assertEquals(PollenSeverity.LOW, custom.severityOf(PollenSpecies.OAK, 12))
    }

    @Test
    fun `asMap is a snapshot that callers cannot use to mutate the configuration`() {
        val snapshot = thresholds.asMap() as MutableMap<PollenSpecies, SpeciesThresholds>
        snapshot[PollenSpecies.BIRCH] = SpeciesThresholds(1, 2, 3)

        assertEquals(PollenThresholds.TREE, thresholds.forSpecies(PollenSpecies.BIRCH))
    }
}

class PollenSeverityTest {

    @Test
    fun `severities are ordered from NONE to VERY_HIGH`() {
        assertEquals(
            listOf(
                PollenSeverity.NONE,
                PollenSeverity.LOW,
                PollenSeverity.MODERATE,
                PollenSeverity.HIGH,
                PollenSeverity.VERY_HIGH,
            ),
            PollenSeverity.entries.toList(),
        )
    }

    @Test
    fun `atLeast expresses a user's minimum notification severity`() {
        assertTrue(PollenSeverity.VERY_HIGH.atLeast(PollenSeverity.HIGH))
        assertTrue(PollenSeverity.HIGH.atLeast(PollenSeverity.HIGH))
        assertFalse(PollenSeverity.MODERATE.atLeast(PollenSeverity.HIGH))
        assertFalse(PollenSeverity.NONE.atLeast(PollenSeverity.LOW))
    }
}
