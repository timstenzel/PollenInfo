package ch.stenzel.tim.polleninfo.feature.allstations.map

import ch.stenzel.tim.polleninfo.core.station.allStations
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwissLakesTest {

    @Test
    fun `every lake is a closed ring`() {
        SWISS_LAKES.forEach { lake ->
            assertTrue(lake.outline.size >= 4, "${lake.name} has ${lake.outline.size} points")
            assertEquals(lake.outline.first(), lake.outline.last(), "${lake.name} is not closed")
        }
    }

    // The lakes exist to make the dots easier to place, so pin the pairings that do that. A swapped
    // lat/lon or a lake filed under the wrong name puts the city tens of kilometres away.
    @Test
    fun `the stations on a lakeshore lie next to their lake`() {
        mapOf(
            "PGE" to "Lac Léman",
            "PLS" to "Lac Léman",
            "PNE" to "Lac de Neuchâtel",
            "PLZ" to "Vierwaldstättersee",
            "PZH" to "Zürichsee",
            "PLU" to "Lago di Lugano",
        ).forEach { (abbr, lakeName) ->
            val station = allStations.first { it.abbr == abbr }
            val lake = SWISS_LAKES.first { it.name == lakeName }
            val km = lake.outline.minOf { distanceKm(it, GeoPoint(station.latitude, station.longitude)) }
            assertTrue(km < MAX_SHORE_DISTANCE_KM, "${station.name} is $km km from $lakeName")
        }
    }

    /** Flat-earth distance — accurate to well under a percent over a few kilometres. */
    private fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val kmPerDegree = 111.2
        val dx = (a.longitude - b.longitude) * cos(46.8 * PI / 180) * kmPerDegree
        val dy = (a.latitude - b.latitude) * kmPerDegree
        return hypot(dx, dy)
    }

    private companion object {
        /** The outlines are simplified, so a shore can be a few kilometres from the nearest vertex. */
        const val MAX_SHORE_DISTANCE_KM = 5.0
    }
}
