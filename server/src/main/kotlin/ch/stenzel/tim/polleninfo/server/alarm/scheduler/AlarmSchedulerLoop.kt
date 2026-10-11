package ch.stenzel.tim.polleninfo.server.alarm.scheduler

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long a stopping server waits for a running tick to finish before cancelling it. A tick is
 * bounded by the 15-second upstream and push timeouts, so this only ever cuts one short that hangs.
 * `stop_grace_period` in `deploy/compose.yaml` must exceed it plus the Netty shutdown in `main()`.
 */
val TICK_STOP_TIMEOUT = 45.seconds

/**
 * Runs [scheduler] once at start — catching up the minutes a restart missed, and the current one —
 * and then at the start of every minute for the life of the application.
 *
 * A tick that throws is logged and the loop carries on, so one bad minute never ends alarm delivery.
 *
 * Stopping lets a running tick finish: on [ApplicationStopping] — before the application's
 * coroutines are cancelled and before the database pool closes — the loop is told to leave after
 * its current tick, and the stop waits for that (at most [TICK_STOP_TIMEOUT]). A deploy therefore
 * never cuts a minute's sends in half; the minute is recorded as finished and a restarted server
 * does not send it again. The loop holds no logic beyond that; [AlarmScheduler.tick] is what is
 * tested.
 */
fun Application.launchAlarmScheduler(scheduler: AlarmScheduler, clock: Clock) {
    val stopRequested = CompletableDeferred<Unit>()
    val ticking = AtomicBoolean(false)
    val job = launch {
        while (!stopRequested.isCompleted) {
            ticking.set(true)
            try {
                scheduler.tick()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("Alarm scheduler tick failed", e)
            } finally {
                ticking.set(false)
            }
            // Sleeps until the next minute, or ends the loop at once when the server stops.
            withTimeoutOrNull(untilNextMinute(clock).toMillis()) { stopRequested.await() }
        }
    }
    monitor.subscribe(ApplicationStopping) {
        stopRequested.complete(Unit)
        val wasTicking = ticking.get()
        if (wasTicking) log.info("Alarm scheduler: waiting for the running tick to finish")
        val finished = runBlocking { withTimeoutOrNull(TICK_STOP_TIMEOUT) { job.join() } } != null
        if (finished) {
            log.info(if (wasTicking) "Alarm scheduler: the running tick finished, stopped" else "Alarm scheduler stopped")
        } else {
            log.warn("Alarm scheduler: the running tick did not finish within $TICK_STOP_TIMEOUT, cancelling it")
            job.cancel()
        }
    }
}

private fun untilNextMinute(clock: Clock): Duration {
    val now = clock.instant()
    return Duration.between(now, now.truncatedTo(ChronoUnit.MINUTES).plus(Duration.ofMinutes(1)))
}
