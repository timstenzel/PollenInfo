package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.alarm.alarmRoutes
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.TtlCache
import ch.stenzel.tim.polleninfo.server.pollen.pollenRoutes
import ch.stenzel.tim.polleninfo.server.pollen.upstream.MeteoSwissPollenService
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * The health and pollen routes. Collaborators are parameters with test-friendly defaults, so a test
 * can install the exact configuration it wants to assert against. `Application.module()` builds the
 * production ones and passes them all in.
 *
 * Nothing here needs a database, so a test of these routes runs without one.
 */
fun Application.configureRouting(
    thresholds: PollenThresholds = PollenThresholds(),
    measurementService: MeasurementService = meteoSwissMeasurementService(thresholds),
    historyService: HistoryService = HistoryService(meteoSwissPollenService(), thresholds),
) {
    routing {
        get("/health") {
            call.respondText("OK")
        }
        pollenRoutes(thresholds, measurementService, historyService)
    }
}

/**
 * The device and alarm routes with the device authentication they need, over stores the caller has
 * built — there are no defaults.
 */
fun Application.configureAlarmRouting(devices: DeviceStore, alarms: AlarmStore) {
    configureDeviceAuthentication(devices)
    routing {
        alarmRoutes(devices, alarms)
    }
}

/**
 * The production upstream wiring for readings: the live MeteoSwiss service, cached per station for
 * [MeasurementService.CACHE_TTL].
 *
 * There is no offline alternative to reach for here — the only other `PollenService` is the fake in
 * the test source set, and tests install their own [MeasurementService] rather than switching a
 * flag on this one.
 */
fun Application.meteoSwissMeasurementService(
    thresholds: PollenThresholds,
    pollenService: PollenService = meteoSwissPollenService(),
): MeasurementService = MeasurementService(
    pollenService = pollenService,
    thresholds = thresholds,
    cache = TtlCache(MeasurementService.CACHE_TTL),
)

/**
 * The live MeteoSwiss service over its own HTTP client, closed when the application stops.
 * `Application.module()` builds one and hands it to both the measurements and the history.
 */
fun Application.meteoSwissPollenService(): PollenService {
    val client = HttpClient(CIO) {
        // An upstream that accepts the connection and then stalls must fail the request rather than
        // hold it open; without this a hung file service would pin request threads indefinitely.
        install(HttpTimeout) {
            requestTimeoutMillis = UPSTREAM_TIMEOUT_MILLIS
            connectTimeoutMillis = UPSTREAM_TIMEOUT_MILLIS
        }
    }
    monitor.subscribe(ApplicationStopped) { client.close() }
    return MeteoSwissPollenService(client)
}

private const val UPSTREAM_TIMEOUT_MILLIS = 15_000L
