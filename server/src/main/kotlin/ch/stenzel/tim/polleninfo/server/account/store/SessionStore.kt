package ch.stenzel.tim.polleninfo.server.account.store

import ch.stenzel.tim.polleninfo.server.account.domain.SessionToken
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.account.domain.hash
import ch.stenzel.tim.polleninfo.server.account.domain.newSessionToken
import ch.stenzel.tim.polleninfo.server.alarm.store.toTimestamp
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** The logins: one session per logged-in phone, so a user can be logged in on several at once. */
interface SessionStore {

    /**
     * Stores a new session of [userId], created and last used at [now], and returns its token — the
     * only time it is ever available: the store keeps its hash alone.
     */
    suspend fun create(userId: UserId, now: Instant): SessionToken
}

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
}
