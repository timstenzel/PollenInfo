package ch.stenzel.tim.polleninfo.core.ui.palette

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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

    // The pill buttons and dialogs: Text/Outline labels in `primary` and Danger labels in `error`, on
    // a screen (`background`) and in a dialog (raised); Tonal labels; the dialog's icon badges; the
    // back arrow on the glass circle, the glass composited over the screen it floats on.
    private fun Scheme.componentPairs(): List<Pair> {
        val glass = meadow.glass.compositeOver(colors.background)
        return listOf(
            Pair("primary label", "background", colors.primary, colors.background, TEXT),
            Pair("primary label", "raised", colors.primary, colors.surfaceContainerLowest, TEXT),
            Pair("error label", "background", colors.error, colors.background, TEXT),
            Pair("error label", "raised", colors.error, colors.surfaceContainerLowest, TEXT),
            Pair("onPrimaryContainer", "primaryContainer", colors.onPrimaryContainer, colors.primaryContainer, TEXT),
            Pair("error icon", "warningContainer", colors.error, meadow.warningContainer, GRAPHICS),
            Pair("back arrow (onSurface)", "glass over background", colors.onSurface, glass, GRAPHICS),
        )
    }

    // Home's feeling card (green `primary`): its dimmed "Not today" and the faces on their tinted
    // circles, alphas as in `FeelingPrompt`. Home's other pairs are above — the reading-time pill
    // (onPrimaryContainer), the stale pill and banner (warning), the gauge word (onSurface on raised);
    // the gauge's unfilled segments are decorative.
    private fun Scheme.homePairs(): List<Pair> {
        val faceCircle = colors.onPrimary.copy(alpha = 0.14f).compositeOver(colors.primary)
        return listOf(
            Pair("Not today (onPrimary 75 %)", "primary", colors.onPrimary.copy(alpha = 0.75f).compositeOver(colors.primary), colors.primary, TEXT),
            Pair("face (onPrimary)", "face circle", colors.onPrimary, faceCircle, GRAPHICS),
        )
    }

    // All stations: the selected row is `primaryContainer`, so its severity dot and its text sit on
    // it; the outdated-reading icon sits on a row of either kind; the map's selected tag is
    // onPrimary on primary (above). The grey "no level" dot and the map's border and halo are
    // decorative — the row's words carry them.
    private fun Scheme.allStationsPairs(): List<Pair> {
        val selectedRow = colors.primaryContainer
        return PollenSeverity.entries.map { severity ->
            Pair("severity $severity", "selected row (primaryContainer)", severityColor(severity, dark), selectedRow, GRAPHICS)
        } + listOf(
            Pair("onSurface", "selected row (primaryContainer)", colors.onSurface, selectedRow, TEXT),
            Pair("onSurfaceVariant", "selected row (primaryContainer)", colors.onSurfaceVariant, selectedRow, TEXT),
            Pair("stale icon (onWarningContainer)", "raised", meadow.onWarningContainer, colors.surfaceContainerLowest, GRAPHICS),
            Pair("stale icon (onWarningContainer)", "selected row (primaryContainer)", meadow.onWarningContainer, selectedRow, GRAPHICS),
        )
    }

    // The Diary: a checked chip is raised with its edge, box and the box's raised tick in the species
    // colour; an unchecked chip's box edge sits on the screen. The dashed feeling line is `primary`
    // on the chart card, held to the species lines' 3.5:1. The no-answers hint and the range toggle's
    // unselected words sit on the sunken colour. The gate's check badges are onPrimaryContainer on
    // primaryContainer (above); the chart's dotted grid is decorative.
    private fun Scheme.diaryPairs(): List<Pair> = SPECIES_IDS.flatMap { id ->
        val color = speciesColor(id, dark) ?: fail("no colour for $id")
        listOf(
            Pair("species $id", "checked chip (raised)", color, colors.surfaceContainerLowest, GRAPHICS),
            Pair("species $id", "background", color, colors.background, GRAPHICS),
        )
    } + listOf(
        Pair("feeling line (primary)", "chart card (surface)", colors.primary, colors.surface, SPECIES_LINE),
        Pair("onSurface", "sunken", colors.onSurface, colors.surfaceContainerHighest, TEXT),
    )

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
    fun `button labels dialog badges and the back arrow are readable`() {
        assertAllMeet { componentPairs() }
    }

    @Test
    fun `the feeling card on Home is readable`() {
        assertAllMeet { homePairs() }
    }

    @Test
    fun `the station rows on All stations are readable`() {
        assertAllMeet { allStationsPairs() }
    }

    @Test
    fun `the chips chart and controls of the Diary are readable`() {
        assertAllMeet { diaryPairs() }
    }

    @Test
    fun `every pair list covers both schemes and all seven species and five severities`() {
        // Guards the tables themselves: an empty list would pass every check above.
        schemes.forEach { scheme ->
            assertTrue(scheme.severityPairs().size == PollenSeverity.entries.size * 6, scheme.name)
            assertTrue(scheme.speciesPairs().size == 7, scheme.name)
            assertTrue(scheme.textPairs().size == 9, scheme.name)
            assertTrue(scheme.componentPairs().size == 7, scheme.name)
            assertTrue(scheme.homePairs().size == 2, scheme.name)
            assertTrue(scheme.allStationsPairs().size == PollenSeverity.entries.size + 4, scheme.name)
            assertTrue(scheme.diaryPairs().size == 7 * 2 + 2, scheme.name)
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
