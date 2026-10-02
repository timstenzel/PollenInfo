package ch.stenzel.tim.polleninfo.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TopLevelDestinationTest {

    /** What the bar shows for [screen] — the same rule `AppNavigation` applies to its back stack. */
    private fun currentTabOn(screen: Screen) = TopLevelDestination.current { it == screen::class }

    @Test
    fun `home is the first tab and is announced as Home`() {
        val first = TopLevelDestination.entries.first()

        assertEquals(TopLevelDestination.HOME, first)
        assertEquals(Screen.Home, first.screen)
        assertEquals("Home", first.contentDescription)
    }

    @Test
    fun `the bar is shown on home with the home tab selected`() {
        assertEquals(TopLevelDestination.HOME, currentTabOn(Screen.Home))
    }

    @Test
    fun `the bar is hidden during onboarding`() {
        assertNull(currentTabOn(Screen.Onboarding))
    }

    @Test
    fun `the bar is hidden on the example screen`() {
        assertNull(currentTabOn(Screen.Example))
    }
}
