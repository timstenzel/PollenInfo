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

/**
 * This device has no alarm with the requested id — it was deleted, from this install or before the
 * list was last loaded.
 */
class AlarmNotFoundException : Exception("This alarm no longer exists")

/**
 * The backend does not know the stored device token (`401`) — its database was reset, or the install
 * was erased or cleaned up. The repository re-registers once on it; it only reaches a caller if the
 * fresh token is refused too.
 */
class UnknownDeviceException : Exception("The backend does not know this device")

/** How many alarms one device may hold; the backend refuses the next with `409`. */
const val MAX_ALARMS = 10

/**
 * The device already holds [MAX_ALARMS] alarms (`409`). The list disables "Create alarm" at the
 * limit, so this means it was reached anyway — from another install sharing the token, or a race.
 */
class AlarmLimitReachedException :
    Exception("You can have at most $MAX_ALARMS alarms. Delete one to create another.")
