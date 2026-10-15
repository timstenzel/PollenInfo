package ch.stenzel.tim.polleninfo.server.account

import ch.stenzel.tim.polleninfo.server.account.domain.AccountValidation
import ch.stenzel.tim.polleninfo.server.account.domain.AccountValidationResult
import ch.stenzel.tim.polleninfo.server.account.domain.PasswordHasher
import ch.stenzel.tim.polleninfo.server.account.model.ErrorDto
import ch.stenzel.tim.polleninfo.server.account.model.RegisterDto
import ch.stenzel.tim.polleninfo.server.account.model.SessionDto
import ch.stenzel.tim.polleninfo.server.account.store.RegisterResult
import ch.stenzel.tim.polleninfo.server.account.store.SessionStore
import ch.stenzel.tim.polleninfo.server.account.store.UserStore
import ch.stenzel.tim.polleninfo.server.plugins.ACCOUNT_LIMIT
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Personal accounts. Registering returns a session token — the key to everything under
 * `/users/me`, sent as `Authorization: Bearer …`. The email and the token travel only in bodies and
 * headers, never in a path or query, so neither reaches the call log.
 *
 * Registration is rate-limited per client address under [ACCOUNT_LIMIT], the bucket the login
 * shares.
 */
fun Route.accountRoutes(users: UserStore, sessions: SessionStore, hasher: PasswordHasher, clock: Clock) {
    route("/users") {
        rateLimit(ACCOUNT_LIMIT) {
            post {
                val body = try {
                    call.receive<RegisterDto>()
                } catch (e: BadRequestException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorDto("Expected {\"email\": \"…\", \"password\": \"…\", \"consent\": true}"),
                    )
                }
                val spec = when (val result = AccountValidation.validateRegistration(body.email, body.password, body.consent)) {
                    is AccountValidationResult.Valid -> result.spec
                    is AccountValidationResult.Invalid ->
                        return@post call.respond(HttpStatusCode.BadRequest, ErrorDto(result.message))
                }
                // Argon2id is deliberately expensive; keep it off the request threads.
                val passwordHash = withContext(Dispatchers.Default) { hasher.hash(spec.password) }
                val now = clock.instant()
                when (val result = users.register(spec, passwordHash, now)) {
                    is RegisterResult.Registered -> {
                        val token = sessions.create(result.userId, now)
                        call.respond(HttpStatusCode.Created, SessionDto(token.value))
                    }
                    // Accepted limitation: without email delivery, registration has to say so.
                    RegisterResult.EmailTaken -> call.respond(
                        HttpStatusCode.Conflict,
                        ErrorDto("An account with this email exists already"),
                    )
                }
            }
        }
    }
}
