package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

/**
 * This platform cannot receive push notifications, so there is nothing to register and no alarm
 * could ever arrive. Carried as the `Failure` of every alarm call; the screen says so instead of
 * offering a retry that cannot help.
 */
class PushUnavailableException : Exception("Push notifications aren't available on this device yet")

/**
 * The backend refused an alarm as invalid (`400`). The editor only offers valid alarms, so this means
 * the two sides disagree on a rule; [message] is the backend's explanation.
 */
class InvalidAlarmException(message: String) : Exception(message)
