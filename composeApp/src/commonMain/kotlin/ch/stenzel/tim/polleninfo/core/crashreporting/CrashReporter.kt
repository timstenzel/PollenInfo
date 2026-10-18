package ch.stenzel.tim.polleninfo.core.crashreporting

/**
 * The platform's crash reporting, reduced to the one thing the app decides: whether it may send.
 *
 * Only [CrashReportingSync] calls [setEnabled] — it passes on every value of the stored choice, which
 * is the single source of truth. Settings reads [isAvailable] alone, to hide the switch on a platform
 * that reports nothing (iOS for now).
 */
interface CrashReporter {

    /** Whether this platform reports crashes at all; fixed for the process. */
    val isAvailable: Boolean

    /** Applies the user's choice immediately; the platform remembers it for the next launch. */
    fun setEnabled(enabled: Boolean)
}
