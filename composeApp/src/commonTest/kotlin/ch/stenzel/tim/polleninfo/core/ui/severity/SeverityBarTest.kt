package ch.stenzel.tim.polleninfo.core.ui.severity

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeverityBarTest {

    @Test
    fun `each severity fills the bar to its fixed stop`() {
        assertEquals(
            listOf(0.04f, 0.25f, 0.5f, 0.75f, 1.0f),
            PollenSeverity.entries.map(::severityFillFraction),
        )
    }

    @Test
    fun `a taxon with no reading fills nothing`() {
        assertEquals(0f, severityFillFraction(null))
    }

    @Test
    fun `the calmest real severity is still visibly filled`() {
        // An empty bar is reserved for "no reading"; NONE must not be confused with it.
        assertTrue(severityFillFraction(PollenSeverity.NONE) > severityFillFraction(null))
    }
}
