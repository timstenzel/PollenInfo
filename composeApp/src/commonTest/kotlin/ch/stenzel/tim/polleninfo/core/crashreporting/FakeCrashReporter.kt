package ch.stenzel.tim.polleninfo.core.crashreporting

/** Records every value it is given. */
class FakeCrashReporter(override val isAvailable: Boolean = true) : CrashReporter {

    /** Every value passed to [setEnabled], in order. */
    val applied = mutableListOf<Boolean>()

    override fun setEnabled(enabled: Boolean) {
        applied += enabled
    }
}
