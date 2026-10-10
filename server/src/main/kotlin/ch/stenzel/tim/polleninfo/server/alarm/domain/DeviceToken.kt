package ch.stenzel.tim.polleninfo.server.alarm.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * The secret an install proves itself with: 256 random bits, base64url without padding, so always
 * 43 characters. Issued once by `POST /devices` and sent as a bearer token from then on.
 *
 * The server never stores it — only its [hash] — so neither the database nor a log line can be used
 * to act as the install.
 */
@JvmInline
value class DeviceToken(val value: String) {

    /** Kept out of logs and exception messages by construction. */
    override fun toString(): String = "DeviceToken(…)"

    companion object {

        /**
         * [value] as a token if it has the shape every issued token has, else `null` — a token that
         * cannot have been issued is refused without a database lookup.
         */
        fun parseOrNull(value: String): DeviceToken? =
            if (value.length == DEVICE_TOKEN_LENGTH && value.all(::isBase64Url)) DeviceToken(value) else null
    }
}

/** An install's internal key. Never sent to the app; the [DeviceToken] is what identifies it there. */
@JvmInline
value class DeviceId(val value: UUID)

/** A fresh device token from [SecureRandom]. */
fun newDeviceToken(random: SecureRandom = secureRandom): DeviceToken {
    val bytes = ByteArray(DEVICE_TOKEN_BYTES).also(random::nextBytes)
    return DeviceToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
}

/**
 * SHA-256 of the token's UTF-8 bytes — what `devices.token_hash` holds. A fast hash is deliberate:
 * the token is 256 bits of random data, not a password, so there is nothing to slow down guessing
 * of. Lookup is by this hash through a unique index, so no comparison happens in Kotlin.
 */
fun DeviceToken.hash(): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

private fun isBase64Url(c: Char) = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_'

private const val DEVICE_TOKEN_BYTES = 32

/** 32 bytes in base64url without padding. */
const val DEVICE_TOKEN_LENGTH = 43

private val secureRandom = SecureRandom()
