package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.newDeviceId
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/** The installs that have registered for alarms. */
interface DeviceStore {

    /** Stores a new device with its push token and returns the id issued for it. */
    suspend fun register(fcmToken: String): DeviceId

    /** Replaces the device's push token, as when FCM rotates it; `false` for an unknown device. */
    suspend fun updateToken(id: DeviceId, fcmToken: String): Boolean

    /**
     * Drops the device's push token if it is still [fcmToken], keeping the device and its alarms, so
     * the scheduler stops processing them until the app sends a new one. Conditional so that a token
     * the push service rejected can never wipe a newer one the app sent in the meantime.
     */
    suspend fun clearToken(id: DeviceId, fcmToken: String)

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

    override suspend fun updateToken(id: DeviceId, fcmToken: String): Boolean = withContext(Dispatchers.IO) {
        transaction(database) {
            DevicesTable.update({ DevicesTable.id eq id.value }) {
                it[DevicesTable.fcmToken] = fcmToken
            } > 0
        }
    }

    override suspend fun clearToken(id: DeviceId, fcmToken: String) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                DevicesTable.update({ (DevicesTable.id eq id.value) and (DevicesTable.fcmToken eq fcmToken) }) {
                    it[DevicesTable.fcmToken] = null
                }
            }
        }
    }

    override suspend fun exists(id: DeviceId): Boolean = withContext(Dispatchers.IO) {
        transaction(database) { deviceExists(id) }
    }
}

internal fun deviceExists(id: DeviceId): Boolean =
    DevicesTable.selectAll().where { DevicesTable.id eq id.value }.any()
