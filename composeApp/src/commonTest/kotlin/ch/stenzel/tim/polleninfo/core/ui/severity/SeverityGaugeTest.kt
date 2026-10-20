package ch.stenzel.tim.polleninfo.core.ui.severity

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeverityGaugeTest {

    @Test
    fun `each severity fills its number of segments`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            PollenSeverity.entries.map(::gaugeFilledSegments),
        )
    }

    @Test
    fun `no reading fills no segment`() {
        assertEquals(0, gaugeFilledSegments(null))
    }

    @Test
    fun `the worst severity fills the whole gauge and nothing fills more`() {
        assertEquals(GAUGE_SEGMENTS, gaugeFilledSegments(PollenSeverity.VERY_HIGH))
        assertTrue(PollenSeverity.entries.all { gaugeFilledSegments(it) in 0..GAUGE_SEGMENTS })
    }

    @Test
    fun `the filling never decreases as the severity rises`() {
        val filled = listOf(gaugeFilledSegments(null)) + PollenSeverity.entries.map(::gaugeFilledSegments)

        assertEquals(filled.sorted(), filled)
    }

    @Test
    fun `every severity above None fills one segment more than the one below`() {
        // Pins the other side of the None boundary: Low is the first severity that fills anything.
        PollenSeverity.entries.zipWithNext().forEach { (lower, higher) ->
            assertEquals(gaugeFilledSegments(lower) + 1, gaugeFilledSegments(higher), "$lower to $higher")
        }
    }
}
