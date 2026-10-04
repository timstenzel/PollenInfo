package ch.stenzel.tim.polleninfo.core.ui.species

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ch.stenzel.tim.polleninfo.theme.speciesAlderDark
import ch.stenzel.tim.polleninfo.theme.speciesAlderLight
import ch.stenzel.tim.polleninfo.theme.speciesAshDark
import ch.stenzel.tim.polleninfo.theme.speciesAshLight
import ch.stenzel.tim.polleninfo.theme.speciesBeechDark
import ch.stenzel.tim.polleninfo.theme.speciesBeechLight
import ch.stenzel.tim.polleninfo.theme.speciesBirchDark
import ch.stenzel.tim.polleninfo.theme.speciesBirchLight
import ch.stenzel.tim.polleninfo.theme.speciesGrassesDark
import ch.stenzel.tim.polleninfo.theme.speciesGrassesLight
import ch.stenzel.tim.polleninfo.theme.speciesHazelDark
import ch.stenzel.tim.polleninfo.theme.speciesHazelLight
import ch.stenzel.tim.polleninfo.theme.speciesOakDark
import ch.stenzel.tim.polleninfo.theme.speciesOakLight

/**
 * The line colour of the species with [id] (the backend's `PollenSpecies` name) in a light or dark
 * scheme, or `null` for an id the app has no colour for — a species the backend added later. The
 * caller decides how to show that; it is never silently given another species' colour.
 */
fun speciesColor(id: String, darkTheme: Boolean): Color? = when (id) {
    "ALDER" -> if (darkTheme) speciesAlderDark else speciesAlderLight
    "BIRCH" -> if (darkTheme) speciesBirchDark else speciesBirchLight
    "HAZEL" -> if (darkTheme) speciesHazelDark else speciesHazelLight
    "BEECH" -> if (darkTheme) speciesBeechDark else speciesBeechLight
    "ASH" -> if (darkTheme) speciesAshDark else speciesAshLight
    "OAK" -> if (darkTheme) speciesOakDark else speciesOakLight
    "GRASSES" -> if (darkTheme) speciesGrassesDark else speciesGrassesLight
    else -> null
}

/** [speciesColor] for the scheme in effect, judged by the same surface rule as `PollenSeverity.color()`. */
@Composable
fun speciesColor(id: String): Color? =
    speciesColor(id, darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f)
