package ch.stenzel.tim.polleninfo.feature.account.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountFormStateTest {

    /** A registration the backend would accept; each test breaks one thing. */
    private val validRegistration = AccountFormState(
        mode = AccountMode.REGISTER,
        email = "tim@example.com",
        password = "correct horse",
        confirmation = "correct horse",
        consent = true,
    )

    private fun AccountFormState.withPasswords(password: String) = withPassword(password).withConfirmation(password)

    // --- Registration: canSubmit ---

    @Test
    fun `a complete registration can be submitted`() {
        assertTrue(validRegistration.canSubmit)
    }

    @Test
    fun `an empty registration cannot be submitted`() {
        assertFalse(AccountFormState(mode = AccountMode.REGISTER).canSubmit)
    }

    @Test
    fun `a password of 7 characters cannot be submitted and one of 8 can`() {
        val seven = validRegistration.withPasswords("1234567")
        val eight = validRegistration.withPasswords("12345678")

        assertTrue(seven.passwordTooShort)
        assertFalse(seven.canSubmit)
        assertFalse(eight.passwordTooShort)
        assertTrue(eight.canSubmit)
    }

    @Test
    fun `a password of 128 characters can be submitted and one of 129 cannot`() {
        val max = validRegistration.withPasswords("a".repeat(128))
        val over = validRegistration.withPasswords("a".repeat(129))

        assertFalse(max.passwordTooLong)
        assertTrue(max.canSubmit)
        assertTrue(over.passwordTooLong)
        assertFalse(over.canSubmit)
    }

    @Test
    fun `the length counts an emoji once as the backend does`() {
        val flower = "🌼"
        val eight = validRegistration.withPasswords("abcdefg$flower")
        val seven = validRegistration.withPasswords("abcdef$flower")

        assertEquals(8, eight.passwordLength)
        assertTrue(eight.canSubmit)
        assertEquals(7, seven.passwordLength)
        assertFalse(seven.canSubmit)
        assertTrue(validRegistration.withPasswords(flower.repeat(128)).canSubmit)
        assertFalse(validRegistration.withPasswords(flower.repeat(129)).canSubmit)
    }

    @Test
    fun `a different confirmation cannot be submitted`() {
        val form = validRegistration.withConfirmation("correct horsE")

        assertTrue(form.passwordsMismatch)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `the mismatch shows only once the confirmation has text`() {
        val empty = validRegistration.withConfirmation("")

        assertFalse(empty.passwordsMismatch)
        assertFalse(empty.canSubmit)
        assertTrue(empty.withConfirmation("c").passwordsMismatch)
        assertFalse(validRegistration.passwordsMismatch)
    }

    @Test
    fun `a registration without consent cannot be submitted`() {
        assertFalse(validRegistration.withConsent(false).canSubmit)
        assertTrue(validRegistration.withConsent(false).withConsent(true).canSubmit)
    }

    @Test
    fun `an implausible email cannot be submitted`() {
        listOf("", "  ", "tim", "@example.com", "tim@", "tim@@example.com", "a@b@c").forEach { email ->
            val form = validRegistration.withEmail(email)
            assertFalse(form.isEmailPlausible, email)
            assertFalse(form.canSubmit, email)
        }
    }

    @Test
    fun `an email is checked as the backend checks it`() {
        val domain = "@example.com"
        assertTrue(validRegistration.withEmail(" Tim@Example.com ").canSubmit)
        assertTrue(validRegistration.withEmail("a".repeat(254 - domain.length) + domain).canSubmit)
        assertFalse(validRegistration.withEmail("a".repeat(255 - domain.length) + domain).canSubmit)
        // Surrounding spaces do not count towards the limit.
        assertTrue(validRegistration.withEmail("  " + "a".repeat(254 - domain.length) + domain + "  ").canSubmit)
    }

    // --- Login: canSubmit ---

    @Test
    fun `logging in needs a plausible email and any password`() {
        val login = AccountFormState(mode = AccountMode.LOGIN, email = "tim@example.com")

        assertFalse(login.canSubmit)
        assertTrue(login.withPassword("x").canSubmit)
        assertFalse(login.withPassword("x").withEmail("tim").canSubmit)
    }

    // --- Visibility ---

    @Test
    fun `both passwords start hidden`() {
        assertFalse(validRegistration.passwordVisible)
        assertFalse(validRegistration.confirmationVisible)
    }

    @Test
    fun `the two visibility toggles are independent`() {
        val passwordShown = validRegistration.togglePasswordVisible()

        assertTrue(passwordShown.passwordVisible)
        assertFalse(passwordShown.confirmationVisible)

        val both = passwordShown.toggleConfirmationVisible()
        assertTrue(both.passwordVisible)
        assertTrue(both.confirmationVisible)

        val confirmationOnly = both.togglePasswordVisible()
        assertFalse(confirmationOnly.passwordVisible)
        assertTrue(confirmationOnly.confirmationVisible)
    }

    // --- Modes ---

    @Test
    fun `switching mode keeps the email and starts the rest over`() {
        val login = validRegistration.togglePasswordVisible().withMode(AccountMode.LOGIN)

        assertEquals(AccountFormState(mode = AccountMode.LOGIN, email = "tim@example.com"), login)
        assertEquals("tim@example.com", login.withMode(AccountMode.REGISTER).email)
    }

    @Test
    fun `switching to the current mode changes nothing`() {
        assertEquals(validRegistration, validRegistration.withMode(AccountMode.REGISTER))
    }

    @Test
    fun `a printed form shows neither the email nor a password`() {
        val printed = validRegistration.toString()

        assertFalse("tim@example.com" in printed || "correct horse" in printed, printed)
    }
}
