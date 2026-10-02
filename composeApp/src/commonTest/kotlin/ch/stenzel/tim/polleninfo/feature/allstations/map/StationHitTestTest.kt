package ch.stenzel.tim.polleninfo.feature.allstations.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StationHitTestTest {

    private val radius = 24f

    // Locarno-and-Lugano-like geometry: two dots closer together than twice the radius, so a tap
    // between them is within reach of both.
    private val closePair = mapOf(
        "PLO" to MapPoint(100f, 100f),
        "PLU" to MapPoint(130f, 100f),
    )

    @Test
    fun `a tap exactly on a dot selects it`() {
        assertEquals("PLO", nearestStation(MapPoint(100f, 100f), closePair, radius))
    }

    @Test
    fun `a tap at exactly the radius selects the dot`() {
        val dots = mapOf("PZH" to MapPoint(200f, 200f))

        assertEquals("PZH", nearestStation(MapPoint(200f + radius, 200f), dots, radius))
        assertEquals("PZH", nearestStation(MapPoint(200f, 200f - radius), dots, radius))
    }

    @Test
    fun `a tap just beyond the radius selects nothing`() {
        val dots = mapOf("PZH" to MapPoint(200f, 200f))

        assertNull(nearestStation(MapPoint(200f + radius + 0.01f, 200f), dots, radius))
        assertNull(nearestStation(MapPoint(200f, 200f - radius - 0.01f), dots, radius))
    }

    @Test
    fun `just on Lugano's side of the midpoint selects Lugano`() {
        assertEquals("PLU", nearestStation(MapPoint(115.5f, 100f), closePair, radius))
    }

    @Test
    fun `just on Locarno's side of the midpoint selects Locarno`() {
        assertEquals("PLO", nearestStation(MapPoint(114.5f, 100f), closePair, radius))
    }

    @Test
    fun `an exact tie resolves to the alphabetically first abbreviation`() {
        val midpoint = MapPoint(115f, 100f)

        assertEquals("PLO", nearestStation(midpoint, closePair, radius))
        // Declared the other way round: the answer must not depend on iteration order.
        val reversed = linkedMapOf("PLU" to MapPoint(130f, 100f), "PLO" to MapPoint(100f, 100f))
        assertEquals("PLO", nearestStation(midpoint, reversed, radius))
    }

    @Test
    fun `a tap far from every dot selects nothing`() {
        assertNull(nearestStation(MapPoint(400f, 400f), closePair, radius))
    }

    @Test
    fun `no dots selects nothing`() {
        assertNull(nearestStation(MapPoint(100f, 100f), emptyMap(), radius))
    }
}
