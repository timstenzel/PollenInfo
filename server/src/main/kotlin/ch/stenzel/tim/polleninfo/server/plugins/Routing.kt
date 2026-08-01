package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.pollenRoutes
import ch.stenzel.tim.polleninfo.server.pollen.upstream.ClasspathPollenFileSource
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Collaborators are parameters with production defaults, so a test can install the exact
 * configuration it wants to assert against.
 */
fun Application.configureRouting(
    thresholds: PollenThresholds = PollenThresholds(),
    measurementService: MeasurementService =
        MeasurementService(ClasspathPollenFileSource(), thresholds),
) {
    routing {
        get("/health") {
            call.respondText("OK")
        }
        pollenRoutes(thresholds, measurementService)
    }
}
