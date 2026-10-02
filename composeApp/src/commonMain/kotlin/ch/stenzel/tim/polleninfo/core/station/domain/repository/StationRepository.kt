package ch.stenzel.tim.polleninfo.core.station.domain.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station

interface StationRepository {

    /** All measuring stations, alphabetically by display name. */
    suspend fun getStations(): Result<List<Station>>
}
