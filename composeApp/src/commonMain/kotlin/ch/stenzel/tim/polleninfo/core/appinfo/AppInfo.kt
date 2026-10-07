package ch.stenzel.tim.polleninfo.core.appinfo

/** The installed build as the platform reports it, e.g. `1.0.0-debug` and `1`. */
data class AppVersion(val name: String, val code: Long)

/**
 * Facts about the installed app that only the platform knows. Bound in `platformModule`:
 * `AndroidAppInfo` reads the `PackageManager`, `IosAppInfo` the main bundle's `Info.plist`.
 */
interface AppInfo {
    /** `null` when the platform does not report a complete version; the screen then hides the line. */
    val version: AppVersion?
}
