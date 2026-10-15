package ch.stenzel.tim.polleninfo.server.account.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {

    /** Cheap parameters; the production defaults are pinned separately. */
    private val hasher = cheapHasher()

    @Test
    fun `a hash is a PHC string carrying its parameters and never the password`() {
        val hash = hasher.hash("correct horse")

        assertTrue(hash.startsWith("\$argon2id\$v=19\$m=64,t=1,p=1\$"), hash)
        assertEquals(6, hash.split('$').size, hash)
        assertFalse("correct horse" in hash, hash)
    }

    @Test
    fun `the production defaults are OWASP's baseline`() {
        assertEquals(19 * 1024, Argon2PasswordHasher.DEFAULT_MEMORY_KIB)
        assertEquals(2, Argon2PasswordHasher.DEFAULT_ITERATIONS)
        assertEquals(1, Argon2PasswordHasher.DEFAULT_PARALLELISM)
        assertTrue(Argon2PasswordHasher().hash("correct horse").startsWith("\$argon2id\$v=19\$m=19456,t=2,p=1\$"))
    }

    @Test
    fun `two hashes of one password differ because of the salt`() {
        assertNotEquals(hasher.hash("correct horse"), hasher.hash("correct horse"))
    }

    @Test
    fun `verify accepts the right password and refuses a wrong one`() {
        val hash = hasher.hash("correct horse")

        assertTrue(hasher.verify("correct horse", hash))
        assertFalse(hasher.verify("correct horsf", hash))
        assertFalse(hasher.verify("Correct horse", hash))
        assertFalse(hasher.verify("", hash))
    }

    @Test
    fun `verify reads the parameters from the string rather than its own`() {
        val hash = cheapHasher(memoryKiB = 128, iterations = 2).hash("correct horse")

        assertTrue(hasher.verify("correct horse", hash))
    }

    @Test
    fun `a malformed hash verifies nothing`() {
        val good = hasher.hash("correct horse").split('$')
        val malformed = listOf(
            "",
            "correct horse",
            "\$argon2i\$v=19\$${good[3]}\$${good[4]}\$${good[5]}",
            "\$argon2id\$v=16\$${good[3]}\$${good[4]}\$${good[5]}",
            "\$argon2id\$v=19\$m=64,t=1\$${good[4]}\$${good[5]}",
            "\$argon2id\$v=19\$m=0,t=1,p=1\$${good[4]}\$${good[5]}",
            "\$argon2id\$v=19\$m=x,t=1,p=1\$${good[4]}\$${good[5]}",
            "\$argon2id\$v=19\$${good[3]}\$not*base64\$${good[5]}",
            "\$argon2id\$v=19\$${good[3]}\$${good[4]}\$",
            "\$argon2id\$v=19\$${good[3]}\$${good[4]}",
        )

        malformed.forEach { assertFalse(hasher.verify("correct horse", it), it) }
    }

    @Test
    fun `verifyDummy does the work and does not throw`() {
        hasher.verifyDummy("anything")
    }
}

/** Argon2id at a cost a test suite can afford hundreds of times. */
fun cheapHasher(memoryKiB: Int = 64, iterations: Int = 1) =
    Argon2PasswordHasher(memoryKiB = memoryKiB, iterations = iterations, parallelism = 1)
