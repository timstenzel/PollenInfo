package ch.stenzel.tim.polleninfo.feature.onboarding

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.onboarding.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.repository.StationRepository

/** Counts calls and replays a scripted result. Hand-written — the project uses no mocking library. */
class FakeStationRepository(
    var result: Result<List<Station>> = Result.Success(stationDtosInServerOrder.toDomain()),
) : StationRepository {

    var callCount: Int = 0
        private set

    override suspend fun getStations(): Result<List<Station>> {
        callCount++
        return result
    }
}
