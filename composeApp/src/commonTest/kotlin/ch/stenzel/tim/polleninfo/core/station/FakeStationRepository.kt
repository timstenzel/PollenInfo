package ch.stenzel.tim.polleninfo.core.station

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository

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
