package ch.stenzel.tim.polleninfo.server.alarm.store

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.time
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

/*
 * The Kotlin view of the tables `db/migration/V1__init.sql` creates. The migrations own the schema;
 * nothing here is ever used to create or alter a table, so a change starts with a new migration
 * and is then mirrored here.
 */

/**
 * One row per registered install, keyed by an internal UUID. [tokenHash] is the SHA-256 of the
 * install's device token — the token itself is never stored. [fcmToken] is nullable so the scheduler
 * can drop a token the push service reports as unregistered without forgetting the device and its
 * alarms. [lastSeenAt] is refreshed by authentication at most once per [LAST_SEEN_RESOLUTION].
 */
internal object DevicesTable : Table("devices") {
    val id = javaUUID("id")
    val tokenHash = binary("token_hash")
    val fcmToken = text("fcm_token").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val lastSeenAt = timestampWithTimeZone("last_seen_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Sets are `text[]` of enum names. [atTime] is set for daily reports, [fromTime] and [untilTime] for
 * threshold alerts, as [type] says.
 */
internal object AlarmsTable : Table("alarms") {
    val id = javaUUID("id")
    val deviceId = reference("device_id", DevicesTable.id, onDelete = ReferenceOption.CASCADE)
    val enabled = bool("enabled")
    val stationAbbr = varchar("station_abbr", length = 3)
    val species = array("species", TextColumnType())
    val minSeverity = varchar("min_severity", length = 16)
    val days = array("days", TextColumnType())
    val type = varchar("type", length = 16)
    val atTime = time("at_time").nullable()
    val fromTime = time("from_time").nullable()
    val untilTime = time("until_time").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)

    const val TYPE_DAILY = "daily"
    const val TYPE_THRESHOLD = "threshold"
}

/**
 * Which pollen types each threshold alert has already notified about on a Swiss calendar day
 * ([localDate]). Persisted so that a restart never repeats a notification sent earlier the same
 * day; rows go with their alarm.
 */
internal object NotificationLogTable : Table("notification_log") {
    val alarmId = reference("alarm_id", AlarmsTable.id, onDelete = ReferenceOption.CASCADE)
    val species = varchar("species", length = 16)
    val localDate = date("local_date")

    override val primaryKey = PrimaryKey(alarmId, species, localDate)
}
