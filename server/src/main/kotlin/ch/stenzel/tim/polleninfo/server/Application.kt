package ch.stenzel.tim.polleninfo.server

import ch.stenzel.tim.polleninfo.server.alarm.push.pushSender
import ch.stenzel.tim.polleninfo.server.alarm.scheduler.AlarmScheduler
import ch.stenzel.tim.polleninfo.server.alarm.scheduler.launchAlarmScheduler
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedNotificationLog
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedSchedulerState
import ch.stenzel.tim.polleninfo.server.alarm.store.PollenInfoDatabase
import ch.stenzel.tim.polleninfo.server.config.ConfigException
import ch.stenzel.tim.polleninfo.server.config.ServerConfig
import ch.stenzel.tim.polleninfo.server.plugins.configureAlarmRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureLogging
import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.plugins.meteoSwissMeasurementService
import ch.stenzel.tim.polleninfo.server.plugins.meteoSwissPollenService
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Clock
import kotlin.system.exitProcess
import org.slf4j.LoggerFactory

fun main() {
    // Read once, before a port is opened: a configuration that cannot work stops the server here,
    // with every problem named, rather than failing on the first request that needs it.
    val config = try {
        ServerConfig.load(System.getenv(), ServerConfig::readFileOrNull)
    } catch (e: ConfigException) {
        LoggerFactory.getLogger("ch.stenzel.tim.polleninfo.server.Application").error(e.message)
        exitProcess(1)
    }
    embeddedServer(
        factory = Netty,
        port = config.port,
        host = "0.0.0.0",
        module = { module(config) },
    ).start(wait = true)
}

/**
 * Builds every long-lived collaborator once. The [MeasurementService] is built here rather than
 * inside routing so that the routes and the alarm scheduler share one reading cache; the history
 * shares its upstream client. The scheduler does not use the history.
 */
fun Application.module(config: ServerConfig) {
    configureSerialization()
    configureLogging()
    log.info("Starting in ${config.environment} on port ${config.port}")

    val thresholds = PollenThresholds()
    val pollenService = meteoSwissPollenService()
    val measurementService = meteoSwissMeasurementService(thresholds, pollenService)
    val historyService = HistoryService(pollenService, thresholds)
    val database = PollenInfoDatabase.open(config.database)
    monitor.subscribe(ApplicationStopped) { database.close() }
    log.info("Alarm database: ${config.database.url}")

    val devices = ExposedDeviceStore(database.database)
    val alarms = ExposedAlarmStore(database.database)

    configureRouting(
        thresholds = thresholds,
        measurementService = measurementService,
        historyService = historyService,
    )
    configureAlarmRouting(devices = devices, alarms = alarms)

    val clock = Clock.systemUTC()
    launchAlarmScheduler(
        scheduler = AlarmScheduler(
            alarms = alarms,
            devices = devices,
            log = ExposedNotificationLog(database.database),
            state = ExposedSchedulerState(database.database),
            measurements = measurementService,
            push = pushSender(config),
            clock = clock,
        ),
        clock = clock,
    )
}
