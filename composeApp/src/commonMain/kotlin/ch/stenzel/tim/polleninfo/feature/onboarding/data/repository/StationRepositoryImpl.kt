package ch.stenzel.tim.polleninfo.feature.onboarding.data.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.feature.onboarding.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.onboarding.data.remote.StationApiService
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.repository.StationRepository

class StationRepositoryImpl(
    private val apiService: StationApiService,
) : StationRepository {

    override suspend fun getStations(): Result<List<Station>> = safeCall {
        apiService.getStations().toDomain()
    }
}
