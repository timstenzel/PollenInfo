package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.http.parsing.ParseException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.request.authorization

/** The name of the bearer provider every `/devices/me` route authenticates with. */
const val DEVICE_AUTH = "device"

/** The install a request authenticated as. */
data class DevicePrincipal(val deviceId: DeviceId)

/**
 * Installs the `device` bearer provider: `Authorization: Bearer <device token>` resolves to the
 * device holding it. A missing, malformed or unknown token is a `401` with `WWW-Authenticate:
 * Bearer`, which is how the app learns to register again.
 */
fun Application.configureDeviceAuthentication(devices: DeviceStore) {
    install(Authentication) {
        bearer(DEVICE_AUTH) {
            // Ktor answers a header it cannot parse ("Bearer a b") with 400; to the app that is the
            // same as no token at all, so it gets the same 401.
            authHeader { call ->
                call.request.authorization()?.let { header ->
                    try {
                        parseAuthorizationHeader(header)
                    } catch (e: ParseException) {
                        null
                    }
                }
            }
            authenticate { credential ->
                // A token that cannot have been issued is refused without a database lookup.
                DeviceToken.parseOrNull(credential.token)
                    ?.let { devices.authenticate(it) }
                    ?.let(::DevicePrincipal)
            }
        }
    }
}
