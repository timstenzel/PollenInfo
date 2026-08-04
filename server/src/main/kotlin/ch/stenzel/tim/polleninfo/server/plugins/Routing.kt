package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.pollenRoutes
import ch.stenzel.tim.polleninfo.server.pollen.upstream.MeteoSwissPollenService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Collaborators are parameters with production defaults, so a test can install the exact
 * configuration it wants to assert against.
 */
fun Application.configureRouting(
    thresholds: PollenThresholds = PollenThresholds(),
    measurementService: MeasurementService = meteoSwissMeasurementService(thresholds),
) {
    routing {
        get("/health") {
            call.respondText("OK")
        }
        pollenRoutes(thresholds, measurementService)
    }
}

/**
 * The production upstream wiring: readings come from the live MeteoSwiss service.
 *
 * There is no offline alternative to reach for here — the only other `PollenService` is the fake in
 * the test source set, and tests install their own [MeasurementService] rather than switching a
 * flag on this one.
 */
private fun Application.meteoSwissMeasurementService(
    thresholds: PollenThresholds,
): MeasurementService {
    val client = HttpClient(CIO) {
        // An upstream that accepts the connection and then stalls must fail the request rather than
        // hold it open; without this a hung file service would pin request threads indefinitely.
        install(HttpTimeout) {
            requestTimeoutMillis = UPSTREAM_TIMEOUT_MILLIS
            connectTimeoutMillis = UPSTREAM_TIMEOUT_MILLIS
        }
    }
    monitor.subscribe(ApplicationStopped) { client.close() }
    return MeasurementService(MeteoSwissPollenService(client), thresholds)
}

private const val UPSTREAM_TIMEOUT_MILLIS = 15_000L
