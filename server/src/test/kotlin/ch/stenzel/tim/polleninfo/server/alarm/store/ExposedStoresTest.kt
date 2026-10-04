package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.TransactionManager
import kotlin.test.assertFailsWith

class ExposedStoresTest {

    private val database = PollenInfoDatabase.inMemory()
    private val devices = ExposedDeviceStore(database)
    private val alarms = ExposedAlarmStore(database)

    private val tempDir = Files.createTempDirectory("polleninfo-db-test")

    @OptIn(ExperimentalPathApi::class)
    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `a registered device exists`() = runTest {
        val id = devices.register("token-1")

        assertTrue(devices.exists(id))
    }

    @Test
    fun `a device that never registered does not exist`() = runTest {
        assertFalse(devices.exists(DeviceId("never-registered")))
    }

    @Test
    fun `every registration issues a new 22 character id`() = runTest {
        val first = devices.register("token-1")
        val second = devices.register("token-1")

        assertNotEquals(first, second)
        assertEquals(22, first.value.length)
        assertTrue(first.value.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun `an unknown device has no alarm list rather than an empty one`() = runTest {
        assertNull(alarms.list(DeviceId("never-registered")))
    }

    @Test
    fun `a registered device without alarms has an empty list`() = runTest {
        val id = devices.register("token-1")

        assertEquals(emptyList(), alarms.list(id))
    }

    @Test
    fun `alarms are listed in creation order`() = runTest {
        val id = devices.register("token-1")
        // Ids chosen so that id order is the reverse of creation order.
        val first = dailyAlarm(id, AlarmId("c"))
        val second = thresholdAlarm(id, AlarmId("b"))
        val third = dailyAlarm(id, AlarmId("a"))
        database.insertAlarm(second, createdAtMillis = 2_000)
        database.insertAlarm(third, createdAtMillis = 3_000)
        database.insertAlarm(first, createdAtMillis = 1_000)

        assertEquals(listOf(first, second, third), alarms.list(id))
    }

    @Test
    fun `both schedule types read back exactly as stored`() = runTest {
        val id = devices.register("token-1")
        val daily = dailyAlarm(id)
        val threshold = thresholdAlarm(id)
        database.insertAlarm(daily, createdAtMillis = 1)
        database.insertAlarm(threshold, createdAtMillis = 2)

        assertEquals(listOf(daily, threshold), alarms.list(id))
    }

    @Test
    fun `a device sees only its own alarms`() = runTest {
        val mine = devices.register("token-1")
        val theirs = devices.register("token-2")
        val myAlarm = dailyAlarm(mine)
        database.insertAlarm(myAlarm, createdAtMillis = 1)
        database.insertAlarm(dailyAlarm(theirs), createdAtMillis = 2)

        assertEquals(listOf(myAlarm), alarms.list(mine))
    }

    @Test
    fun `an alarm for an unregistered device is refused because foreign keys are enforced`() {
        assertFailsWith<ExposedSQLException> {
            database.insertAlarm(dailyAlarm(DeviceId("never-registered")), createdAtMillis = 1)
        }
    }

    @Test
    fun `data survives closing and reopening a file backed database`() = runTest {
        val path = tempDir.resolve("nested/dir/polleninfo.db")
        val first = PollenInfoDatabase.file(path)
        val id = ExposedDeviceStore(first).register("token-1")
        val alarm = dailyAlarm(id)
        first.insertAlarm(alarm, createdAtMillis = 1)
        TransactionManager.closeAndUnregister(first)

        val reopened = PollenInfoDatabase.file(path)

        assertTrue(path.exists())
        assertTrue(ExposedDeviceStore(reopened).exists(id))
        assertEquals(listOf(alarm), ExposedAlarmStore(reopened).list(id))
    }
}
