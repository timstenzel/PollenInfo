package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.alarm.domain.PushMessage

/**
 * The keys of a push message's data payload — the contract with the app's `parseAlarmPayload`.
 * Every value is a string, as FCM requires.
 */
object PushPayload {
    const val KIND = "kind"
    const val CHANNEL = "channel"
    const val STATION_ABBR = "stationAbbr"

    /** The station's name as published, so the app can word a notification with no station list loaded. */
    const val STATION_NAME = "stationName"
    const val ALARM_ID = "alarmId"

    /** `BIRCH:HIGH,GRASSES:MODERATE` — species and severity enum names, in display order. */
    const val LEVELS = "levels"

    /** ISO-8601 instant of the latest reading, for `no_current_reading` only. */
    const val MEASURED_AT = "measuredAt"
}

/** The data payload for [this]; `levels` and `measuredAt` only when the message has them. */
fun PushMessage.toData(): Map<String, String> = buildMap {
    put(PushPayload.KIND, kind.wire)
    put(PushPayload.CHANNEL, channel.id)
    put(PushPayload.STATION_ABBR, station.abbr)
    put(PushPayload.STATION_NAME, station.displayName)
    put(PushPayload.ALARM_ID, alarmId.value)
    if (levels.isNotEmpty()) {
        put(PushPayload.LEVELS, levels.joinToString(",") { (species, severity) -> "${species.name}:${severity.name}" })
    }
    measuredAt?.let { put(PushPayload.MEASURED_AT, it.toString()) }
}
