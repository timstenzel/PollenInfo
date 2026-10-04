package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.ALARM_TIME_FORMAT
import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction

/** Every device's alarms. A device only ever sees its own. */
interface AlarmStore {

    /**
     * The device's alarms in creation order, or `null` if the device is not registered — which a
     * caller must be able to tell apart from a registered device with no alarms.
     */
    suspend fun list(deviceId: DeviceId): List<Alarm>?
}

class ExposedAlarmStore(private val database: Database) : AlarmStore {

    override suspend fun list(deviceId: DeviceId): List<Alarm>? = withContext(Dispatchers.IO) {
        // One transaction, so a device cannot vanish between the two reads.
        transaction(database) {
            if (!deviceExists(deviceId)) return@transaction null
            AlarmsTable.selectAll()
                .where { AlarmsTable.deviceId eq deviceId.value }
                .orderBy(AlarmsTable.createdAt to SortOrder.ASC, AlarmsTable.id to SortOrder.ASC)
                .map { it.toAlarm() }
        }
    }
}

/** Writes every column of [alarm]; shared by inserts and, later, updates. */
internal fun UpdateBuilder<*>.setAlarm(alarm: Alarm) {
    this[AlarmsTable.id] = alarm.id.value
    this[AlarmsTable.deviceId] = alarm.deviceId.value
    this[AlarmsTable.enabled] = alarm.enabled
    this[AlarmsTable.stationAbbr] = alarm.station.abbr
    this[AlarmsTable.species] = alarm.species.sorted().joinToString(SEPARATOR) { it.name }
    this[AlarmsTable.minSeverity] = alarm.minSeverity.name
    this[AlarmsTable.days] = alarm.days.sorted().joinToString(SEPARATOR) { it.name }
    when (val schedule = alarm.schedule) {
        is AlarmSchedule.Daily -> {
            this[AlarmsTable.type] = AlarmsTable.TYPE_DAILY
            this[AlarmsTable.atTime] = schedule.at.format(ALARM_TIME_FORMAT)
            this[AlarmsTable.fromTime] = null
            this[AlarmsTable.untilTime] = null
        }
        is AlarmSchedule.Threshold -> {
            this[AlarmsTable.type] = AlarmsTable.TYPE_THRESHOLD
            this[AlarmsTable.atTime] = null
            this[AlarmsTable.fromTime] = schedule.from.format(ALARM_TIME_FORMAT)
            this[AlarmsTable.untilTime] = schedule.until.format(ALARM_TIME_FORMAT)
        }
    }
}

private fun ResultRow.toAlarm() = Alarm(
    id = AlarmId(this[AlarmsTable.id]),
    deviceId = DeviceId(this[AlarmsTable.deviceId]),
    enabled = this[AlarmsTable.enabled],
    station = requireNotNull(PollenStation.fromAbbr(this[AlarmsTable.stationAbbr])) {
        "Unknown station ${this[AlarmsTable.stationAbbr]}"
    },
    species = this[AlarmsTable.species].split(SEPARATOR).map(PollenSpecies::valueOf).toSet(),
    minSeverity = PollenSeverity.valueOf(this[AlarmsTable.minSeverity]),
    days = this[AlarmsTable.days].split(SEPARATOR).map(DayOfWeek::valueOf).toSet(),
    schedule = when (val type = this[AlarmsTable.type]) {
        AlarmsTable.TYPE_DAILY -> AlarmSchedule.Daily(LocalTime.parse(this[AlarmsTable.atTime], ALARM_TIME_FORMAT))
        AlarmsTable.TYPE_THRESHOLD -> AlarmSchedule.Threshold(
            from = LocalTime.parse(this[AlarmsTable.fromTime], ALARM_TIME_FORMAT),
            until = LocalTime.parse(this[AlarmsTable.untilTime], ALARM_TIME_FORMAT),
        )
        else -> error("Unknown alarm type $type")
    },
)

private const val SEPARATOR = ","
