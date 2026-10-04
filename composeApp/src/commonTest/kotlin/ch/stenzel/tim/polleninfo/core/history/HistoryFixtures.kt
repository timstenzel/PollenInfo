package ch.stenzel.tim.polleninfo.core.history

import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryDay
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory
import ch.stenzel.tim.polleninfo.core.history.domain.repository.StationHistoryRepository
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Answers every request with [result], after [gate] completes when one is set — so a test can hold
 * a load in flight and observe what is on screen meanwhile. [requested] records each call.
 */
class FakeStationHistoryRepository(
    var result: Result<StationHistory> = Result.Success(stationHistory()),
    var gate: CompletableDeferred<Unit>? = null,
) : StationHistoryRepository {

    val requested = mutableListOf<Pair<String, HistoryRange>>()

    override suspend fun history(stationAbbr: String, range: HistoryRange): Result<StationHistory> {
        requested += stationAbbr to range
        gate?.await()
        return result
    }
}

/**
 * A history of [days] consecutive days from [from], every day with the same [levels] — enough for
 * tests that care about the shape rather than the values.
 */
fun stationHistory(
    stationAbbr: String = "PZH",
    from: LocalDate = LocalDate(2026, 9, 4),
    days: Int = 30,
    levels: Map<String, PollenSeverity?> = mapOf(
        "BIRCH" to PollenSeverity.LOW,
        "GRASSES" to PollenSeverity.HIGH,
        "ASH" to null,
    ),
): StationHistory {
    val dates = (0 until days).map { from.plus(DatePeriod(days = it)) }
    return StationHistory(
        stationAbbr = stationAbbr,
        from = dates.first(),
        until = dates.last(),
        days = dates.map { HistoryDay(it, levels) },
    )
}
