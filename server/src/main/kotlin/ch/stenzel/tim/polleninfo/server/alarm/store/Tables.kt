package ch.stenzel.tim.polleninfo.server.alarm.store

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/**
 * One row per registered install. [fcmToken] is nullable so push can later drop a token the push
 * service reports as unregistered without forgetting the device and its alarms.
 */
internal object DevicesTable : Table("devices") {
    val id = varchar("id", length = 32)
    val fcmToken = text("fcm_token").nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Sets are stored as comma-separated enum names and times as `HH:mm`, which keeps the file readable
 * with the `sqlite3` shell. [atTime] is set for daily reports, [fromTime] and [untilTime] for
 * threshold alerts, as [type] says.
 */
internal object AlarmsTable : Table("alarms") {
    val id = varchar("id", length = 36)
    val deviceId = reference("device_id", DevicesTable.id)
    val enabled = bool("enabled")
    val stationAbbr = varchar("station_abbr", length = 3)
    val species = text("species")
    val minSeverity = varchar("min_severity", length = 16)
    val days = text("days")
    val type = varchar("type", length = 16)
    val atTime = varchar("at_time", length = 5).nullable()
    val fromTime = varchar("from_time", length = 5).nullable()
    val untilTime = varchar("until_time", length = 5).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    const val TYPE_DAILY = "daily"
    const val TYPE_THRESHOLD = "threshold"
}

/**
 * Which pollen types each threshold alert has already notified about on a Swiss calendar day
 * ([localDate], ISO `yyyy-MM-dd`). Persisted so that a restart never repeats a notification sent
 * earlier the same day; rows go with their alarm.
 */
internal object NotificationLogTable : Table("notification_log") {
    val alarmId = reference("alarm_id", AlarmsTable.id, onDelete = ReferenceOption.CASCADE)
    val species = varchar("species", length = 16)
    val localDate = varchar("local_date", length = 10)

    override val primaryKey = PrimaryKey(alarmId, species, localDate)
}
