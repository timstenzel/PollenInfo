package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

fun dailyAlarm(
    deviceId: DeviceId,
    id: AlarmId = AlarmId(UUID.randomUUID().toString()),
    station: PollenStation = PollenStation.ZUERICH,
    at: LocalTime = LocalTime.of(8, 0),
) = Alarm(
    id = id,
    deviceId = deviceId,
    enabled = true,
    station = station,
    species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES),
    minSeverity = PollenSeverity.NONE,
    days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
    schedule = AlarmSchedule.Daily(at),
)

fun thresholdAlarm(
    deviceId: DeviceId,
    id: AlarmId = AlarmId(UUID.randomUUID().toString()),
    station: PollenStation = PollenStation.BERN,
) = Alarm(
    id = id,
    deviceId = deviceId,
    enabled = false,
    station = station,
    species = setOf(PollenSpecies.HAZEL),
    minSeverity = PollenSeverity.HIGH,
    days = DayOfWeek.entries.toSet(),
    schedule = AlarmSchedule.Threshold(from = LocalTime.of(7, 0), until = LocalTime.of(21, 0)),
)

/**
 * Inserts [alarm] directly, with the id and creation time a test chooses — neither of which
 * `AlarmStore.create` lets its caller pick. Also the only way to store a threshold alert until the
 * API accepts them.
 */
fun Database.insertAlarm(alarm: Alarm, createdAtMillis: Long) {
    transaction(this) {
        AlarmsTable.insert {
            it.setAlarm(alarm)
            it[AlarmsTable.createdAt] = createdAtMillis
        }
    }
}
