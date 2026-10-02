package ch.stenzel.tim.polleninfo.core.station.data.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.core.station.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.core.station.data.remote.StationApiService
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository

class StationRepositoryImpl(
    private val apiService: StationApiService,
) : StationRepository {

    override suspend fun getStations(): Result<List<Station>> = safeCall {
        apiService.getStations().toDomain()
    }
}
