package ch.stenzel.tim.polleninfo.server.account.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/** Turns a password into what `users.password_hash` holds, and checks one against it. */
interface PasswordHasher {

    /** A new salted hash of [password]; two calls for one password give different results. */
    fun hash(password: String): String

    /** Whether [password] is the one [hash] was made from; `false` for a hash it cannot read. */
    fun verify(password: String, hash: String): Boolean

    /**
     * Does the work of one [verify] against a fixed hash and throws the answer away — for a login
     * with an email that has no account, so its timing does not tell that apart from a wrong password.
     */
    fun verifyDummy(password: String)
}

/**
 * Argon2id through Bouncy Castle, with a 16-byte salt from [SecureRandom]. The hash is a PHC string,
 * `$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>`, carrying its own parameters, so [verify] reads them
 * from the string and they can be raised later without invalidating stored hashes.
 *
 * The defaults are OWASP's baseline (19 MiB, two passes, one lane). Tests pass cheaper ones.
 */
class Argon2PasswordHasher(
    private val memoryKiB: Int = DEFAULT_MEMORY_KIB,
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val parallelism: Int = DEFAULT_PARALLELISM,
    private val random: SecureRandom = SecureRandom(),
) : PasswordHasher {

    private val dummyHash by lazy { hash("dummy password for unknown emails") }

    override fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(password, salt, memoryKiB, iterations, parallelism, HASH_BYTES)
        return "\$argon2id\$v=$VERSION\$m=$memoryKiB,t=$iterations,p=$parallelism\$${encode(salt)}\$${encode(hash)}"
    }

    override fun verify(password: String, hash: String): Boolean {
        val stored = parse(hash) ?: return false
        val candidate = derive(password, stored.salt, stored.memoryKiB, stored.iterations, stored.parallelism, stored.hash.size)
        return MessageDigest.isEqual(candidate, stored.hash)
    }

    override fun verifyDummy(password: String) {
        verify(password, dummyHash)
    }

    private class Stored(
        val memoryKiB: Int,
        val iterations: Int,
        val parallelism: Int,
        val salt: ByteArray,
        val hash: ByteArray,
    )

    /** `["", "argon2id", "v=19", "m=…,t=…,p=…", salt, hash]`, or `null` for anything else. */
    private fun parse(phc: String): Stored? {
        val parts = phc.split('$')
        if (parts.size != 6 || parts[0].isNotEmpty() || parts[1] != "argon2id" || parts[2] != "v=$VERSION") return null
        val parameters = parts[3].split(',').associate { entry ->
            val (key, value) = entry.split('=', limit = 2).takeIf { it.size == 2 } ?: return null
            key to (value.toIntOrNull()?.takeIf { it > 0 } ?: return null)
        }
        val salt = decode(parts[4]) ?: return null
        val hash = decode(parts[5]) ?: return null
        if (salt.isEmpty() || hash.isEmpty()) return null
        return Stored(
            memoryKiB = parameters["m"] ?: return null,
            iterations = parameters["t"] ?: return null,
            parallelism = parameters["p"] ?: return null,
            salt = salt,
            hash = hash,
        )
    }

    companion object {
        const val DEFAULT_MEMORY_KIB = 19 * 1024
        const val DEFAULT_ITERATIONS = 2
        const val DEFAULT_PARALLELISM = 1

        private const val VERSION = 19
        private const val SALT_BYTES = 16
        private const val HASH_BYTES = 32

        private fun derive(
            password: String,
            salt: ByteArray,
            memoryKiB: Int,
            iterations: Int,
            parallelism: Int,
            length: Int,
        ): ByteArray {
            val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memoryKiB)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build()
            val generator = Argon2BytesGenerator().apply { init(parameters) }
            return ByteArray(length).also { generator.generateBytes(password.toByteArray(Charsets.UTF_8), it) }
        }

        /** PHC strings use standard base64 without padding. */
        private fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)

        private fun decode(text: String): ByteArray? = try {
            Base64.getDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
