package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.pollenRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(
    thresholds: PollenThresholds = PollenThresholds(),
) {
    routing {
        get("/health") {
            call.respondText("OK")
        }
        pollenRoutes(thresholds)
    }
}
