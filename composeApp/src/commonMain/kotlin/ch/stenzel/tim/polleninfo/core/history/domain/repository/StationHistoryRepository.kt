package ch.stenzel.tim.polleninfo.core.history.domain.repository

import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory
import ch.stenzel.tim.polleninfo.core.result.Result

interface StationHistoryRepository {

    /** [stationAbbr]'s daily levels over [range], ending yesterday. */
    suspend fun history(stationAbbr: String, range: HistoryRange): Result<StationHistory>
}
