package ch.stenzel.tim.polleninfo.core.appinfo

import platform.Foundation.NSBundle

/**
 * `CFBundleShortVersionString` and `CFBundleVersion` from the main bundle's `Info.plist`. A build
 * number that is not a whole number (iOS allows "1.0.3") counts as missing, so the line is hidden
 * rather than showing half a version. Compile-verified only: there is no iOS app wrapper yet.
 */
class IosAppInfo : AppInfo {

    override val version: AppVersion? by lazy {
        val bundle = NSBundle.mainBundle
        val name = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val code = (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toLongOrNull()
        if (name != null && code != null) AppVersion(name, code) else null
    }
}
