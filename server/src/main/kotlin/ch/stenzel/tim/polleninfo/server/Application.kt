package ch.stenzel.tim.polleninfo.server

import ch.stenzel.tim.polleninfo.server.alarm.push.pushSenderFromEnvironment
import ch.stenzel.tim.polleninfo.server.alarm.scheduler.AlarmScheduler
import ch.stenzel.tim.polleninfo.server.alarm.scheduler.launchAlarmScheduler
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.PollenInfoDatabase
import ch.stenzel.tim.polleninfo.server.plugins.configureLogging
import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.plugins.meteoSwissMeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Clock

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

/**
 * Builds every long-lived collaborator once. The [MeasurementService] is built here rather than
 * inside routing so that the routes and the alarm scheduler share one reading cache.
 */
fun Application.module() {
    configureSerialization()
    configureLogging()

    val thresholds = PollenThresholds()
    val measurementService = meteoSwissMeasurementService(thresholds)
    val database = PollenInfoDatabase.fromEnvironment()
    log.info("Alarm database: ${database.url}")

    val alarms = ExposedAlarmStore(database)

    configureRouting(
        thresholds = thresholds,
        measurementService = measurementService,
        database = database,
        devices = ExposedDeviceStore(database),
        alarms = alarms,
    )

    val clock = Clock.systemUTC()
    launchAlarmScheduler(
        scheduler = AlarmScheduler(
            alarms = alarms,
            measurements = measurementService,
            push = pushSenderFromEnvironment(),
            clock = clock,
        ),
        clock = clock,
    )
}
