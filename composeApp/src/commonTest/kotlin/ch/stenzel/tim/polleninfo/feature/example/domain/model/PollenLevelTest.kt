package ch.stenzel.tim.polleninfo.feature.example.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Boundary coverage for the concentration -> [PollenLevel] classification.
 *
 * The bucket edges are the contract the push-notification threshold logic will be built on,
 * so every boundary is pinned from both sides.
 */
class PollenLevelTest {

    @Test
    fun `null concentration is NONE`() {
        assertEquals(PollenLevel.NONE, null.toPollenLevel())
    }

    @Test
    fun `zero and negative concentrations are NONE`() {
        assertEquals(PollenLevel.NONE, 0f.toPollenLevel())
        assertEquals(PollenLevel.NONE, (-1f).toPollenLevel())
        assertEquals(PollenLevel.NONE, (-0.0001f).toPollenLevel())
    }

    @Test
    fun `any value above zero and below ten is LOW`() {
        assertEquals(PollenLevel.LOW, 0.1f.toPollenLevel())
        assertEquals(PollenLevel.LOW, 5f.toPollenLevel())
        assertEquals(PollenLevel.LOW, 9.999f.toPollenLevel())
    }

    @Test
    fun `ten is the lower bound of MODERATE`() {
        assertEquals(PollenLevel.MODERATE, 10f.toPollenLevel())
        assertEquals(PollenLevel.MODERATE, 49.999f.toPollenLevel())
    }

    @Test
    fun `fifty is the lower bound of HIGH`() {
        assertEquals(PollenLevel.HIGH, 50f.toPollenLevel())
        assertEquals(PollenLevel.HIGH, 199.999f.toPollenLevel())
    }

    @Test
    fun `two hundred is the lower bound of VERY_HIGH`() {
        assertEquals(PollenLevel.VERY_HIGH, 200f.toPollenLevel())
        assertEquals(PollenLevel.VERY_HIGH, 10_000f.toPollenLevel())
    }

    @Test
    fun `levels are ordered from NONE to VERY_HIGH so they can be compared against a threshold`() {
        assertEquals(
            listOf(
                PollenLevel.NONE,
                PollenLevel.LOW,
                PollenLevel.MODERATE,
                PollenLevel.HIGH,
                PollenLevel.VERY_HIGH,
            ),
            PollenLevel.entries.toList(),
        )
    }
}
