package ch.stenzel.tim.polleninfo.core.notifications

/**
 * Whether the app may post notifications and, if not, which route back to "yes" is left.
 *
 * The two non-enabled cases need different buttons: the system prompt can only be shown while the
 * system is still willing to show it, and after that the only way is the app's notification
 * settings.
 */
enum class NotificationPermissionState {
    /** Notifications reach the user. */
    ENABLED,

    /** Not enabled, and the system prompt can still be shown. */
    CAN_REQUEST,

    /**
     * Not enabled, and the system will not prompt again: permanently denied, or switched off in
     * system settings on an Android version without a runtime prompt.
     */
    MUST_OPEN_SETTINGS,
}
