package ch.stenzel.tim.polleninfo.server

import ch.stenzel.tim.polleninfo.server.plugins.configureLogging
import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureLogging()
    configureRouting()
}
