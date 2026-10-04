package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.ALARM_TIME_FORMAT
import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/** Every device's alarms. A device only ever sees its own. */
interface AlarmStore {

    /**
     * The device's alarms in creation order, or `null` if the device is not registered — which a
     * caller must be able to tell apart from a registered device with no alarms.
     */
    suspend fun list(deviceId: DeviceId): List<Alarm>?

    /**
     * Stores [spec] as a new alarm of the device, under a fresh id — unless the device already holds
     * [MAX_ALARMS_PER_DEVICE].
     */
    suspend fun create(deviceId: DeviceId, spec: AlarmSpec): CreateResult

    /**
     * Replaces the alarm's settings with [spec], keeping its id and creation time. `null` if the
     * device has no alarm [alarmId] — whether it does not exist or belongs to another device, which a
     * caller must not be able to tell apart.
     */
    suspend fun update(deviceId: DeviceId, alarmId: AlarmId, spec: AlarmSpec): Alarm?

    /** Deletes the alarm and its notification log; `false` if the device has no alarm [alarmId]. */
    suspend fun delete(deviceId: DeviceId, alarmId: AlarmId): Boolean

    /**
     * Every enabled alarm whose device has a push token, with that token — everything the scheduler
     * can deliver, and nothing it cannot.
     */
    suspend fun enabledWithDeliverableDevice(): List<AlarmWithToken>
}

/** An alarm with the push token its notifications go to. */
data class AlarmWithToken(val alarm: Alarm, val fcmToken: String)

sealed interface CreateResult {
    data class Created(val alarm: Alarm) : CreateResult

    /** The device is not registered; nothing was stored. */
    data object UnknownDevice : CreateResult

    /** The device already holds [MAX_ALARMS_PER_DEVICE] alarms; nothing was stored. */
    data object LimitReached : CreateResult
}

/** How many alarms one device may hold. */
const val MAX_ALARMS_PER_DEVICE = 10

class ExposedAlarmStore(
    private val database: Database,
    private val clock: Clock = Clock.systemUTC(),
) : AlarmStore {

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

    override suspend fun create(deviceId: DeviceId, spec: AlarmSpec): CreateResult = withContext(Dispatchers.IO) {
        val alarm = spec.toAlarm(AlarmId(UUID.randomUUID().toString()), deviceId)
        transaction(database) {
            if (!deviceExists(deviceId)) return@transaction CreateResult.UnknownDevice
            // Counted in the same transaction as the insert, so two concurrent creates at nine
            // cannot both get through.
            val held = AlarmsTable.selectAll().where { AlarmsTable.deviceId eq deviceId.value }.count()
            if (held >= MAX_ALARMS_PER_DEVICE) return@transaction CreateResult.LimitReached
            AlarmsTable.insert {
                it.setAlarm(alarm)
                it[createdAt] = nextCreatedAt(deviceId)
            }
            CreateResult.Created(alarm)
        }
    }

    override suspend fun update(deviceId: DeviceId, alarmId: AlarmId, spec: AlarmSpec): Alarm? =
        withContext(Dispatchers.IO) {
            val alarm = spec.toAlarm(alarmId, deviceId)
            transaction(database) {
                // The notification log is left alone: it is keyed per type, so an edit neither repeats
                // a notification sent earlier today nor holds back a newly selected type.
                val updated = AlarmsTable.update({ ownedBy(deviceId, alarmId) }) { it.setAlarm(alarm) }
                if (updated == 0) null else alarm
            }
        }

    override suspend fun delete(deviceId: DeviceId, alarmId: AlarmId): Boolean = withContext(Dispatchers.IO) {
        // The notification log goes with the alarm through its ON DELETE CASCADE.
        transaction(database) { AlarmsTable.deleteWhere { ownedBy(deviceId, alarmId) } > 0 }
    }

    override suspend fun enabledWithDeliverableDevice(): List<AlarmWithToken> = withContext(Dispatchers.IO) {
        transaction(database) {
            (AlarmsTable innerJoin DevicesTable)
                .selectAll()
                .where { (AlarmsTable.enabled eq true) and DevicesTable.fcmToken.isNotNull() }
                .orderBy(AlarmsTable.createdAt to SortOrder.ASC, AlarmsTable.id to SortOrder.ASC)
                .map { AlarmWithToken(it.toAlarm(), checkNotNull(it[DevicesTable.fcmToken])) }
        }
    }

    /**
     * Now, or one millisecond after the device's newest alarm if that is not earlier. Creation order
     * is the list order, and two alarms created within the same millisecond would otherwise fall
     * back to the order of their random ids.
     */
    private fun nextCreatedAt(deviceId: DeviceId): Long {
        val newest = AlarmsTable.createdAt.max()
        val latest = AlarmsTable.select(newest)
            .where { AlarmsTable.deviceId eq deviceId.value }
            .single()[newest]
        return if (latest == null) clock.millis() else maxOf(clock.millis(), latest + 1)
    }
}

private fun ownedBy(deviceId: DeviceId, alarmId: AlarmId): Op<Boolean> =
    (AlarmsTable.id eq alarmId.value) and (AlarmsTable.deviceId eq deviceId.value)

/** Writes every column of [alarm] but its creation time; shared by inserts and updates. */
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
