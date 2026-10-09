package ch.stenzel.tim.polleninfo.core.push

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.ui.species.speciesNameResource
import kotlin.time.Instant

/**
 * The notification channel an alarm is posted on. [id] is the backend's `PushChannel` id and the id
 * of the Android channel the app creates.
 */
enum class AlarmChannel(val id: String) {
    DAILY_REPORT("daily_report"),
    THRESHOLD_ALERT("threshold_alert"),
}

/**
 * What an alarm's push message says, read from its data payload ([parseAlarmPayload]) and worded
 * on the device by [notificationText] — the backend sends no text.
 */
sealed interface AlarmNotificationContent {
    /** The station as published ("Zürich"), its abbreviation if the name was missing, or empty. */
    val stationName: String
    val channel: AlarmChannel

    /**
     * A daily report's or ([isAlert]) a threshold alert's pollen types, by the backend's species
     * id, in the order the backend chose: worst first, then species order.
     */
    data class Levels(
        override val stationName: String,
        override val channel: AlarmChannel,
        val levels: List<Pair<String, PollenSeverity>>,
        val isAlert: Boolean,
    ) : AlarmNotificationContent

    /** Every selected type the station reports is at `NONE`. */
    data class NoPollen(override val stationName: String, override val channel: AlarmChannel) : AlarmNotificationContent

    /** The station reports none of the selected types. */
    data class NotReported(override val stationName: String, override val channel: AlarmChannel) :
        AlarmNotificationContent

    /** Only an old reading exists; [measuredAt] is when it is from. */
    data class NoCurrentReading(
        override val stationName: String,
        override val channel: AlarmChannel,
        val measuredAt: Instant,
    ) : AlarmNotificationContent

    /** No reading at all. */
    data class Unavailable(override val stationName: String, override val channel: AlarmChannel) :
        AlarmNotificationContent

    /** A message the app does not fully understand: still shown, as "Pollen in <station>". */
    data class Generic(override val stationName: String, override val channel: AlarmChannel) : AlarmNotificationContent
}

/**
 * Reads an alarm's FCM data payload — the keys the backend's `PushPayload` writes.
 *
 * Anything the app does not understand — an unknown `kind`, an unknown species or severity, a
 * malformed or missing `levels` where one is needed, a missing or unparseable `measuredAt` —
 * turns the **whole** message into [AlarmNotificationContent.Generic], never into a list that
 * silently drops a type. A missing `stationName` falls back to `stationAbbr`; a missing or unknown
 * `channel` to the daily report channel, since the channel never changes what is said.
 */
fun parseAlarmPayload(data: Map<String, String>): AlarmNotificationContent {
    val station = data[KEY_STATION_NAME]?.takeIf { it.isNotBlank() }
        ?: data[KEY_STATION_ABBR]?.takeIf { it.isNotBlank() }
        ?: ""
    val channel = AlarmChannel.entries.firstOrNull { it.id == data[KEY_CHANNEL] } ?: AlarmChannel.DAILY_REPORT
    val generic = AlarmNotificationContent.Generic(station, channel)
    return when (data[KEY_KIND]) {
        KIND_REPORT, KIND_ALERT -> parseLevels(data[KEY_LEVELS])
            ?.let { AlarmNotificationContent.Levels(station, channel, it, isAlert = data[KEY_KIND] == KIND_ALERT) }
            ?: generic
        KIND_NO_POLLEN -> AlarmNotificationContent.NoPollen(station, channel)
        KIND_NOT_REPORTED -> AlarmNotificationContent.NotReported(station, channel)
        KIND_NO_CURRENT_READING -> data[KEY_MEASURED_AT]
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?.let { AlarmNotificationContent.NoCurrentReading(station, channel, it) }
            ?: generic
        KIND_UNAVAILABLE -> AlarmNotificationContent.Unavailable(station, channel)
        else -> generic
    }
}

/** `BIRCH:HIGH,GRASSES:MODERATE` in its order, or `null` if it is empty or any entry is not understood. */
private fun parseLevels(levels: String?): List<Pair<String, PollenSeverity>>? {
    if (levels.isNullOrBlank()) return null
    return levels.split(',').map { entry ->
        val parts = entry.split(':')
        if (parts.size != 2) return null
        val (species, severityName) = parts
        if (speciesNameResource(species) == null) return null
        val severity = PollenSeverity.entries.firstOrNull { it.name == severityName } ?: return null
        species to severity
    }
}

private const val KEY_KIND = "kind"
private const val KEY_CHANNEL = "channel"
private const val KEY_STATION_ABBR = "stationAbbr"
private const val KEY_STATION_NAME = "stationName"
private const val KEY_LEVELS = "levels"
private const val KEY_MEASURED_AT = "measuredAt"

private const val KIND_REPORT = "report"
private const val KIND_NO_POLLEN = "no_pollen"
private const val KIND_NOT_REPORTED = "not_reported"
private const val KIND_NO_CURRENT_READING = "no_current_reading"
private const val KIND_UNAVAILABLE = "unavailable"
private const val KIND_ALERT = "alert"
