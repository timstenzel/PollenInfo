package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import ch.stenzel.tim.polleninfo.feature.allstations.map.SwissOutline
import kotlin.reflect.KClass

/**
 * The tabs of the bottom navigation bar, in display order — the single place a tab is defined.
 *
 * The tabs are icon-only, so [contentDescription] is the only name a screen reader has to tell
 * them apart.
 */
enum class TopLevelDestination(
    val screen: Screen,
    val icon: ImageVector,
    val contentDescription: String,
) {
    HOME(Screen.Home, Icons.Default.LocationOn, "Home"),

    ALL_STATIONS(Screen.AllStations, SwissOutline, "All stations"),

    // Placeholder: shares the pin until its feature gets its own icon.
    FEATURE_3(Screen.Feature3, Icons.Default.LocationOn, "Feature 3"),

    ALARMS(Screen.Alarms, Icons.Default.Notifications, "Alarms"),

    // Placeholder, as above.
    FEATURE_5(Screen.Feature5, Icons.Default.LocationOn, "Feature 5"),
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
