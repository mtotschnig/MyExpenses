package org.totschnig.myexpenses.next

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.totschnig.myexpenses.dialog.MenuItem

class NextLayoutTest {

    @Test
    fun adaptiveModeUsesRailInLandscapeAndBarInPortrait() {
        val calculated = NavigationSuiteType.ShortNavigationBarCompact
        assertEquals(
            NavigationSuiteType.WideNavigationRailCollapsed,
            navigationSuiteType(MenuItem.NavigationMode.ADAPTIVE, calculated, isNavigationVisible = true, isLandscape = true)
        )
        assertEquals(
            NavigationSuiteType.ShortNavigationBarMedium,
            navigationSuiteType(MenuItem.NavigationMode.ADAPTIVE, calculated, isNavigationVisible = true, isLandscape = false)
        )
        assertEquals(
            NavigationSuiteType.None,
            navigationSuiteType(MenuItem.NavigationMode.TOGGLEABLE_RAIL, calculated, isNavigationVisible = false, isLandscape = false)
        )
    }

    @Test
    fun showsMoreActionsOnWiderWindowsAndFewerAtLargeFonts() {
        assertEquals(6, visibleActionItems(isExpanded = true, isMedium = true, is2Pane = false, fontScale = 1f))
        assertEquals(4, visibleActionItems(isExpanded = true, isMedium = true, is2Pane = true, fontScale = 1f))
        assertEquals(2, visibleActionItems(isExpanded = false, isMedium = false, is2Pane = false, fontScale = 1f))
        assertEquals(1, visibleActionItems(isExpanded = false, isMedium = false, is2Pane = false, fontScale = 1.2f))
        assertEquals(0, visibleActionItems(isExpanded = false, isMedium = false, is2Pane = false, fontScale = 2f))
    }
}
