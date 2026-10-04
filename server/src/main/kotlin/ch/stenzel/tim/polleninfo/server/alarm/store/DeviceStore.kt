package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.newDeviceId
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** The installs that have registered for alarms. */
interface DeviceStore {

    /** Stores a new device with its push token and returns the id issued for it. */
    suspend fun register(fcmToken: String): DeviceId

    suspend fun exists(id: DeviceId): Boolean
}

class ExposedDeviceStore(
    private val database: Database,
    private val clock: Clock = Clock.systemUTC(),
) : DeviceStore {

    override suspend fun register(fcmToken: String): DeviceId = withContext(Dispatchers.IO) {
        val id = newDeviceId()
        transaction(database) {
            DevicesTable.insert {
                it[DevicesTable.id] = id.value
                it[DevicesTable.fcmToken] = fcmToken
                it[createdAt] = clock.millis()
            }
        }
        id
    }

    override suspend fun exists(id: DeviceId): Boolean = withContext(Dispatchers.IO) {
        transaction(database) { deviceExists(id) }
    }
}

internal fun deviceExists(id: DeviceId): Boolean =
    DevicesTable.selectAll().where { DevicesTable.id eq id.value }.any()
