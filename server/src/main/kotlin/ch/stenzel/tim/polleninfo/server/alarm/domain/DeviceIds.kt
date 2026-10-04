package ch.stenzel.tim.polleninfo.server.alarm.domain

import java.security.SecureRandom
import java.util.Base64

/**
 * A fresh device id: 128 random bits, base64url without padding, so always 22 characters.
 *
 * The id is the only thing standing between one install's alarms and everybody else, so it comes
 * from [SecureRandom] and is long enough not to be guessed.
 */
fun newDeviceId(random: SecureRandom = secureRandom): DeviceId {
    val bytes = ByteArray(DEVICE_ID_BYTES).also(random::nextBytes)
    return DeviceId(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
}

private const val DEVICE_ID_BYTES = 16

private val secureRandom = SecureRandom()
