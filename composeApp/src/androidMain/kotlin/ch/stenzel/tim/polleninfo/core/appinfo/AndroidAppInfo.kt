package ch.stenzel.tim.polleninfo.core.appinfo

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

/**
 * The `versionName` and `versionCode` Gradle declares — so a debug build shows its
 * `versionNameSuffix` ("1.0.0-debug") on its own. Checked by hand.
 */
class AndroidAppInfo(private val context: Context) : AppInfo {

    override val version: AppVersion? by lazy {
        val info = packageInfo() ?: return@lazy null
        val name = info.versionName ?: return@lazy null
        AppVersion(name = name, code = PackageInfoCompat.getLongVersionCode(info))
    }

    private fun packageInfo(): PackageInfo? = try {
        val packageManager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        // Cannot happen for the app's own package; treated as "no version" rather than a crash.
        null
    }
}
