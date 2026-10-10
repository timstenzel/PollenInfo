package ch.stenzel.tim.polleninfo.server.alarm.store

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
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

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
            // The device row stays locked until the insert commits, so a second create for the same
            // device waits here and then counts this one. Counting in one transaction is not enough
            // on its own: under READ COMMITTED two concurrent creates at nine would both count nine.
            val locked = DevicesTable.selectAll().where { DevicesTable.id eq deviceId.value }.forUpdate().any()
            if (!locked) return@transaction CreateResult.UnknownDevice
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
            val uuid = alarmId.toUuidOrNull() ?: return@withContext null
            transaction(database) {
                // The notification log is left alone: it is keyed per type, so an edit neither repeats
                // a notification sent earlier today nor holds back a newly selected type.
                val updated = AlarmsTable.update({ ownedBy(deviceId, uuid) }) { it.setAlarm(alarm) }
                if (updated == 0) null else alarm
            }
        }

    override suspend fun delete(deviceId: DeviceId, alarmId: AlarmId): Boolean = withContext(Dispatchers.IO) {
        val uuid = alarmId.toUuidOrNull() ?: return@withContext false
        // The notification log goes with the alarm through its ON DELETE CASCADE.
        transaction(database) { AlarmsTable.deleteWhere { ownedBy(deviceId, uuid) } > 0 }
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
    private fun nextCreatedAt(deviceId: DeviceId): OffsetDateTime {
        val newest = AlarmsTable.createdAt.max()
        val latest = AlarmsTable.select(newest)
            .where { AlarmsTable.deviceId eq deviceId.value }
            .single()[newest]
            ?.toInstant()
        val now = clock.instant()
        return (if (latest == null) now else maxOf(now, latest.plusMillis(1))).toTimestamp()
    }
}

private fun ownedBy(deviceId: DeviceId, alarmId: UUID): Op<Boolean> =
    (AlarmsTable.id eq alarmId) and (AlarmsTable.deviceId eq deviceId.value)

/**
 * The alarm id as the column's UUID, or `null` for an id no alarm can have — which callers answer
 * exactly as an unknown alarm, so a malformed id reveals nothing either.
 */
internal fun AlarmId.toUuidOrNull(): UUID? = try {
    UUID.fromString(value)
} catch (e: IllegalArgumentException) {
    null
}

/** How instants are written to `timestamptz` columns: at UTC, to the millisecond. */
internal fun Instant.toTimestamp(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

/** Writes every column of [alarm] but its creation time; shared by inserts and updates. */
internal fun UpdateBuilder<*>.setAlarm(alarm: Alarm) {
    this[AlarmsTable.id] = UUID.fromString(alarm.id.value)
    this[AlarmsTable.deviceId] = alarm.deviceId.value
    this[AlarmsTable.enabled] = alarm.enabled
    this[AlarmsTable.stationAbbr] = alarm.station.abbr
    this[AlarmsTable.species] = alarm.species.sorted().map { it.name }
    this[AlarmsTable.minSeverity] = alarm.minSeverity.name
    this[AlarmsTable.days] = alarm.days.sorted().map { it.name }
    when (val schedule = alarm.schedule) {
        is AlarmSchedule.Daily -> {
            this[AlarmsTable.type] = AlarmsTable.TYPE_DAILY
            this[AlarmsTable.atTime] = schedule.at
            this[AlarmsTable.fromTime] = null
            this[AlarmsTable.untilTime] = null
        }
        is AlarmSchedule.Threshold -> {
            this[AlarmsTable.type] = AlarmsTable.TYPE_THRESHOLD
            this[AlarmsTable.atTime] = null
            this[AlarmsTable.fromTime] = schedule.from
            this[AlarmsTable.untilTime] = schedule.until
        }
    }
}

private fun ResultRow.toAlarm() = Alarm(
    id = AlarmId(this[AlarmsTable.id].toString()),
    deviceId = DeviceId(this[AlarmsTable.deviceId]),
    enabled = this[AlarmsTable.enabled],
    station = requireNotNull(PollenStation.fromAbbr(this[AlarmsTable.stationAbbr])) {
        "Unknown station ${this[AlarmsTable.stationAbbr]}"
    },
    species = this[AlarmsTable.species].map(PollenSpecies::valueOf).toSet(),
    minSeverity = PollenSeverity.valueOf(this[AlarmsTable.minSeverity]),
    days = this[AlarmsTable.days].map(DayOfWeek::valueOf).toSet(),
    schedule = when (val type = this[AlarmsTable.type]) {
        AlarmsTable.TYPE_DAILY -> AlarmSchedule.Daily(checkNotNull(this[AlarmsTable.atTime]))
        AlarmsTable.TYPE_THRESHOLD -> AlarmSchedule.Threshold(
            from = checkNotNull(this[AlarmsTable.fromTime]),
            until = checkNotNull(this[AlarmsTable.untilTime]),
        )
        else -> error("Unknown alarm type $type")
    },
)
