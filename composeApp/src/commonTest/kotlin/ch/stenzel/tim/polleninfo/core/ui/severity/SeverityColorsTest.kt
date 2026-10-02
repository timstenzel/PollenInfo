package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.ui.graphics.Color
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SeverityColorsTest {

    @Test
    fun `light theme maps grey green amber orange red from calmest to worst`() {
        assertEquals(
            listOf(
                Color(0xFF6B6B60),
                Color(0xFF2E7D32),
                Color(0xFFB08000),
                Color(0xFFB4500F),
                Color(0xFFB3261E),
            ),
            PollenSeverity.entries.map { severityColor(it, darkTheme = false) },
        )
    }

    @Test
    fun `dark theme maps grey green amber orange red from calmest to worst`() {
        assertEquals(
            listOf(
                Color(0xFF9C9C90),
                Color(0xFF7DD87F),
                Color(0xFFF0B429),
                Color(0xFFF08135),
                Color(0xFFF2564B),
            ),
            PollenSeverity.entries.map { severityColor(it, darkTheme = true) },
        )
    }

    @Test
    fun `every severity has its own colour in each theme`() {
        for (dark in listOf(false, true)) {
            val colors = PollenSeverity.entries.map { severityColor(it, dark) }
            assertEquals(colors.size, colors.toSet().size, "dark = $dark")
        }
    }

    @Test
    fun `no severity reuses its light colour in the dark theme`() {
        // A light-tuned colour on the dark surface is what the explicit dark variants exist to avoid.
        PollenSeverity.entries.forEach {
            assertNotEquals(severityColor(it, darkTheme = false), severityColor(it, darkTheme = true), "$it")
        }
    }
}
