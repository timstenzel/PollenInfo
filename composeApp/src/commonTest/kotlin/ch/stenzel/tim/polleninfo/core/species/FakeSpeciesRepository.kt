package ch.stenzel.tim.polleninfo.core.species

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.species.domain.repository.SpeciesRepository

/** Counts calls and replays a scripted result. */
class FakeSpeciesRepository(
    var result: Result<List<Species>> = Result.Success(allSpecies),
) : SpeciesRepository {

    var callCount: Int = 0
        private set

    override suspend fun getSpecies(): Result<List<Species>> {
        callCount++
        return result
    }
}
