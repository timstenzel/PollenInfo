package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.domain.hash
import ch.stenzel.tim.polleninfo.server.alarm.domain.newDeviceToken
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/** The installs that have registered for alarms. */
interface DeviceStore {

    /**
     * Stores a new device with its push token and returns the device token issued for it — the only
     * time it is ever available: the store keeps its hash alone.
     */
    suspend fun register(fcmToken: String): DeviceToken

    /**
     * The device [token] belongs to, or `null` for a token no device has. Records that the device
     * was seen, at most once per [LAST_SEEN_RESOLUTION], so an active install does not write on
     * every call.
     */
    suspend fun authenticate(token: DeviceToken): DeviceId?

    /** Replaces the device's push token, as when FCM rotates it; `false` for an unknown device. */
    suspend fun updateFcmToken(id: DeviceId, fcmToken: String): Boolean

    /**
     * Drops the device's push token if it is still [fcmToken], keeping the device and its alarms, so
     * the scheduler stops processing them until the app sends a new one. Conditional so that a token
     * the push service rejected can never wipe a newer one the app sent in the meantime.
     */
    suspend fun clearFcmToken(id: DeviceId, fcmToken: String)

    /** Deletes the device with its alarms and their notification log; `false` for an unknown device. */
    suspend fun delete(id: DeviceId): Boolean

    /**
     * Deletes, with their alarms and notification log, the devices that can no longer be reached —
     * no push token — and were last seen more than [INACTIVE_DEVICE_RETENTION] before [now]. A
     * device with a push token is never removed for inactivity. Returns how many were deleted.
     */
    suspend fun pruneInactive(now: Instant): Int
}

/** How often at most a device's `last_seen_at` is written: a day is all its later cleanup needs. */
val LAST_SEEN_RESOLUTION = 1.days

/** How long a device without a push token is kept after its last call. */
val INACTIVE_DEVICE_RETENTION = 90.days

class ExposedDeviceStore(
    private val database: Database,
    private val clock: Clock = Clock.systemUTC(),
) : DeviceStore {

    override suspend fun register(fcmToken: String): DeviceToken = withContext(Dispatchers.IO) {
        val token = newDeviceToken()
        val now = clock.instant().toTimestamp()
        transaction(database) {
            DevicesTable.insert {
                it[id] = UUID.randomUUID()
                it[tokenHash] = token.hash()
                it[DevicesTable.fcmToken] = fcmToken
                it[createdAt] = now
                it[lastSeenAt] = now
            }
        }
        token
    }

    override suspend fun authenticate(token: DeviceToken): DeviceId? = withContext(Dispatchers.IO) {
        val now = clock.instant()
        transaction(database) {
            val id = DevicesTable.select(DevicesTable.id)
                .where { DevicesTable.tokenHash eq token.hash() }
                .singleOrNull()
                ?.get(DevicesTable.id)
                ?: return@transaction null
            // Conditional, so a call within a day of the last refresh writes nothing.
            val due = now.minus(LAST_SEEN_RESOLUTION.toJavaDuration()).toTimestamp()
            DevicesTable.update({ (DevicesTable.id eq id) and (DevicesTable.lastSeenAt lessEq due) }) {
                it[lastSeenAt] = now.toTimestamp()
            }
            DeviceId(id)
        }
    }

    override suspend fun updateFcmToken(id: DeviceId, fcmToken: String): Boolean = withContext(Dispatchers.IO) {
        transaction(database) {
            DevicesTable.update({ DevicesTable.id eq id.value }) {
                it[DevicesTable.fcmToken] = fcmToken
            } > 0
        }
    }

    override suspend fun clearFcmToken(id: DeviceId, fcmToken: String) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                DevicesTable.update({ (DevicesTable.id eq id.value) and (DevicesTable.fcmToken eq fcmToken) }) {
                    it[DevicesTable.fcmToken] = null
                }
            }
        }
    }

    override suspend fun delete(id: DeviceId): Boolean = withContext(Dispatchers.IO) {
        // Alarms and their notification log go with the device through ON DELETE CASCADE.
        transaction(database) { DevicesTable.deleteWhere { DevicesTable.id eq id.value } > 0 }
    }

    override suspend fun pruneInactive(now: Instant): Int = withContext(Dispatchers.IO) {
        val cutoff = now.minus(INACTIVE_DEVICE_RETENTION.toJavaDuration()).toTimestamp()
        // Alarms and their notification log go with the devices through ON DELETE CASCADE.
        transaction(database) {
            DevicesTable.deleteWhere { DevicesTable.fcmToken.isNull() and (DevicesTable.lastSeenAt less cutoff) }
        }
    }
}
