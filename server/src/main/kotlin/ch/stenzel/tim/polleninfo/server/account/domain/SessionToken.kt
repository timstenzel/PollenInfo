package ch.stenzel.tim.polleninfo.server.account.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * The secret a logged-in phone proves its login with: 256 random bits, base64url without padding,
 * so always 43 characters — the same shape as a device token. Issued by registration and login and
 * sent as a bearer token from then on.
 *
 * The server never stores it — only its [hash] — so neither the database nor a log line can be used
 * to act as the user.
 */
@JvmInline
value class SessionToken(val value: String) {

    /** Kept out of logs and exception messages by construction. */
    override fun toString(): String = "SessionToken(…)"

    companion object {

        /**
         * [value] as a token if it has the shape every issued token has, else `null` — a token that
         * cannot have been issued is refused without a database lookup.
         */
        fun parseOrNull(value: String): SessionToken? =
            if (value.length == SESSION_TOKEN_LENGTH && value.all(::isBase64Url)) SessionToken(value) else null
    }
}

/** A user's internal key. Never sent to the app; the [SessionToken] is what identifies it there. */
@JvmInline
value class UserId(val value: UUID)

/** A fresh session token from [SecureRandom]. */
fun newSessionToken(random: SecureRandom = secureRandom): SessionToken {
    val bytes = ByteArray(SESSION_TOKEN_BYTES).also(random::nextBytes)
    return SessionToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
}

/**
 * SHA-256 of the token's UTF-8 bytes — what `sessions.token_hash` holds. A fast hash is right for
 * 256 random bits, as for the device token; passwords get Argon2id instead ([PasswordHasher]).
 */
fun SessionToken.hash(): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

private fun isBase64Url(c: Char) = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_'

private const val SESSION_TOKEN_BYTES = 32

/** 32 bytes in base64url without padding. */
const val SESSION_TOKEN_LENGTH = 43

private val secureRandom = SecureRandom()
