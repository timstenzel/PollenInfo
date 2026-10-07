package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import ch.stenzel.tim.polleninfo.feature.allstations.map.SwissOutline
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.nav_alarms
import ch.stenzel.tim.polleninfo.resources.nav_all_stations
import ch.stenzel.tim.polleninfo.resources.nav_diary
import ch.stenzel.tim.polleninfo.resources.nav_home
import ch.stenzel.tim.polleninfo.resources.nav_settings
import org.jetbrains.compose.resources.StringResource
import kotlin.reflect.KClass

/**
 * The tabs of the bottom navigation bar, in display order — the single place a tab is defined.
 *
 * The tabs are icon-only, so [contentDescription] is the only name a screen reader has to tell
 * them apart. It is a string resource, resolved in the app's language where the bar is drawn.
 */
enum class TopLevelDestination(
    val screen: Screen,
    val icon: ImageVector,
    val contentDescription: StringResource,
) {
    HOME(Screen.Home, Icons.Default.LocationOn, Res.string.nav_home),

    ALL_STATIONS(Screen.AllStations, SwissOutline, Res.string.nav_all_stations),

    DIARY(Screen.Diary, Icons.AutoMirrored.Filled.MenuBook, Res.string.nav_diary),

    ALARMS(Screen.Alarms, Icons.Default.Notifications, Res.string.nav_alarms),

    SETTINGS(Screen.Settings, Icons.Default.Settings, Res.string.nav_settings),
    ;

    companion object {
        /**
         * The tab whose route [isOnRoute] matches, or `null` when the destination is not a tab —
         * which is exactly when the bottom bar is hidden.
         *
         * Takes a predicate rather than a [NavDestination][androidx.navigation.NavDestination] so
         * the rule is testable without a `NavController`; `AppNavigation` asks
         * `destination.hasRoute(it)`, the tests compare classes directly.
         */
        fun current(isOnRoute: (KClass<out Screen>) -> Boolean): TopLevelDestination? =
            entries.firstOrNull { isOnRoute(it.screen::class) }
    }
}
