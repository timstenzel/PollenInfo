package ch.stenzel.tim.polleninfo.server.account.store

import ch.stenzel.tim.polleninfo.server.account.domain.SessionToken
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.account.domain.hash
import ch.stenzel.tim.polleninfo.server.account.domain.newSessionToken
import ch.stenzel.tim.polleninfo.server.alarm.store.toTimestamp
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/** The logins: one session per logged-in phone, so a user can be logged in on several at once. */
interface SessionStore {

    /**
     * Stores a new session of [userId], created and last used at [now], and returns its token — the
     * only time it is ever available: the store keeps its hash alone.
     */
    suspend fun create(userId: UserId, now: Instant): SessionToken

    /**
     * The user [token] logs in, or `null` for a token no session has or one last used more than
     * [SESSION_EXPIRY] before [now] — refused even before a cleanup has removed it. Records that the
     * session was used, at most once per [LAST_USED_RESOLUTION], so an active phone does not write on
     * every call.
     */
    suspend fun authenticate(token: SessionToken, now: Instant): UserId?

    /** Ends the session [token] belongs to, and only that one; nothing happens for an unknown token. */
    suspend fun delete(token: SessionToken)
}

/** How long a session stays valid after its last use; exactly this long is still valid. */
val SESSION_EXPIRY = 90.days

/** How often at most a session's `last_used_at` is written: a day is all its expiry needs. */
val LAST_USED_RESOLUTION = 1.days

class ExposedSessionStore(private val database: Database) : SessionStore {

    override suspend fun create(userId: UserId, now: Instant): SessionToken = withContext(Dispatchers.IO) {
        val token = newSessionToken()
        transaction(database) {
            SessionsTable.insert {
                it[tokenHash] = token.hash()
                it[SessionsTable.userId] = userId.value
                it[createdAt] = now.toTimestamp()
                it[lastUsedAt] = now.toTimestamp()
            }
        }
        token
    }

    override suspend fun authenticate(token: SessionToken, now: Instant): UserId? = withContext(Dispatchers.IO) {
        val hash = token.hash()
        transaction(database) {
            val row = SessionsTable.select(SessionsTable.userId, SessionsTable.lastUsedAt)
                .where { SessionsTable.tokenHash eq hash }
                .singleOrNull()
                ?: return@transaction null
            val lastUsed = row[SessionsTable.lastUsedAt].toInstant()
            if (lastUsed.isBefore(now.minus(SESSION_EXPIRY.toJavaDuration()))) return@transaction null
            // Conditional, so a call within a day of the last refresh writes nothing.
            val due = now.minus(LAST_USED_RESOLUTION.toJavaDuration()).toTimestamp()
            SessionsTable.update({ (SessionsTable.tokenHash eq hash) and (SessionsTable.lastUsedAt lessEq due) }) {
                it[lastUsedAt] = now.toTimestamp()
            }
            UserId(row[SessionsTable.userId])
        }
    }

    override suspend fun delete(token: SessionToken) {
        val hash = token.hash()
        withContext(Dispatchers.IO) {
            transaction(database) {
                SessionsTable.deleteWhere { SessionsTable.tokenHash eq hash }
            }
        }
    }
}
