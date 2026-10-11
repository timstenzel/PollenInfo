package ch.stenzel.tim.polleninfo.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level

/**
 * One line per call: status, method, path and duration — spelled out rather than left to the
 * default, so no header (the device token travels in `Authorization`), query or body can ever be
 * logged.
 */
fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
        // The format below has no colours; with them on, the plugin loads Jansi's native library,
        // which the production container's read-only root and noexec /tmp refuse.
        disableDefaultColors()
        format { call ->
            "${call.response.status()}: ${call.request.httpMethod.value} ${call.request.path()} " +
                "in ${call.processingTimeMillis()} ms"
        }
    }
}
