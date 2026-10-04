package ch.stenzel.tim.polleninfo.core.species.domain.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species

interface SpeciesRepository {

    /** Every pollen type the backend measures, in the backend's order. */
    suspend fun getSpecies(): Result<List<Species>>
}
