package ch.stenzel.tim.polleninfo.core.species.data.mapper

import ch.stenzel.tim.polleninfo.core.species.data.remote.dto.SpeciesDto
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species

/**
 * Keeps the server's order — `PollenSpecies` declaration order, the same order every reading lists
 * its taxa in — so the editor's chips match the rest of the app. The latin name is dropped: nothing
 * that needs the vocabulary without a reading shows it.
 */
fun List<SpeciesDto>.toDomain(): List<Species> = map { it.toDomain() }

fun SpeciesDto.toDomain(): Species = Species(id = id, name = name)
