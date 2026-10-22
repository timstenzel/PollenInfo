package ch.stenzel.tim.polleninfo.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode

/**
 * [Screen.Account]'s `mode` argument, written as the enum's name. Navigation finds an enum's
 * [NavType] by reflection on Android only; on iOS the route fails to register without this one
 * (passed through [Screen.Account.typeMap]).
 */
internal object AccountModeNavType : NavType<AccountMode>(isNullableAllowed = false) {
    override fun get(bundle: SavedState, key: String): AccountMode? =
        bundle.read { if (contains(key)) parseValue(getString(key)) else null }

    override fun put(bundle: SavedState, key: String, value: AccountMode) =
        bundle.write { putString(key, serializeAsValue(value)) }

    override fun parseValue(value: String): AccountMode = AccountMode.valueOf(value)

    override fun serializeAsValue(value: AccountMode): String = value.name
}
