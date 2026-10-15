package ch.stenzel.tim.polleninfo.server.account.store

import ch.stenzel.tim.polleninfo.server.account.domain.AccountSpec
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.alarm.store.toTimestamp
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** The accounts. */
interface UserStore {

    /**
     * Stores a new account for [spec]'s email with [passwordHash], consented and created at [now] —
     * unless an account with that email exists. The password in [spec] is never stored.
     */
    suspend fun register(spec: AccountSpec, passwordHash: String, now: Instant): RegisterResult
}

sealed interface RegisterResult {
    data class Registered(val userId: UserId) : RegisterResult

    /** An account with this email exists already; nothing was stored. */
    data object EmailTaken : RegisterResult
}

class ExposedUserStore(private val database: Database) : UserStore {

    override suspend fun register(spec: AccountSpec, passwordHash: String, now: Instant): RegisterResult =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID()
            // The unique index decides, not a lookup first: two concurrent registrations of one
            // email cannot both pass a check that neither has written yet.
            try {
                transaction(database) {
                    // A unique violation is the answer, not a glitch worth retrying.
                    maxAttempts = 1
                    UsersTable.insert {
                        it[UsersTable.id] = id
                        it[email] = spec.email
                        it[UsersTable.passwordHash] = passwordHash
                        it[consentedAt] = now.toTimestamp()
                        it[createdAt] = now.toTimestamp()
                    }
                }
                RegisterResult.Registered(UserId(id))
            } catch (e: ExposedSQLException) {
                if (e.sqlState == UNIQUE_VIOLATION) RegisterResult.EmailTaken else throw e
            }
        }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
