package ch.stenzel.tim.polleninfo.core.ui.palette

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.ui.severity.severityColor
import ch.stenzel.tim.polleninfo.core.ui.species.speciesColor
import ch.stenzel.tim.polleninfo.theme.MeadowColors
import ch.stenzel.tim.polleninfo.theme.MeadowDarkColorScheme
import ch.stenzel.tim.polleninfo.theme.MeadowDarkColors
import ch.stenzel.tim.polleninfo.theme.MeadowLightColorScheme
import ch.stenzel.tim.polleninfo.theme.MeadowLightColors
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every pairing of the Meadow palette that has to stay readable, in both schemes. A failure names
 * every pair below its minimum with the ratio it reached, so a colour change is judged by numbers
 * rather than by eye. Add each new foreground/background pairing a screen introduces here.
 */
class PaletteContrastTest {

    private class Scheme(val name: String, val colors: ColorScheme, val meadow: MeadowColors, val dark: Boolean)

    private val schemes = listOf(
        Scheme("light", MeadowLightColorScheme, MeadowLightColors, dark = false),
        Scheme("dark", MeadowDarkColorScheme, MeadowDarkColors, dark = true),
    )

    private class Pair(val foreground: String, val background: String, val fg: Color, val bg: Color, val minimum: Double)

    private fun Scheme.severityBackgrounds() = listOf(
        "surface" to colors.surface,
        "raised" to colors.surfaceContainerLowest,
        "sunken" to colors.surfaceContainerHighest,
        "background" to colors.background,
        "wash" to meadow.wash,
        "mapLand" to meadow.mapLand,
    )

    private fun Scheme.severityPairs() = PollenSeverity.entries.flatMap { severity ->
        severityBackgrounds().map { (bgName, bg) ->
            Pair("severity $severity", bgName, severityColor(severity, dark), bg, GRAPHICS)
        }
    }

    // The diary chart card is `surface`; on `surfaceContainerLowest` dark Ash would fall to 3.11:1.
    private fun Scheme.speciesPairs() = SPECIES_IDS.map { id ->
        val color = speciesColor(id, dark) ?: fail("no colour for $id")
        Pair("species $id", "chart card (surface)", color, colors.surface, SPECIES_LINE)
    }

    private fun Scheme.textPairs(): List<Pair> {
        val textBackgrounds = listOf(
            "background" to colors.background,
            "surface" to colors.surface,
            "raised" to colors.surfaceContainerLowest,
        )
        val bodyText = listOf("onSurface" to colors.onSurface, "onSurfaceVariant" to colors.onSurfaceVariant)
        return bodyText.flatMap { (fgName, fg) ->
            textBackgrounds.map { (bgName, bg) -> Pair(fgName, bgName, fg, bg, TEXT) }
        } + listOf(
            Pair("onPrimary", "primary", colors.onPrimary, colors.primary, TEXT),
            Pair("onAccent", "accent", meadow.onAccent, meadow.accent, TEXT),
            Pair("onWarningContainer", "warningContainer", meadow.onWarningContainer, meadow.warningContainer, TEXT),
        )
    }

    private fun assertAllMeet(pairs: Scheme.() -> List<Pair>) {
        val failures = schemes.flatMap { scheme ->
            scheme.pairs().mapNotNull { pair ->
                val ratio = contrastRatio(pair.fg, pair.bg)
                if (ratio >= pair.minimum) {
                    null
                } else {
                    "${scheme.name}: ${pair.foreground} on ${pair.background} is ${ratio.rounded()}:1, needs ${pair.minimum}:1"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `every severity colour reaches 3 to 1 on every background it is drawn on`() {
        assertAllMeet { severityPairs() }
    }

    @Test
    fun `every species colour reaches 3_5 to 1 on the chart card`() {
        assertAllMeet { speciesPairs() }
    }

    @Test
    fun `body text and text on primary accent and warning reach 4_5 to 1`() {
        assertAllMeet { textPairs() }
    }

    @Test
    fun `every pair list covers both schemes and all seven species and five severities`() {
        // Guards the tables themselves: an empty list would pass every check above.
        schemes.forEach { scheme ->
            assertTrue(scheme.severityPairs().size == PollenSeverity.entries.size * 6, scheme.name)
            assertTrue(scheme.speciesPairs().size == 7, scheme.name)
            assertTrue(scheme.textPairs().size == 9, scheme.name)
        }
    }

    @Test
    fun `the old light Moderate would fail on the sunken track`() {
        // Pins the boundary from the other side: the value this feature replaced (#B08000) drops below
        // 3:1 on the new track, the very reason it was darkened.
        val ratio = contrastRatio(Color(0xFFB08000), MeadowLightColorScheme.surfaceContainerHighest)

        assertTrue(ratio < GRAPHICS, "was ${ratio.rounded()}:1")
    }

    private fun Double.rounded(): String = ((this * 100).toInt() / 100.0).toString()

    private companion object {
        const val GRAPHICS = 3.0
        const val SPECIES_LINE = 3.5
        const val TEXT = 4.5
        val SPECIES_IDS = listOf("ALDER", "BIRCH", "HAZEL", "BEECH", "ASH", "OAK", "GRASSES")
    }
}
