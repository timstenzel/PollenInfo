package ch.stenzel.tim.polleninfo.server

import ch.stenzel.tim.polleninfo.server.account.domain.Argon2PasswordHasher
import ch.stenzel.tim.polleninfo.server.account.store.ExposedDiaryStore
import ch.stenzel.tim.polleninfo.server.account.store.ExposedSessionStore
import ch.stenzel.tim.polleninfo.server.account.store.ExposedUserStore
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
import ch.stenzel.tim.polleninfo.server.plugins.configureAccountRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureAlarmRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureLogging
import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSecurity
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.plugins.meteoSwissMeasurementService
import ch.stenzel.tim.polleninfo.server.plugins.meteoSwissPollenService
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.engine.connector
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
    // On SIGTERM (`docker compose stop`) Ktor's shutdown hook stops Netty — new requests are refused,
    // ones in flight get the grace period — and then the application, whose alarm scheduler lets a
    // running tick finish (TICK_STOP_TIMEOUT). compose.yaml's stop_grace_period covers the sum.
    embeddedServer(
        factory = Netty,
        configure = {
            connector {
                port = config.port
                host = "0.0.0.0"
            }
            shutdownGracePeriod = SHUTDOWN_GRACE_PERIOD_MILLIS
            shutdownTimeout = SHUTDOWN_TIMEOUT_MILLIS
        },
        module = { module(config) },
    ).start(wait = true)
}

private const val SHUTDOWN_GRACE_PERIOD_MILLIS = 2_000L
private const val SHUTDOWN_TIMEOUT_MILLIS = 5_000L

/**
 * Builds every long-lived collaborator once. The [MeasurementService] is built here rather than
 * inside routing so that the routes and the alarm scheduler share one reading cache; the history
 * shares its upstream client. The scheduler does not use the history.
 */
fun Application.module(config: ServerConfig) {
    configureSerialization()
    configureLogging()
    configureSecurity(rateLimits = config.rateLimits, trustedProxy = config.trustedProxy)
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
    val sessions = ExposedSessionStore(database.database)
    configureAccountRouting(
        users = ExposedUserStore(database.database),
        sessions = sessions,
        diary = ExposedDiaryStore(database.database),
        hasher = Argon2PasswordHasher(),
        clock = clock,
    )

    launchAlarmScheduler(
        scheduler = AlarmScheduler(
            alarms = alarms,
            devices = devices,
            sessions = sessions,
            log = ExposedNotificationLog(database.database),
            state = ExposedSchedulerState(database.database),
            measurements = measurementService,
            push = pushSender(config),
            clock = clock,
        ),
        clock = clock,
    )
}
