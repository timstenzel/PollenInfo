package ch.stenzel.tim.polleninfo.core.push

import ch.stenzel.tim.polleninfo.core.diary.domain.model.SWISS_ZONE
import ch.stenzel.tim.polleninfo.core.diary.domain.model.swissToday
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.format.formatTime
import ch.stenzel.tim.polleninfo.core.ui.severity.labelResource
import ch.stenzel.tim.polleninfo.core.ui.species.speciesNameResource
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.notification_generic
import ch.stenzel.tim.polleninfo.resources.notification_level
import ch.stenzel.tim.polleninfo.resources.notification_no_current_reading_earlier
import ch.stenzel.tim.polleninfo.resources.notification_no_current_reading_today
import ch.stenzel.tim.polleninfo.resources.notification_no_current_reading_yesterday
import ch.stenzel.tim.polleninfo.resources.notification_no_pollen
import ch.stenzel.tim.polleninfo.resources.notification_not_reported
import ch.stenzel.tim.polleninfo.resources.notification_title
import ch.stenzel.tim.polleninfo.resources.notification_title_no_station
import ch.stenzel.tim.polleninfo.resources.notification_unavailable
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource

/** Looks a string up in the app's language — `getString` in the app, a marker in tests. */
fun interface NotificationStrings {
    suspend fun get(resource: StringResource, args: List<String>): String
}

data class NotificationText(val title: String, val body: String)

/**
 * The title and body of an alarm notification, from the same resources as the screens: "Pollen in
 * Zürich" over "Grasses: High · Birch: Moderate", with the app's pollen-type and severity words.
 *
 * An old reading is named by its time if it is from today, "<time> yesterday" if from yesterday,
 * otherwise by its date — judged in Swiss time against [swissToday], since alarm times are Swiss
 * times whatever zone the phone is in.
 */
suspend fun notificationText(
    content: AlarmNotificationContent,
    dates: DateWording,
    clock: Clock,
    strings: NotificationStrings,
): NotificationText {
    val station = content.stationName
    val title = if (station.isEmpty()) {
        strings.get(Res.string.notification_title_no_station, emptyList())
    } else {
        strings.get(Res.string.notification_title, listOf(station))
    }
    val body = when (content) {
        is AlarmNotificationContent.Levels -> content.levels.map { (species, severity) ->
            // The parser only lets through species the app has a name for.
            val name = strings.get(requireNotNull(speciesNameResource(species)), emptyList())
            strings.get(Res.string.notification_level, listOf(name, strings.get(severity.labelResource(), emptyList())))
        }.joinToString(LEVEL_SEPARATOR)
        is AlarmNotificationContent.NoPollen -> strings.get(Res.string.notification_no_pollen, emptyList())
        is AlarmNotificationContent.NotReported -> strings.get(Res.string.notification_not_reported, emptyList())
        is AlarmNotificationContent.NoCurrentReading -> {
            val latest = content.measuredAt.toLocalDateTime(SWISS_ZONE)
            val today = swissToday(clock)
            when (latest.date) {
                today -> strings.get(
                    Res.string.notification_no_current_reading_today,
                    listOf(station, formatTime(latest.time)),
                )
                today.minus(DatePeriod(days = 1)) -> strings.get(
                    Res.string.notification_no_current_reading_yesterday,
                    listOf(station, formatTime(latest.time)),
                )
                else -> strings.get(
                    Res.string.notification_no_current_reading_earlier,
                    listOf(station, dates.fullDate(latest.date)),
                )
            }
        }
        is AlarmNotificationContent.Unavailable -> strings.get(Res.string.notification_unavailable, listOf(station))
        is AlarmNotificationContent.Generic -> strings.get(Res.string.notification_generic, emptyList())
    }
    return NotificationText(title, body)
}

private const val LEVEL_SEPARATOR = " · "
