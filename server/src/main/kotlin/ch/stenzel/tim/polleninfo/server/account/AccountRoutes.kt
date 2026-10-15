package ch.stenzel.tim.polleninfo.server.account

import ch.stenzel.tim.polleninfo.server.account.domain.AccountValidation
import ch.stenzel.tim.polleninfo.server.account.domain.AccountValidationResult
import ch.stenzel.tim.polleninfo.server.account.domain.DiaryEntry
import ch.stenzel.tim.polleninfo.server.account.domain.DiaryFeeling
import ch.stenzel.tim.polleninfo.server.account.domain.PasswordHasher
import ch.stenzel.tim.polleninfo.server.account.model.DiaryAnswerDto
import ch.stenzel.tim.polleninfo.server.account.model.DiaryEntryDto
import ch.stenzel.tim.polleninfo.server.account.model.ErrorDto
import ch.stenzel.tim.polleninfo.server.account.model.LoginDto
import ch.stenzel.tim.polleninfo.server.account.model.RegisterDto
import ch.stenzel.tim.polleninfo.server.account.model.SessionDto
import ch.stenzel.tim.polleninfo.server.account.store.DiaryStore
import ch.stenzel.tim.polleninfo.server.account.store.RecordResult
import ch.stenzel.tim.polleninfo.server.account.store.RegisterResult
import ch.stenzel.tim.polleninfo.server.account.store.SessionStore
import ch.stenzel.tim.polleninfo.server.account.store.UserStore
import ch.stenzel.tim.polleninfo.server.plugins.ACCOUNT_LIMIT
import ch.stenzel.tim.polleninfo.server.plugins.USER_AUTH
import ch.stenzel.tim.polleninfo.server.plugins.USER_LIMIT
import ch.stenzel.tim.polleninfo.server.plugins.UserPrincipal
import ch.stenzel.tim.polleninfo.server.pollen.domain.SWISS_ZONE
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Personal accounts. Registering and logging in return a session token — the key to everything
 * under `/users/me`, sent as `Authorization: Bearer …`. The email and the token travel only in
 * bodies and headers, never in a path or query, so neither reaches the call log.
 *
 * Registration and login share one rate-limit bucket per client address, [ACCOUNT_LIMIT]; the
 * `/users/me` routes are limited per account under [USER_LIMIT], inside authentication, so an
 * unknown or expired token is a `401` before its body is read and never gets a bucket.
 *
 * Reading the diary takes its date range in the query; only dates travel there, nothing personal.
 *
 * The diary's "today" is always the server's: the Swiss calendar day of [clock]'s instant, so an
 * answer is filed under the same date as the daily means it is compared with, whatever zone or clock
 * the phone has.
 */
fun Route.accountRoutes(
    users: UserStore,
    sessions: SessionStore,
    diary: DiaryStore,
    hasher: PasswordHasher,
    clock: Clock,
) {
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

            post("/session") {
                val body = try {
                    call.receive<LoginDto>()
                } catch (e: BadRequestException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorDto("Expected {\"email\": \"…\", \"password\": \"…\"}"),
                    )
                }
                // An unknown email does the work of a verify too and gets the very same answer as a
                // wrong password, so the login never tells whether an address has an account.
                val userId = withContext(Dispatchers.Default) {
                    val stored = AccountValidation.normalizeEmail(body.email)?.let { users.credentials(it) }
                    if (stored == null) {
                        hasher.verifyDummy(body.password)
                        null
                    } else {
                        stored.userId.takeIf { hasher.verify(body.password, stored.passwordHash) }
                    }
                } ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorDto(INVALID_CREDENTIALS))
                val token = sessions.create(userId, clock.instant())
                call.respond(HttpStatusCode.Created, SessionDto(token.value))
            }
        }

        authenticate(USER_AUTH) {
            rateLimit(USER_LIMIT) {
                route("/me") {
                    delete("/session") {
                        // Only this phone's session; the account's other phones stay logged in.
                        sessions.delete(call.userPrincipal().token)
                        call.respond(HttpStatusCode.NoContent)
                    }

                    route("/diary") {
                        post {
                            val body = try {
                                call.receive<DiaryAnswerDto>()
                            } catch (e: BadRequestException) {
                                return@post call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorDto("Expected {\"feeling\": \"…\"}"),
                                )
                            }
                            val feeling = DiaryFeeling.fromNameOrNull(body.feeling)
                                ?: return@post call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorDto("feeling must be one of ${DiaryFeeling.entries.joinToString()}"),
                                )
                            val now = clock.instant()
                            // The first answer of the day is 201; any later one gets the stored answer back.
                            when (val result = diary.record(call.userPrincipal().userId, now.swissDate(), feeling, now)) {
                                is RecordResult.Recorded -> call.respond(HttpStatusCode.Created, result.entry.toDto())
                                is RecordResult.AlreadyAnswered -> call.respond(HttpStatusCode.OK, result.entry.toDto())
                            }
                        }

                        get {
                            val range = diaryRange(call.request.queryParameters["from"], call.request.queryParameters["until"])
                                ?: return@get call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorDto(
                                        "Expected from and until as YYYY-MM-DD, until not before from, " +
                                            "at most $MAX_DIARY_RANGE_DAYS days",
                                    ),
                                )
                            val entries = diary.entries(call.userPrincipal().userId, range.start, range.endInclusive)
                            call.respond(entries.map { it.toDto() })
                        }

                        get("/today") {
                            val entry = diary.entry(call.userPrincipal().userId, clock.instant().swissDate())
                            if (entry == null) call.respond(HttpStatusCode.NoContent) else call.respond(entry.toDto())
                        }
                    }
                }
            }
        }
    }
}

private fun Instant.swissDate(): LocalDate = atZone(SWISS_ZONE).toLocalDate()

/**
 * The days `GET /users/me/diary` reads, from [from] to [until] inclusive, or `null` if either is
 * missing or not an ISO date, [until] is before [from], or the range is longer than
 * [MAX_DIARY_RANGE_DAYS] — enough for the Diary's year, even one reaching across a leap day.
 */
private fun diaryRange(from: String?, until: String?): ClosedRange<LocalDate>? {
    val start = from?.toLocalDateOrNull() ?: return null
    val end = until?.toLocalDateOrNull() ?: return null
    if (end < start || ChronoUnit.DAYS.between(start, end) + 1 > MAX_DIARY_RANGE_DAYS) return null
    return start..end
}

private fun String.toLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this)
    } catch (e: DateTimeParseException) {
        null
    }

/** The longest range, in days counting both ends, one diary read may cover. */
const val MAX_DIARY_RANGE_DAYS = 366

private fun DiaryEntry.toDto() = DiaryEntryDto(date = date.toString(), feeling = feeling.name)

/** The one answer to a login that fails, whichever of the two was wrong. */
private const val INVALID_CREDENTIALS = "Email or password is incorrect"

/** The authenticated account; only called inside [authenticate], where a principal always exists. */
private fun ApplicationCall.userPrincipal(): UserPrincipal = checkNotNull(principal<UserPrincipal>())
