package ch.stenzel.tim.polleninfo.server.account.store

import ch.stenzel.tim.polleninfo.server.account.domain.DiaryEntry
import ch.stenzel.tim.polleninfo.server.account.domain.DiaryFeeling
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.alarm.store.toTimestamp
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** The users' diaries: at most one answer per user and Swiss calendar day, and the first is final. */
interface DiaryStore {

    /**
     * Stores [feeling] as [userId]'s answer for [date], recorded at [now] — unless that day has an
     * answer already, which is then returned unchanged: a second answer never overwrites the first.
     */
    suspend fun record(userId: UserId, date: LocalDate, feeling: DiaryFeeling, now: Instant): RecordResult

    /** [userId]'s answer for [date], or `null` if that day has none. */
    suspend fun entry(userId: UserId, date: LocalDate): DiaryEntry?
}

sealed interface RecordResult {
    val entry: DiaryEntry

    /** [entry] is the answer just stored. */
    data class Recorded(override val entry: DiaryEntry) : RecordResult

    /** The day had an answer already; [entry] is that stored one, and nothing was written. */
    data class AlreadyAnswered(override val entry: DiaryEntry) : RecordResult
}

class ExposedDiaryStore(private val database: Database) : DiaryStore {

    override suspend fun record(userId: UserId, date: LocalDate, feeling: DiaryFeeling, now: Instant): RecordResult =
        withContext(Dispatchers.IO) {
            transaction(database) {
                // ON CONFLICT DO NOTHING, then a read: of two concurrent answers the one that lost the
                // insert reads the winner's, and neither overwrites the other.
                val inserted = DiaryEntriesTable.insertIgnore {
                    it[DiaryEntriesTable.userId] = userId.value
                    it[DiaryEntriesTable.date] = date
                    it[DiaryEntriesTable.feeling] = feeling.name
                    it[createdAt] = now.toTimestamp()
                }.insertedCount > 0
                val stored = checkNotNull(selectEntry(userId, date)) { "the diary entry just written is missing" }
                if (inserted) RecordResult.Recorded(stored) else RecordResult.AlreadyAnswered(stored)
            }
        }

    override suspend fun entry(userId: UserId, date: LocalDate): DiaryEntry? = withContext(Dispatchers.IO) {
        transaction(database) { selectEntry(userId, date) }
    }

    private fun selectEntry(userId: UserId, date: LocalDate): DiaryEntry? =
        DiaryEntriesTable.select(DiaryEntriesTable.date, DiaryEntriesTable.feeling)
            .where { (DiaryEntriesTable.userId eq userId.value) and (DiaryEntriesTable.date eq date) }
            .singleOrNull()
            ?.toEntry()

    private fun ResultRow.toEntry() =
        DiaryEntry(this[DiaryEntriesTable.date], DiaryFeeling.valueOf(this[DiaryEntriesTable.feeling]))
}
