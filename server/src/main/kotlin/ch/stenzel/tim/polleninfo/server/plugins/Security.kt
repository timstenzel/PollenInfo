package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.config.RateLimits
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.engine.defaultExceptionStatusCode
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `POST /devices`, per client address. */
val REGISTER_LIMIT = RateLimitName("register")

/** Everything under `/devices/me`, per authenticated device. */
val DEVICE_LIMIT = RateLimitName("device")

/** Everything under `/pollen`, per client address. */
val POLLEN_LIMIT = RateLimitName("pollen")

/** The largest request body the server reads; an alarm is well under 1 KB. */
const val MAX_BODY_BYTES = 16L * 1024

/**
 * What protects the server from its callers: the client address behind the proxy, the three rate
 * limiters the routes wrap themselves in, the body size cap and error answers that reveal nothing.
 *
 * Install it before the routes: [io.ktor.server.plugins.ratelimit.rateLimit] looks its limiter up
 * when the route is built. There is deliberately no CORS plugin — no browser page on another site
 * may call this API.
 */
fun Application.configureSecurity(rateLimits: RateLimits = RateLimits(), trustedProxy: Boolean = false) {
    // Only when nothing but our own proxy can reach the server; otherwise every caller could name
    // its own address and so its own rate-limit bucket. The proxy appends the address it saw, so
    // the last value is the one it vouches for.
    if (trustedProxy) {
        install(XForwardedHeaders) { useLastProxy() }
    }

    install(RateLimit) {
        register(REGISTER_LIMIT) {
            rateLimiter(limit = rateLimits.registerPerHour, refillPeriod = 1.hours)
            requestKey { call -> call.clientAddress() }
        }
        // Installed inside `authenticate`, so only a known device gets a bucket: random unknown
        // tokens are refused with a 401 before they reach this limiter.
        register(DEVICE_LIMIT) {
            rateLimiter(limit = rateLimits.devicePerMinute, refillPeriod = 1.minutes)
            requestKey { call -> call.principal<DevicePrincipal>()?.deviceId ?: call.clientAddress() }
        }
        register(POLLEN_LIMIT) {
            rateLimiter(limit = rateLimits.pollenPerMinute, refillPeriod = 1.minutes)
            requestKey { call -> call.clientAddress() }
        }
    }

    // Both a declared Content-Length and a chunked body: the latter is counted while it is read.
    install(RequestBodyLimit) {
        bodyLimit { MAX_BODY_BYTES }
    }

    install(StatusPages) {
        // The rate limiter answers with a bare status and its own Retry-After header, which stays.
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respondError(status, "Too many requests")
        }
        exception<Throwable> { call, cause ->
            val status = cause.knownStatus()
            if (status != null) {
                call.respondError(status, status.description)
            } else {
                // The details go to the log only: no exception text, stack trace or SQL reaches a
                // client.
                val request = "${call.request.httpMethod.value} ${call.request.path()}"
                call.application.log.error("Unhandled error in $request", cause)
                call.respondError(HttpStatusCode.InternalServerError, "internal error")
            }
        }
    }
}

private fun ApplicationCall.clientAddress(): String = request.origin.remoteAddress

/**
 * The status Ktor's own exceptions stand for — a malformed body, an unsupported media type, a body
 * too large — or `null` for an unexpected failure. An oversized body is a `413` even if a reader
 * wrapped the limit's exception in another one.
 */
private fun Throwable.knownStatus(): HttpStatusCode? {
    if (generateSequence(this) { it.cause }.any { it is PayloadTooLargeException }) {
        return HttpStatusCode.PayloadTooLarge
    }
    return defaultExceptionStatusCode(this)?.takeIf { it.value < 500 }
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
    respondText(errorJson.encodeToString(ErrorBody(message)), ContentType.Application.Json, status)
}

/** The `{error}` shape of every other error body, written here without content negotiation. */
@Serializable
private data class ErrorBody(val error: String)

private val errorJson = Json
