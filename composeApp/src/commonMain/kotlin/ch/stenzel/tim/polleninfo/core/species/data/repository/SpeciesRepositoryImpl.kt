package ch.stenzel.tim.polleninfo.core.species.data.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.core.species.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.core.species.data.remote.SpeciesApiService
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.species.domain.repository.SpeciesRepository

class SpeciesRepositoryImpl(
    private val apiService: SpeciesApiService,
) : SpeciesRepository {

    override suspend fun getSpecies(): Result<List<Species>> = safeCall {
        apiService.getSpecies().toDomain()
    }
}
