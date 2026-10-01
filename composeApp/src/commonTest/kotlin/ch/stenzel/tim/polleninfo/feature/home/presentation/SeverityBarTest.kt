package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeverityBarTest {

    @Test
    fun `each severity fills the bar to its fixed fifth`() {
        assertEquals(
            listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f),
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
