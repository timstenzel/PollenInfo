package ch.stenzel.tim.polleninfo.feature.allstations.map

import ch.stenzel.tim.polleninfo.core.station.allStations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwissBorderTest {

    @Test
    fun `the ring is closed`() {
        assertEquals(SWISS_BORDER.first(), SWISS_BORDER.last())
    }

    @Test
    fun `the ring has between 150 and 300 points`() {
        assertTrue(SWISS_BORDER.size in 150..300, "had ${SWISS_BORDER.size} points")
    }

    // A mirrored, swapped (lat/lon) or wrong-country dataset fails here before anyone looks at a map.
    @Test
    fun `every station lies inside the border`() {
        allStations.forEach { station ->
            assertTrue(
                contains(SWISS_BORDER, GeoPoint(station.latitude, station.longitude)),
                "${station.name} is outside the border",
            )
        }
    }

    @Test
    fun `a point outside Switzerland is not inside the border`() {
        val milan = GeoPoint(45.4642, 9.1900)
        val munich = GeoPoint(48.1351, 11.5820)

        assertTrue(!contains(SWISS_BORDER, milan))
        assertTrue(!contains(SWISS_BORDER, munich))
    }

    /** Even-odd ray casting; the test's own, so the assertion does not trust production geometry. */
    private fun contains(ring: List<GeoPoint>, point: GeoPoint): Boolean {
        var inside = false
        for (i in 0 until ring.size - 1) {
            val a = ring[i]
            val b = ring[i + 1]
            if ((a.latitude > point.latitude) != (b.latitude > point.latitude)) {
                val crossing = a.longitude +
                    (b.longitude - a.longitude) * (point.latitude - a.latitude) / (b.latitude - a.latitude)
                if (point.longitude < crossing) inside = !inside
            }
        }
        return inside
    }
}
