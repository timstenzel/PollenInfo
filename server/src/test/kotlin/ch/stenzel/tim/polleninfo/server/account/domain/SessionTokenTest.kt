package ch.stenzel.tim.polleninfo.server.account.domain

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionTokenTest {

    @Test
    fun `a new token is 43 base64url characters`() {
        val token = newSessionToken()

        assertEquals(SESSION_TOKEN_LENGTH, token.value.length)
        assertTrue(token.value.all { it.isLetterOrDigit() || it == '-' || it == '_' }, token.value)
    }

    @Test
    fun `two new tokens differ`() {
        assertNotEquals(newSessionToken(), newSessionToken())
    }

    @Test
    fun `the hash is deterministic and 32 bytes long`() {
        val token = newSessionToken()

        assertEquals(32, token.hash().size)
        assertContentEquals(token.hash(), SessionToken(token.value).hash())
    }

    @Test
    fun `different tokens have different hashes`() {
        assertFalse(newSessionToken().hash().contentEquals(newSessionToken().hash()))
    }

    @Test
    fun `an issued token parses and anything of another shape does not`() {
        val token = newSessionToken()

        assertEquals(token, SessionToken.parseOrNull(token.value))
        assertNull(SessionToken.parseOrNull(token.value.dropLast(1)))
        assertNull(SessionToken.parseOrNull(token.value + "A"))
        assertNull(SessionToken.parseOrNull(token.value.dropLast(1) + "="))
        assertNull(SessionToken.parseOrNull(""))
    }

    @Test
    fun `a token does not print its value`() {
        val token = newSessionToken()

        assertFalse(token.value in token.toString())
    }
}
