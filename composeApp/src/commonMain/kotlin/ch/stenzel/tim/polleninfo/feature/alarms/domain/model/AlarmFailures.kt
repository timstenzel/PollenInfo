package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

/**
 * This platform cannot receive push notifications, so there is nothing to register and no alarm
 * could ever arrive. Carried as the `Failure` of every alarm call; the screen says so instead of
 * offering a retry that cannot help.
 */
class PushUnavailableException : Exception("Push notifications aren't available on this device yet")
