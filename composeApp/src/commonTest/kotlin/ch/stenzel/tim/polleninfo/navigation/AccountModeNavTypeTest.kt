package ch.stenzel.tim.polleninfo.navigation

import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * `put` / `get` are not tested: on the JVM host `SavedState` is Android's stubbed `Bundle`, and both
 * only wrap [AccountModeNavType.parseValue] / the enum name pinned here.
 */
class AccountModeNavTypeTest {

    @Test
    fun `every mode survives the route string`() {
        AccountMode.entries.forEach { mode ->
            assertEquals(mode, AccountModeNavType.parseValue(AccountModeNavType.serializeAsValue(mode)))
        }
    }

    @Test
    fun `an unknown name is rejected`() {
        assertFailsWith<IllegalArgumentException> { AccountModeNavType.parseValue("SIGN_UP") }
    }

    @Test
    fun `the account type map covers the route argument type`() {
        assertSame(AccountModeNavType, accountTypeMap[typeOf<AccountMode>()])
    }
}
