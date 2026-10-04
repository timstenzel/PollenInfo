package ch.stenzel.tim.polleninfo.feature.alarms

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmDraft
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmNotFoundException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.feature.alarms.domain.repository.AlarmRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * While [gate] is set, every list call suspends until it is completed — how a test holds a load in
 * flight to observe the state the screen shows meanwhile. [result] is read after the gate opens.
 * [createGate] and [createResult] do the same for [create]; without a [createResult] a create
 * succeeds with the draft stored under `created-N`.
 *
 * [alarm] finds the id in [result] unless [alarmResult] is set, after [alarmGate] opens. [update] and [delete] succeed and
 * record their arguments unless [updateResult] / [deleteResult] say otherwise; [updateGate] holds an
 * update in flight.
 */
class FakeAlarmRepository(
    var result: Result<List<Alarm>> = Result.Success(emptyList()),
    var gate: CompletableDeferred<Unit>? = null,
) : AlarmRepository {

    var callCount: Int = 0
        private set

    var createResult: Result<Alarm>? = null
    var createGate: CompletableDeferred<Unit>? = null

    /** Every draft handed to [create], in order. */
    val createdDrafts = mutableListOf<AlarmDraft>()

    var alarmResult: Result<Alarm>? = null
    var alarmGate: CompletableDeferred<Unit>? = null

    var updateResult: Result<Alarm>? = null
    var updateGate: CompletableDeferred<Unit>? = null

    /** Every `(id, draft)` handed to [update], in order. */
    val updates = mutableListOf<Pair<String, AlarmDraft>>()

    var deleteResult: Result<Unit> = Result.Success(Unit)

    /** Every id handed to [delete], in order. */
    val deletedIds = mutableListOf<String>()

    override suspend fun alarms(): Result<List<Alarm>> {
        callCount++
        gate?.await()
        return result
    }

    override suspend fun create(draft: AlarmDraft): Result<Alarm> {
        createdDrafts += draft
        createGate?.await()
        return createResult ?: Result.Success(draft.toAlarm("created-${createdDrafts.size}"))
    }

    override suspend fun alarm(id: String): Result<Alarm> {
        alarmGate?.await()
        alarmResult?.let { return it }
        return when (val all = result) {
            is Result.Success -> all.data.firstOrNull { it.id == id }?.let { Result.Success(it) }
                ?: Result.Failure(AlarmNotFoundException())
            is Result.Failure -> all
        }
    }

    override suspend fun update(id: String, draft: AlarmDraft): Result<Alarm> {
        updates += id to draft
        updateGate?.await()
        return updateResult ?: Result.Success(draft.toAlarm(id))
    }

    override suspend fun delete(id: String): Result<Unit> {
        deletedIds += id
        return deleteResult
    }
}

fun AlarmDraft.toAlarm(id: String) = Alarm(
    id = id,
    enabled = enabled,
    stationAbbr = stationAbbr,
    species = species,
    minSeverity = minSeverity,
    days = days,
    schedule = schedule,
)

val WEEKDAYS = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

fun dailyAlarm(
    id: String = "daily-1",
    stationAbbr: String = "PZH",
    at: LocalTime = LocalTime(8, 0),
) = Alarm(
    id = id,
    enabled = true,
    stationAbbr = stationAbbr,
    species = setOf("BIRCH", "GRASSES"),
    minSeverity = PollenSeverity.NONE,
    days = WEEKDAYS,
    schedule = AlarmSchedule.Daily(at),
)

fun thresholdAlarm(
    id: String = "threshold-1",
    stationAbbr: String = "PBE",
) = Alarm(
    id = id,
    enabled = false,
    stationAbbr = stationAbbr,
    species = setOf("HAZEL"),
    minSeverity = PollenSeverity.HIGH,
    days = DayOfWeek.entries.toSet(),
    schedule = AlarmSchedule.Threshold(from = LocalTime(7, 0), until = LocalTime(21, 0)),
)
