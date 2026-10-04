package ch.stenzel.tim.polleninfo.server.alarm.scheduler

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs [scheduler] at the start of every minute for the life of the application.
 *
 * A tick that throws is logged and the loop carries on, so one bad minute never ends alarm delivery.
 * The loop holds no logic beyond that; [AlarmScheduler.tick] is what is tested.
 */
fun Application.launchAlarmScheduler(scheduler: AlarmScheduler, clock: Clock) {
    val job = launch {
        while (isActive) {
            delay(untilNextMinute(clock).toMillis())
            try {
                scheduler.tick()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("Alarm scheduler tick failed", e)
            }
        }
    }
    monitor.subscribe(ApplicationStopped) { job.cancel() }
}

private fun untilNextMinute(clock: Clock): Duration {
    val now = clock.instant()
    return Duration.between(now, now.truncatedTo(ChronoUnit.MINUTES).plus(Duration.ofMinutes(1)))
}
