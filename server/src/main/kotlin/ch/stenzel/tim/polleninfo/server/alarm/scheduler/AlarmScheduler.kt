package ch.stenzel.tim.polleninfo.server.alarm.scheduler

import ch.stenzel.tim.polleninfo.server.alarm.domain.ALARM_ZONE
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmRules
import ch.stenzel.tim.polleninfo.server.alarm.push.PushResult
import ch.stenzel.tim.polleninfo.server.alarm.push.PushSender
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmWithToken
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import java.time.Clock
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Decides, once a minute, which alarms are due and sends them.
 *
 * Readings come from the same [MeasurementService] the routes use, so alarms and people looking at
 * a station share one cached reading and one upstream request per cache period. A station is only
 * fetched when one of its alarms is due this minute; a station nobody has an alarm for is never
 * fetched at all.
 *
 * Only the current minute is evaluated. A minute the scheduler did not run in — the backend was
 * down, a tick overran — is not replayed: an "08:00 report" at 08:40 is worse than none.
 */
class AlarmScheduler(
    private val alarms: AlarmStore,
    private val measurements: MeasurementService,
    private val push: PushSender,
    private val clock: Clock,
    private val logger: Logger = LoggerFactory.getLogger(AlarmScheduler::class.java),
) {

    /** The minute the last tick evaluated, so a second tick within one minute sends nothing again. */
    private var lastMinute: ZonedDateTime? = null

    suspend fun tick() {
        val now = ZonedDateTime.now(clock).withZoneSameInstant(ALARM_ZONE).truncatedTo(ChronoUnit.MINUTES)
        if (now == lastMinute) return
        lastMinute = now

        val due = alarms.enabledWithDeliverableDevice()
            .filter { AlarmRules.isDailyDue(it.alarm, now) }
            .groupBy { it.alarm.station }

        // Each station on its own: a slow upstream for one never delays another, and anything one
        // throws is logged and stops only that station.
        supervisorScope {
            due.forEach { (station, stationAlarms) ->
                launch {
                    try {
                        sendDue(station, stationAlarms, now)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("Alarms for ${station.abbr} failed at $now", e)
                    }
                }
            }
        }
    }

    private suspend fun sendDue(station: PollenStation, due: List<AlarmWithToken>, now: ZonedDateTime) {
        val reading = measurements.measurementFor(station)
        due.forEach { (alarm, token) ->
            val message = AlarmRules.evaluateDaily(alarm, now, reading) ?: return@forEach
            when (val result = push.send(token, message)) {
                PushResult.Sent -> logger.info("Sent daily report ${alarm.id.value} for ${station.abbr}")
                // Dropping the token arrives with token lifecycle handling; until then it is retried
                // at the alarm's next time.
                PushResult.Unregistered ->
                    logger.warn("Daily report ${alarm.id.value}: push token no longer registered")
                is PushResult.Failed ->
                    logger.warn("Daily report ${alarm.id.value} for ${station.abbr} not delivered", result.cause)
            }
        }
    }
}
