package ch.stenzel.tim.polleninfo.server.alarm.domain

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.Instant

/**
 * What an alarm has to tell, independent of the push service that delivers it — and of any
 * language: the app writes the notification's text from this, in its own language.
 *
 * [levels] is already in display order — worst first, then [PollenSpecies] order — and empty for
 * the kinds that list none. [measuredAt] is set for [PushKind.NO_CURRENT_READING] only.
 */
data class PushMessage(
    val kind: PushKind,
    val channel: PushChannel,
    val station: PollenStation,
    val alarmId: AlarmId,
    val levels: List<Pair<PollenSpecies, PollenSeverity>> = emptyList(),
    val measuredAt: Instant? = null,
)

/** Which notification the app should write. [wire] is the `kind` value in the push payload. */
enum class PushKind(val wire: String) {
    /** Daily report, current reading, at least one selected type above `NONE`: the [PushMessage.levels]. */
    REPORT("report"),

    /** Daily report, current reading, every selected type the station reports at `NONE`. */
    NO_POLLEN("no_pollen"),

    /** Daily report, current reading, the station reports none of the selected types. */
    NOT_REPORTED("not_reported"),

    /** Daily report with "Any", only an old reading: how old the latest one is ([PushMessage.measuredAt]). */
    NO_CURRENT_READING("no_current_reading"),

    /** Daily report with "Any", no reading at all. */
    UNAVAILABLE("unavailable"),

    /** Threshold alert: the types that have newly reached the minimum, in [PushMessage.levels]. */
    ALERT("alert"),
}

/**
 * The Android notification channel a message is posted on. [id] must equal the channel ids the app
 * creates, so the user's per-channel settings apply.
 */
enum class PushChannel(val id: String) {
    DAILY_REPORT("daily_report"),
    THRESHOLD_ALERT("threshold_alert"),
}
