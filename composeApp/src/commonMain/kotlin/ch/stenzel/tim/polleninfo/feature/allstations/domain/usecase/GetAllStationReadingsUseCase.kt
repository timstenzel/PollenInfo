package ch.stenzel.tim.polleninfo.feature.allstations.domain.usecase

import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reads every station at once, one backend request each.
 *
 * There is no batch endpoint, so the fan-out happens here. The backend caches each station's reading
 * for 30 minutes, so this costs at most one upstream request per station per period however many
 * people open the screen. A batch endpoint could later replace the fan-out behind this same
 * signature without the screen noticing.
 *
 * The overall severity, its driver and the taxa order come from [GetStationMeasurementUseCase]
 * unchanged — Home and this screen must never derive them two ways.
 */
class GetAllStationReadingsUseCase(
    private val getStationMeasurement: GetStationMeasurementUseCase,
) {

    /**
     * Emits the full list first with every station [StationReading.Pending], then again after each
     * station resolves, so each row fills in as soon as its own answer arrives. Completes once every
     * station has resolved.
     *
     * Alphabetical by name and never reordered between emissions. The repository already returns
     * that order; it is applied again here so the rule belongs to this screen's logic rather than
     * to whatever order a data source happens to use.
     */
    operator fun invoke(stations: List<Station>): Flow<List<StationReading>> = channelFlow {
        val ordered = stations.sortedBy { it.name }
        val readings = ordered.map<Station, StationReading> { StationReading.Pending(it) }.toMutableList()
        // The requests resolve concurrently; the lock makes each update-and-snapshot atomic so no
        // emission can lose another station's answer.
        val lock = Mutex()
        send(readings.toList())

        ordered.forEachIndexed { index, station ->
            launch {
                val resolved = when (val result = getStationMeasurement(station.abbr)) {
                    is Result.Success -> StationReading.Available(station, result.data)
                    is Result.Failure -> StationReading.Unavailable(station)
                }
                lock.withLock {
                    readings[index] = resolved
                    send(readings.toList())
                }
            }
        }
    }
}
