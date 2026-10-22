package ch.stenzel.tim.polleninfo.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.Navigator
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AccountRouteTest {

    /** The smallest navigator a destination can be built for. */
    @Navigator.Name("test")
    private class TestNavigator : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination(this)
    }

    /** Registers the route as `composable<Screen.Account>` does — on iOS it throws without the map. */
    private fun accountDestination() =
        NavDestinationBuilder<NavDestination>(TestNavigator(), Screen.Account::class, Screen.Account.typeMap).build()

    @Test
    fun `the account route registers its mode argument with the account mode type`() {
        assertSame<Any>(AccountModeNavType, accountDestination().arguments.getValue("mode").type)
    }

    @Test
    fun `the account mode travels as its name and comes back unchanged`() {
        AccountMode.entries.forEach { mode ->
            val value = AccountModeNavType.serializeAsValue(mode)

            assertEquals(mode.name, value)
            assertEquals(mode, AccountModeNavType.parseValue(value))
        }
    }
}
