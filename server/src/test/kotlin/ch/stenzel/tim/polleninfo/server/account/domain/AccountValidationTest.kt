package ch.stenzel.tim.polleninfo.server.account.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountValidationTest {

    private val password = "correct horse"

    private fun valid(email: String = "tim@example.com", password: String = this.password, consent: Boolean = true) =
        assertIs<AccountValidationResult.Valid>(AccountValidation.validateRegistration(email, password, consent)).spec

    private fun invalid(email: String = "tim@example.com", password: String = this.password, consent: Boolean = true) =
        assertIs<AccountValidationResult.Invalid>(AccountValidation.validateRegistration(email, password, consent)).message

    /** An address of exactly [length] characters. */
    private fun emailOfLength(length: Int): String {
        val domain = "@example.com"
        return "a".repeat(length - domain.length) + domain
    }

    // --- Email ---

    @Test
    fun `capitals and surrounding spaces are normalised away`() {
        assertEquals("tim@gmx.ch", valid(email = "  Tim@GMX.ch \t").email)
    }

    @Test
    fun `an email of 254 characters is accepted and one of 255 is not`() {
        assertEquals(254, valid(email = emailOfLength(254)).email.length)
        assertTrue("email" in invalid(email = emailOfLength(255)))
    }

    @Test
    fun `the length limit applies after trimming`() {
        assertEquals(254, valid(email = "  " + emailOfLength(254) + "  ").email.length)
    }

    @Test
    fun `an email needs exactly one at sign with text on each side`() {
        listOf("", "   ", "tim", "@example.com", "tim@", "@", "tim@@example.com", "tim@a@example.com").forEach {
            assertNull(AccountValidation.normalizeEmail(it), it)
            assertTrue("email" in invalid(email = it), it)
        }
        assertEquals("t@e", AccountValidation.normalizeEmail("t@e"))
    }

    // --- Password ---

    @Test
    fun `a password of 7 characters is refused and one of 8 is accepted`() {
        assertTrue("password" in invalid(password = "a".repeat(7)))
        assertEquals("a".repeat(8), valid(password = "a".repeat(8)).password)
    }

    @Test
    fun `a password of 128 characters is accepted and one of 129 is not`() {
        assertEquals(128, valid(password = "a".repeat(128)).password.length)
        assertTrue("password" in invalid(password = "a".repeat(129)))
    }

    @Test
    fun `password length counts code points so an emoji counts once`() {
        // Seven letters and one emoji: eight code points, nine UTF-16 chars.
        valid(password = "abcdefg🌼")
        // 128 emoji: 128 code points, 256 UTF-16 chars.
        valid(password = "🌼".repeat(128))
        assertTrue("password" in invalid(password = "🌼".repeat(129)))
    }

    @Test
    fun `a password is never trimmed and has no character rules`() {
        // Eight characters only with the spaces, which count.
        assertEquals("  pass  ", valid(password = "  pass  ").password)
        assertEquals("12345678", valid(password = "12345678").password)
    }

    // --- Consent ---

    @Test
    fun `a registration without consent is refused`() {
        assertTrue("consent" in invalid(consent = false))
    }

    @Test
    fun `no message repeats the email or the password`() {
        val messages = listOf(
            invalid(email = "not-an-address"),
            invalid(password = "short"),
            invalid(consent = false),
        )
        messages.forEach { message ->
            assertFalse("not-an-address" in message || "short" in message || this.password in message, message)
        }
    }

    @Test
    fun `a spec does not print its email or password`() {
        val printed = valid().toString()

        assertFalse("tim@example.com" in printed || password in printed, printed)
    }
}
