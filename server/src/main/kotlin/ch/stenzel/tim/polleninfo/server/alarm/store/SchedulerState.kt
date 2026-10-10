package ch.stenzel.tim.polleninfo.server.alarm.store

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

/**
 * The last minute the alarm scheduler finished, kept across restarts so a new instance knows which
 * minutes it missed and which it must not process again.
 */
interface SchedulerState {

    /** The start of the last finished minute, or `null` if the scheduler has never finished one. */
    suspend fun lastMinute(): Instant?

    /** Records [minute] (its start) as finished. */
    suspend fun recordMinute(minute: Instant)
}

class ExposedSchedulerState(private val database: Database) : SchedulerState {

    override suspend fun lastMinute(): Instant? = withContext(Dispatchers.IO) {
        transaction(database) {
            SchedulerStateTable.selectAll().singleOrNull()?.get(SchedulerStateTable.lastMinute)?.toInstant()
        }
    }

    override suspend fun recordMinute(minute: Instant) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                SchedulerStateTable.upsert {
                    it[id] = true
                    it[lastMinute] = minute.toTimestamp()
                }
            }
        }
    }
}
