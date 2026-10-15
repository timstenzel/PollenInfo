package ch.stenzel.tim.polleninfo.server.account.store

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

/*
 * The Kotlin view of the account tables `db/migration/V1__init.sql` creates — the sibling of the
 * alarm store's `Tables.kt`. As there, the migrations own the schema and nothing here creates or
 * alters a table.
 */

/**
 * One row per account, keyed by an internal UUID. [email] is stored normalised (trimmed and
 * lowercased) and is unique, which is what makes two registrations of one address impossible.
 * [passwordHash] is an Argon2id PHC string, never the password.
 */
internal object UsersTable : Table("users") {
    val id = javaUUID("id")
    val email = text("email")
    val passwordHash = text("password_hash")
    val consentedAt = timestampWithTimeZone("consented_at")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * One row per logged-in phone. [tokenHash] is the SHA-256 of the session token — the token itself is
 * never stored. Rows go with their user.
 */
internal object SessionsTable : Table("sessions") {
    val tokenHash = binary("token_hash")
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestampWithTimeZone("created_at")
    val lastUsedAt = timestampWithTimeZone("last_used_at")

    override val primaryKey = PrimaryKey(tokenHash)
}

/**
 * The diary: one answer per user and Swiss calendar day, the key that makes a second answer for a
 * day impossible to store. [feeling] is a `DiaryFeeling` name. Rows go with their user.
 */
internal object DiaryEntriesTable : Table("diary_entries") {
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val date = date("date")
    val feeling = varchar("feeling", 16)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(userId, date)
}
