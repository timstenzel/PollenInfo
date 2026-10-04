package ch.stenzel.tim.polleninfo.server

import ch.stenzel.tim.polleninfo.server.alarm.push.pushSenderFromEnvironment
import ch.stenzel.tim.polleninfo.server.alarm.scheduler.AlarmScheduler
import ch.stenzel.tim.polleninfo.server.alarm.scheduler.launchAlarmScheduler
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedNotificationLog
import ch.stenzel.tim.polleninfo.server.alarm.store.PollenInfoDatabase
import ch.stenzel.tim.polleninfo.server.plugins.configureLogging
import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.plugins.meteoSwissMeasurementService
import ch.stenzel.tim.polleninfo.server.plugins.meteoSwissPollenService
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryService
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
 * inside routing so that the routes and the alarm scheduler share one reading cache; the history
 * shares its upstream client. The scheduler does not use the history.
 */
fun Application.module() {
    configureSerialization()
    configureLogging()

    val thresholds = PollenThresholds()
    val pollenService = meteoSwissPollenService()
    val measurementService = meteoSwissMeasurementService(thresholds, pollenService)
    val historyService = HistoryService(pollenService, thresholds)
    val database = PollenInfoDatabase.fromEnvironment()
    log.info("Alarm database: ${database.url}")

    val devices = ExposedDeviceStore(database)
    val alarms = ExposedAlarmStore(database)

    configureRouting(
        thresholds = thresholds,
        measurementService = measurementService,
        historyService = historyService,
        database = database,
        devices = devices,
        alarms = alarms,
    )

    val clock = Clock.systemUTC()
    launchAlarmScheduler(
        scheduler = AlarmScheduler(
            alarms = alarms,
            devices = devices,
            log = ExposedNotificationLog(database),
            measurements = measurementService,
            push = pushSenderFromEnvironment(),
            clock = clock,
        ),
        clock = clock,
    )
}
