package ch.stenzel.tim.polleninfo.server.alarm.scheduler

import ch.stenzel.tim.polleninfo.server.alarm.domain.ALARM_ZONE
import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmRules
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.push.PushResult
import ch.stenzel.tim.polleninfo.server.alarm.push.PushSender
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmWithToken
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.NotificationLog
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.measurement.CacheResult
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.StationMeasurement
import java.time.Clock
import java.time.LocalDate
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
 * fetched when one of its alarms needs it this minute — a daily report due now, or a threshold
 * alert whose window is open; a station nobody has an alarm for is never fetched at all.
 *
 * A threshold alert is evaluated on every tick of its window, and [log] remembers which pollen
 * types it has notified about today. A type is recorded only once its message was delivered, so a
 * failed send is retried on the next tick; the log is persisted, so a restart never repeats one.
 *
 * Only the current minute is evaluated. A minute the scheduler did not run in — the backend was
 * down, a tick overran — is not replayed: an "08:00 report" at 08:40 is worse than none.
 */
class AlarmScheduler(
    private val alarms: AlarmStore,
    private val devices: DeviceStore,
    private val log: NotificationLog,
    private val measurements: MeasurementService,
    private val push: PushSender,
    private val clock: Clock,
    private val logger: Logger = LoggerFactory.getLogger(AlarmScheduler::class.java),
) {

    /** The minute the last tick evaluated, so a second tick within one minute sends nothing again. */
    private var lastMinute: ZonedDateTime? = null

    /** The Swiss date the log was last pruned for; pruning runs on the first tick of each day. */
    private var prunedFor: LocalDate? = null

    /** The Swiss date inactive devices were last pruned on, tracked apart so each step retries alone. */
    private var devicesPrunedFor: LocalDate? = null

    suspend fun tick() {
        val now = ZonedDateTime.now(clock).withZoneSameInstant(ALARM_ZONE).truncatedTo(ChronoUnit.MINUTES)
        if (now == lastMinute) return
        lastMinute = now
        pruneLog(now.toLocalDate())
        pruneInactiveDevices(now)

        val due = alarms.enabledWithDeliverableDevice()
            .filter { AlarmRules.isDailyDue(it.alarm, now) || AlarmRules.isThresholdActive(it.alarm, now) }
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

    /** A failed prune is retried on the next tick and never costs this minute's alarms. */
    private suspend fun pruneLog(today: LocalDate) {
        if (prunedFor == today) return
        try {
            log.pruneBefore(today)
            prunedFor = today
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Pruning the notification log before $today failed", e)
        }
    }

    /**
     * Removes the devices that have no push token and have not called for 90 days, on the first tick
     * of each Swiss day. Like the log prune, a failure is retried on the next tick and never costs
     * this minute's alarms.
     */
    private suspend fun pruneInactiveDevices(now: ZonedDateTime) {
        val today = now.toLocalDate()
        if (devicesPrunedFor == today) return
        try {
            val pruned = devices.pruneInactive(clock.instant())
            devicesPrunedFor = today
            if (pruned > 0) logger.info("Removed $pruned inactive devices without a push token")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Removing inactive devices on $today failed", e)
        }
    }

    private suspend fun sendDue(station: PollenStation, due: List<AlarmWithToken>, now: ZonedDateTime) {
        val reading = measurements.measurementFor(station)
        due.forEach { (alarm, token) ->
            when (alarm.schedule) {
                is AlarmSchedule.Daily -> sendDaily(alarm, token, now, reading)
                is AlarmSchedule.Threshold -> sendThreshold(alarm, token, now, reading)
            }
        }
    }

    private suspend fun sendDaily(alarm: Alarm, token: String, now: ZonedDateTime, reading: CacheResult<StationMeasurement>) {
        val message = AlarmRules.evaluateDaily(alarm, now, reading) ?: return
        when (val result = push.send(token, message)) {
            PushResult.Sent -> logger.info("Sent daily report ${alarm.id.value} for ${alarm.station.abbr}")
            PushResult.Unregistered -> dropToken(alarm, token)
            is PushResult.Failed ->
                logger.warn("Daily report ${alarm.id.value} for ${alarm.station.abbr} not delivered", result.cause)
        }
    }

    private suspend fun sendThreshold(
        alarm: Alarm,
        token: String,
        now: ZonedDateTime,
        reading: CacheResult<StationMeasurement>,
    ) {
        val measurement = when (reading) {
            is CacheResult.Fresh -> reading.value
            is CacheResult.Stale -> reading.value
            is CacheResult.Failed -> null
        }
        val today = now.toLocalDate()
        val outcome = AlarmRules.evaluateThreshold(alarm, now, measurement, log.notifiedSpecies(alarm.id, today))
            ?: return
        when (val result = push.send(token, outcome.message)) {
            PushResult.Sent -> {
                log.record(alarm.id, outcome.species, today)
                logger.info("Sent threshold alert ${alarm.id.value} for ${alarm.station.abbr}: ${outcome.species}")
            }
            // Not recorded either way: after a failure the next tick in the window tries again, and
            // after a dropped token the alert picks up once the app sends a new one.
            PushResult.Unregistered -> dropToken(alarm, token)
            is PushResult.Failed ->
                logger.warn("Threshold alert ${alarm.id.value} for ${alarm.station.abbr} not delivered", result.cause)
        }
    }

    /**
     * The push service no longer knows [token] — typically the app was uninstalled. The device and
     * its alarms are kept, but without a token none of them is loaded again until the app sends a
     * new one through `PUT /devices/me/fcm-token`.
     */
    private suspend fun dropToken(alarm: Alarm, token: String) {
        logger.warn("Alarm ${alarm.id.value}: push token no longer registered, dropping it")
        devices.clearFcmToken(alarm.deviceId, token)
    }
}
