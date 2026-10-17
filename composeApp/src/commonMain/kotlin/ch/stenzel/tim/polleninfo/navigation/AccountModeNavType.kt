package ch.stenzel.tim.polleninfo.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode
import kotlin.reflect.typeOf

/**
 * [AccountMode] as a route argument of [Screen.Account], by its enum name.
 *
 * On Android, Navigation derives a `NavType` for any enum by reflection; on iOS it cannot, and
 * registering the route without this type map throws while the graph is built — on every launch,
 * because the graph is built up front.
 */
internal object AccountModeNavType : NavType<AccountMode>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: AccountMode) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(bundle: SavedState, key: String): AccountMode? =
        bundle.read { if (contains(key)) parseValue(getString(key)) else null }

    override fun parseValue(value: String): AccountMode = AccountMode.valueOf(value)

    override fun serializeAsValue(value: AccountMode): String = value.name
}

/** The type map to register [Screen.Account] with. */
internal val accountTypeMap = mapOf(typeOf<AccountMode>() to AccountModeNavType)
