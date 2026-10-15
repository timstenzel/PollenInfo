package ch.stenzel.tim.polleninfo.server.account.store

import ch.stenzel.tim.polleninfo.server.account.domain.AccountSpec
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.alarm.store.toTimestamp
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** The accounts. */
interface UserStore {

    /**
     * Stores a new account for [spec]'s email with [passwordHash], consented and created at [now] —
     * unless an account with that email exists. The password in [spec] is never stored.
     */
    suspend fun register(spec: AccountSpec, passwordHash: String, now: Instant): RegisterResult

    /** The account of [email] — already normalised — with its password hash, or `null` if there is none. */
    suspend fun credentials(email: String): StoredCredentials?

    /** The password hash of [userId]'s account, or `null` if there is no such account. */
    suspend fun passwordHash(userId: UserId): String?

    /**
     * Deletes [userId]'s account with its sessions and diary answers; `false` if there was no such
     * account.
     */
    suspend fun delete(userId: UserId): Boolean
}

/** What a login is checked against. Never prints the hash. */
class StoredCredentials(val userId: UserId, val passwordHash: String) {
    override fun toString(): String = "StoredCredentials(userId=$userId)"
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

    override suspend fun credentials(email: String): StoredCredentials? = withContext(Dispatchers.IO) {
        transaction(database) {
            UsersTable.select(UsersTable.id, UsersTable.passwordHash)
                .where { UsersTable.email eq email }
                .singleOrNull()
                ?.let { StoredCredentials(UserId(it[UsersTable.id]), it[UsersTable.passwordHash]) }
        }
    }

    override suspend fun passwordHash(userId: UserId): String? = withContext(Dispatchers.IO) {
        transaction(database) {
            UsersTable.select(UsersTable.passwordHash)
                .where { UsersTable.id eq userId.value }
                .singleOrNull()
                ?.get(UsersTable.passwordHash)
        }
    }

    override suspend fun delete(userId: UserId): Boolean = withContext(Dispatchers.IO) {
        // Sessions and diary answers go with the account through ON DELETE CASCADE.
        transaction(database) { UsersTable.deleteWhere { UsersTable.id eq userId.value } > 0 }
    }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
