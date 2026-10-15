package ch.stenzel.tim.polleninfo.server.plugins

import ch.stenzel.tim.polleninfo.server.account.domain.SessionToken
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.account.store.SessionStore
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.http.parsing.ParseException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.bearer
import io.ktor.server.request.authorization
import java.time.Clock

/** The name of the bearer provider every `/devices/me` route authenticates with. */
const val DEVICE_AUTH = "device"

/** The name of the bearer provider every `/users/me` route authenticates with. */
const val USER_AUTH = "user"

/** The install a request authenticated as. */
data class DevicePrincipal(val deviceId: DeviceId)

/**
 * The account a request authenticated as, and the session it used — kept so that logging out can
 * end exactly that session.
 */
data class UserPrincipal(val userId: UserId, val token: SessionToken)

/*
 * Each provider is added with `authentication { }`, which installs the Authentication plugin the
 * first time and extends it after that — it can be installed only once. So the device and the account
 * routes each bring their own provider, in either order, and a test can install just one of them.
 */

/**
 * Adds the `device` bearer provider: `Authorization: Bearer <device token>` resolves to the device
 * holding it. A missing, malformed or unknown token is a `401` with `WWW-Authenticate: Bearer`, which
 * is how the app learns to register again.
 */
fun Application.configureDeviceAuthentication(devices: DeviceStore) {
    authentication {
        bearer(DEVICE_AUTH) {
            authHeader(::lenientAuthHeader)
            authenticate { credential ->
                // A token that cannot have been issued is refused without a database lookup.
                DeviceToken.parseOrNull(credential.token)
                    ?.let { devices.authenticate(it) }
                    ?.let(::DevicePrincipal)
            }
        }
    }
}

/**
 * Adds the `user` bearer provider: `Authorization: Bearer <session token>` resolves to the account
 * the session belongs to, as of [clock]. A missing, malformed, unknown or expired token is the same
 * `401` with `WWW-Authenticate: Bearer`, which is how the app learns it has been logged out.
 */
fun Application.configureUserAuthentication(sessions: SessionStore, clock: Clock) {
    authentication {
        bearer(USER_AUTH) {
            authHeader(::lenientAuthHeader)
            authenticate { credential ->
                // A token that cannot have been issued is refused without a database lookup.
                val token = SessionToken.parseOrNull(credential.token) ?: return@authenticate null
                sessions.authenticate(token, clock.instant())?.let { UserPrincipal(it, token) }
            }
        }
    }
}

/**
 * Ktor answers a header it cannot parse ("Bearer a b") with 400; to the app that is the same as no
 * token at all, so it gets the same 401.
 */
private fun lenientAuthHeader(call: ApplicationCall): HttpAuthHeader? =
    call.request.authorization()?.let { header ->
        try {
            parseAuthorizationHeader(header)
        } catch (e: ParseException) {
            null
        }
    }
