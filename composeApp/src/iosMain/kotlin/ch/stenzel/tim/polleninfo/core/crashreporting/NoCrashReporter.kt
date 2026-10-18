package ch.stenzel.tim.polleninfo.core.crashreporting

/**
 * iOS reports no crashes yet (that needs the native Firebase SDK in the not-yet-existing wrapper),
 * so Settings shows no switch. Compile-verified only.
 */
class NoCrashReporter : CrashReporter {
    override val isAvailable: Boolean = false

    override fun setEnabled(enabled: Boolean) = Unit
}
