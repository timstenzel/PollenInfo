package ch.stenzel.tim.polleninfo.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.http.HttpStatusCode
import org.slf4j.event.Level

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                text = "Internal Server Error: ${cause.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}
