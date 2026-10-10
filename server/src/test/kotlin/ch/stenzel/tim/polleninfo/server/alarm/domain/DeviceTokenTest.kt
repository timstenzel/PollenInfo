package ch.stenzel.tim.polleninfo.server.alarm.domain

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceTokenTest {

    @Test
    fun `a new token is 43 base64url characters`() {
        val token = newDeviceToken()

        assertEquals(DEVICE_TOKEN_LENGTH, token.value.length)
        assertTrue(token.value.all { it.isLetterOrDigit() || it == '-' || it == '_' }, token.value)
    }

    @Test
    fun `two new tokens differ`() {
        assertNotEquals(newDeviceToken(), newDeviceToken())
    }

    @Test
    fun `the hash is deterministic and 32 bytes long`() {
        val token = newDeviceToken()

        assertEquals(32, token.hash().size)
        assertContentEquals(token.hash(), DeviceToken(token.value).hash())
    }

    @Test
    fun `different tokens have different hashes`() {
        assertFalse(newDeviceToken().hash().contentEquals(newDeviceToken().hash()))
    }

    @Test
    fun `an issued token parses and anything of another shape does not`() {
        val token = newDeviceToken()

        assertEquals(token, DeviceToken.parseOrNull(token.value))
        assertNull(DeviceToken.parseOrNull(token.value.dropLast(1)))
        assertNull(DeviceToken.parseOrNull(token.value + "A"))
        assertNull(DeviceToken.parseOrNull(token.value.dropLast(1) + "="))
        assertNull(DeviceToken.parseOrNull(""))
    }

    @Test
    fun `a token does not print its value`() {
        val token = newDeviceToken()

        assertFalse(token.value in token.toString())
    }
}
