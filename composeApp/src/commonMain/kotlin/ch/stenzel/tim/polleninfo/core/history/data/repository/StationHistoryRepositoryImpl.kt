package ch.stenzel.tim.polleninfo.core.history.data.repository

import ch.stenzel.tim.polleninfo.core.history.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.core.history.data.mapper.toWireName
import ch.stenzel.tim.polleninfo.core.history.data.remote.StationHistoryApiService
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory
import ch.stenzel.tim.polleninfo.core.history.domain.repository.StationHistoryRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall

class StationHistoryRepositoryImpl(
    private val apiService: StationHistoryApiService,
) : StationHistoryRepository {

    override suspend fun history(stationAbbr: String, range: HistoryRange): Result<StationHistory> =
        safeCall { apiService.getHistory(stationAbbr, range.toWireName()).toDomain() }
}
