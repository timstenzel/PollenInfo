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
    fun `there are five tabs with home and all stations first and the placeholders in order`() {
        assertEquals(
            listOf(
                TopLevelDestination.HOME,
                TopLevelDestination.ALL_STATIONS,
                TopLevelDestination.FEATURE_3,
                TopLevelDestination.FEATURE_4,
                TopLevelDestination.FEATURE_5,
            ),
            TopLevelDestination.entries,
        )
    }

    @Test
    fun `the tabs are announced as Home then All stations then Feature 3 to Feature 5`() {
        assertEquals(
            listOf("Home", "All stations", "Feature 3", "Feature 4", "Feature 5"),
            TopLevelDestination.entries.map { it.contentDescription },
        )
    }

    @Test
    fun `every tab has its own content description`() {
        val descriptions = TopLevelDestination.entries.map { it.contentDescription }

        assertEquals(descriptions.size, descriptions.toSet().size)
    }

    @Test
    fun `every tab has its own screen`() {
        val screens = TopLevelDestination.entries.map { it.screen }

        assertEquals(screens.size, screens.toSet().size)
    }

    @Test
    fun `the bar is shown on every tab with that tab selected`() {
        TopLevelDestination.entries.forEach { tab ->
            assertEquals(tab, currentTabOn(tab.screen))
        }
    }

    @Test
    fun `all stations is the second tab and is announced as All stations`() {
        val second = TopLevelDestination.entries[1]

        assertEquals(TopLevelDestination.ALL_STATIONS, second)
        assertEquals(Screen.AllStations, second.screen)
        assertEquals("All stations", second.contentDescription)
    }

    @Test
    fun `the bar is shown on all stations with that tab selected`() {
        assertEquals(TopLevelDestination.ALL_STATIONS, currentTabOn(Screen.AllStations))
    }

    @Test
    fun `the bar is shown on each placeholder screen`() {
        assertEquals(TopLevelDestination.FEATURE_3, currentTabOn(Screen.Feature3))
        assertEquals(TopLevelDestination.FEATURE_4, currentTabOn(Screen.Feature4))
        assertEquals(TopLevelDestination.FEATURE_5, currentTabOn(Screen.Feature5))
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
