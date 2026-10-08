package ch.stenzel.tim.polleninfo.core.ui.species

import androidx.compose.runtime.Composable
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.species_alder
import ch.stenzel.tim.polleninfo.resources.species_ash
import ch.stenzel.tim.polleninfo.resources.species_beech
import ch.stenzel.tim.polleninfo.resources.species_birch
import ch.stenzel.tim.polleninfo.resources.species_grasses
import ch.stenzel.tim.polleninfo.resources.species_hazel
import ch.stenzel.tim.polleninfo.resources.species_oak
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The name of the species with [id] (the backend's `PollenSpecies` name) in the app's language,
 * or `null` for an id the app has no name for — a species the backend added later. For code
 * outside Compose (`getString`); composables use [speciesName].
 */
fun speciesNameResource(id: String): StringResource? = when (id) {
    "ALDER" -> Res.string.species_alder
    "BIRCH" -> Res.string.species_birch
    "HAZEL" -> Res.string.species_hazel
    "BEECH" -> Res.string.species_beech
    "ASH" -> Res.string.species_ash
    "OAK" -> Res.string.species_oak
    "GRASSES" -> Res.string.species_grasses
    else -> null
}

/**
 * The name of the species with [id] in the app's language through [resolve], or [fallback] — the
 * server's English name — for an id the app does not know, so a new species is still named rather
 * than dropped.
 */
inline fun speciesName(id: String, fallback: String, resolve: (StringResource) -> String): String =
    speciesNameResource(id)?.let { resolve(it) } ?: fallback

/** [speciesName] resolved in the current composition. */
@Composable
fun speciesName(id: String, fallback: String): String = speciesName(id, fallback) { stringResource(it) }
