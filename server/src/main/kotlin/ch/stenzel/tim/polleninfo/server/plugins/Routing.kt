package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.push.PushRoutes
import ch.stenzel.tim.polleninfo.server.push.PushService
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(pushService: PushService) {
    routing {
        get("/health") {
            call.respondText("OK")
        }
        PushRoutes.run { pushRoutes(pushService) }
    }
}
