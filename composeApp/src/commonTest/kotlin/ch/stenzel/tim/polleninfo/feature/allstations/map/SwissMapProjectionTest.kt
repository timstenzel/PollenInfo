package ch.stenzel.tim.polleninfo.feature.allstations.map

import ch.stenzel.tim.polleninfo.core.station.allStations
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class SwissMapProjectionTest {

    private fun projectedBorder(width: Float, height: Float) =
        SWISS_BORDER.map { SwissMapProjection.project(it, width, height) }

    private fun station(abbr: String, width: Float = 1000f, height: Float = 1000f): MapPoint {
        val station = allStations.first { it.abbr == abbr }
        return SwissMapProjection.project(GeoPoint(station.latitude, station.longitude), width, height)
    }

    private fun assertInside(point: MapPoint, width: Float, height: Float, what: String) {
        assertTrue(point.x in 0f..width && point.y in 0f..height, "$what at $point outside $width×$height")
    }

    private fun assertClose(expected: Float, actual: Float, what: String) {
        assertTrue(abs(expected - actual) < 0.01f, "$what: expected $expected, was $actual")
    }

    @Test
    fun `the aspect ratio is plausible for Switzerland`() {
        assertTrue(SwissMapProjection.aspectRatio in 1.4f..1.7f, "was ${SwissMapProjection.aspectRatio}")
    }

    @Test
    fun `on a canvas of the map's shape the border reaches every edge within the margin`() {
        val width = 1550f
        val height = width / SwissMapProjection.aspectRatio
        val border = projectedBorder(width, height)
        val margin = 0.05f

        val left = border.minOf { it.x }
        val right = border.maxOf { it.x }
        val top = border.minOf { it.y }
        val bottom = border.maxOf { it.y }

        assertTrue(left > 0f && left < width * margin, "left edge at $left")
        assertTrue(right < width && right > width * (1 - margin), "right edge at $right")
        assertTrue(top > 0f && top < height * margin, "top edge at $top")
        assertTrue(bottom < height && bottom > height * (1 - margin), "bottom edge at $bottom")
        // The margin is symmetric, so the bounding box is centred.
        assertClose(width - right, left, "horizontal margins")
        assertClose(height - bottom, top, "vertical margins")
    }

    @Test
    fun `on a canvas wider than the map the border fits the height and is centred horizontally`() {
        val width = 2000f
        val height = 500f
        val border = projectedBorder(width, height)

        border.forEach { assertInside(it, width, height, "border point") }
        val left = border.minOf { it.x }
        val right = border.maxOf { it.x }
        assertClose(width - right, left, "horizontal margins")
        // Height is the limiting axis, so the vertical margins are the projection's own small margin.
        assertTrue(border.minOf { it.y } < height * 0.05f)
        assertTrue(left > width * 0.1f, "map should not span the full width, left at $left")
    }

    @Test
    fun `on a canvas taller than the map the border fits the width and is centred vertically`() {
        val width = 500f
        val height = 2000f
        val border = projectedBorder(width, height)

        border.forEach { assertInside(it, width, height, "border point") }
        val top = border.minOf { it.y }
        val bottom = border.maxOf { it.y }
        assertClose(height - bottom, top, "vertical margins")
        assertTrue(border.minOf { it.x } < width * 0.05f)
        assertTrue(top > height * 0.1f, "map should not span the full height, top at $top")
    }

    @Test
    fun `every station projects inside the canvas`() {
        listOf(1000f to 645f, 2000f to 500f, 500f to 2000f, 24f to 24f).forEach { (width, height) ->
            allStations.forEach { station ->
                assertInside(station(station.abbr, width, height), width, height, station.name)
            }
        }
    }

    @Test
    fun `Geneve lies west of Zurich`() {
        assertTrue(station("PGE").x < station("PZH").x)
    }

    @Test
    fun `Lugano lies south of Luzern`() {
        // South is further down the canvas.
        assertTrue(station("PLU").y > station("PLZ").y)
    }
}
