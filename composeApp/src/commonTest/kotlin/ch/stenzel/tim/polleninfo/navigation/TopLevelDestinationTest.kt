package ch.stenzel.tim.polleninfo.navigation

import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.nav_alarms
import ch.stenzel.tim.polleninfo.resources.nav_all_stations
import ch.stenzel.tim.polleninfo.resources.nav_diary
import ch.stenzel.tim.polleninfo.resources.nav_home
import ch.stenzel.tim.polleninfo.resources.nav_settings
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
        assertEquals(Res.string.nav_home, first.contentDescription)
    }

    @Test
    fun `the bar is shown on home with the home tab selected`() {
        assertEquals(TopLevelDestination.HOME, currentTabOn(Screen.Home))
    }

    @Test
    fun `there are five tabs with the diary third and settings last`() {
        assertEquals(
            listOf(
                TopLevelDestination.HOME,
                TopLevelDestination.ALL_STATIONS,
                TopLevelDestination.DIARY,
                TopLevelDestination.ALARMS,
                TopLevelDestination.SETTINGS,
            ),
            TopLevelDestination.entries,
        )
    }

    @Test
    fun `the tabs are announced as Home then All stations then Diary then Alarms then Settings`() {
        assertEquals(
            listOf(
                Res.string.nav_home,
                Res.string.nav_all_stations,
                Res.string.nav_diary,
                Res.string.nav_alarms,
                Res.string.nav_settings,
            ),
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
        assertEquals(Res.string.nav_all_stations, second.contentDescription)
    }

    @Test
    fun `the bar is shown on all stations with that tab selected`() {
        assertEquals(TopLevelDestination.ALL_STATIONS, currentTabOn(Screen.AllStations))
    }

    @Test
    fun `alarms is the fourth tab and is announced as Alarms`() {
        val fourth = TopLevelDestination.entries[3]

        assertEquals(TopLevelDestination.ALARMS, fourth)
        assertEquals(Screen.Alarms, fourth.screen)
        assertEquals(Res.string.nav_alarms, fourth.contentDescription)
    }

    @Test
    fun `the bar is shown on alarms with that tab selected`() {
        assertEquals(TopLevelDestination.ALARMS, currentTabOn(Screen.Alarms))
    }

    @Test
    fun `the diary is the third tab and is announced as Diary`() {
        val third = TopLevelDestination.entries[2]

        assertEquals(TopLevelDestination.DIARY, third)
        assertEquals(Screen.Diary, third.screen)
        assertEquals(Res.string.nav_diary, third.contentDescription)
    }

    @Test
    fun `the bar is shown on the diary with that tab selected`() {
        assertEquals(TopLevelDestination.DIARY, currentTabOn(Screen.Diary))
    }

    @Test
    fun `settings is the fifth tab and is announced as Settings`() {
        val fifth = TopLevelDestination.entries[4]

        assertEquals(TopLevelDestination.SETTINGS, fifth)
        assertEquals(Screen.Settings, fifth.screen)
        assertEquals(Res.string.nav_settings, fifth.contentDescription)
    }

    @Test
    fun `the bar is shown on settings with that tab selected`() {
        assertEquals(TopLevelDestination.SETTINGS, currentTabOn(Screen.Settings))
    }

    @Test
    fun `the bar is hidden during onboarding`() {
        assertNull(currentTabOn(Screen.Onboarding))
    }

    @Test
    fun `the alarm editor is not a tab so the bar is hidden on it`() {
        assertNull(currentTabOn(Screen.AlarmEditor()))
        assertNull(currentTabOn(Screen.AlarmEditor(alarmId = "alarm-1")))
    }

    @Test
    fun `the change-station screen is not a tab so the bar is hidden on it`() {
        assertNull(currentTabOn(Screen.ChangeStation))
    }

    @Test
    fun `the bar is hidden on the example screen`() {
        assertNull(currentTabOn(Screen.Example))
    }
}
