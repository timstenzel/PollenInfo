package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * What each threshold alert has already notified about, per pollen type and Swiss calendar day —
 * the record behind "at most once per pollen type per day". Keyed per type, so a type added to an
 * alert later in the day can still notify.
 */
interface NotificationLog {

    /** The types [alarmId] has notified about on [date]. */
    suspend fun notifiedSpecies(alarmId: AlarmId, date: LocalDate): Set<PollenSpecies>

    /**
     * Records that [alarmId] notified about [species] on [date]. Recording a type twice is harmless,
     * and recording for an alarm that no longer exists does nothing.
     */
    suspend fun record(alarmId: AlarmId, species: Set<PollenSpecies>, date: LocalDate)

    /** Forgets every day before [date]; nothing older than today is ever asked for again. */
    suspend fun pruneBefore(date: LocalDate)
}

class ExposedNotificationLog(private val database: Database) : NotificationLog {

    override suspend fun notifiedSpecies(alarmId: AlarmId, date: LocalDate): Set<PollenSpecies> =
        withContext(Dispatchers.IO) {
            transaction(database) {
                NotificationLogTable.selectAll()
                    .where {
                        (NotificationLogTable.alarmId eq alarmId.value) and
                            (NotificationLogTable.localDate eq date.toString())
                    }
                    .map { PollenSpecies.valueOf(it[NotificationLogTable.species]) }
                    .toSet()
            }
        }

    override suspend fun record(alarmId: AlarmId, species: Set<PollenSpecies>, date: LocalDate) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                // An alarm deleted since it was evaluated would fail the foreign key; there is
                // nothing left to protect from a repeat, so there is nothing to record.
                val alarmExists = AlarmsTable.selectAll().where { AlarmsTable.id eq alarmId.value }.any()
                if (!alarmExists) return@transaction
                species.forEach { type ->
                    NotificationLogTable.insertIgnore {
                        it[NotificationLogTable.alarmId] = alarmId.value
                        it[NotificationLogTable.species] = type.name
                        it[localDate] = date.toString()
                    }
                }
            }
        }
    }

    override suspend fun pruneBefore(date: LocalDate) {
        withContext(Dispatchers.IO) {
            // ISO dates compare as strings in calendar order.
            transaction(database) { NotificationLogTable.deleteWhere { localDate less date.toString() } }
        }
    }
}
