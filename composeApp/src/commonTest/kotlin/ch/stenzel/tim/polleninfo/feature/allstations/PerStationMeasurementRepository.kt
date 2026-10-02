package ch.stenzel.tim.polleninfo.feature.allstations

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.StationMeasurement
import ch.stenzel.tim.polleninfo.core.measurement.domain.repository.StationMeasurementRepository
import ch.stenzel.tim.polleninfo.core.measurement.measurement
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.CompletableDeferred

/**
 * A measurement repository scripted **per station**, which the single-result
 * `FakeStationMeasurementRepository` cannot be: the All stations screen is about one station
 * failing or lagging while the others do not.
 *
 * A station with no entry in [results] answers successfully with a reading for its own
 * abbreviation. A station with an entry in [gates] suspends until that gate is completed; [gateAll]
 * holds every station that has no gate of its own. Results are read after the gate opens.
 */
class PerStationMeasurementRepository : StationMeasurementRepository {

    val results = mutableMapOf<String, Result<StationMeasurement>>()
    val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
    var gateAll: CompletableDeferred<Unit>? = null

    val requested = mutableListOf<String>()

    fun failFor(vararg abbrs: String) {
        abbrs.forEach { results[it] = Result.Failure(RuntimeException("upstream failed for $it")) }
    }

    override suspend fun getMeasurement(stationAbbr: String): Result<StationMeasurement> {
        requested += stationAbbr
        (gates[stationAbbr] ?: gateAll)?.await()
        return results[stationAbbr] ?: Result.Success(measurement(stationAbbr = stationAbbr))
    }
}
