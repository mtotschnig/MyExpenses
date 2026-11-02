package org.totschnig.myexpenses.next

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.NextPreviewTheme

/**
 * A tab of the bar at the bottom or the rail at the start, which shows the page of [key]. The
 * overview and the menu are always there, the areas bring theirs, see [NextFeature.tab]. Icons are
 * placeholders.
 */
class NextTab(val icon: ImageVector, @param:StringRes val labelRes: Int, val key: NextKey?) {
    /** Whether [page] belongs to this tab, e.g. a contract opened from a saving tip to the contracts */
    fun selects(page: NavKey) = key != null && key::class == page::class

    companion object {
        val Overview = NextTab(Icons.Default.DashboardCustomize, R.string.next_tab_overview, NextKey.Overview)

        /** Opens the menu, has no page */
        val Menu = NextTab(Icons.Default.Menu, R.string.next_tab_menu, null)
    }
}

@Composable
fun NextBottomBar(
    /** In the order they are shown */
    tabs: List<NextTab>,
    selectedTab: NextTab,
    onTabClick: (NextTab) -> Unit,
    modifier: Modifier = Modifier,
    /** Tabs with something new, e.g. unread news about contracts, get a dot */
    badges: Set<NextTab> = emptySet(),
) {
    // The Material bar marks the selected tab with an indicator behind its icon, not only by color
    ShortNavigationBar(modifier = modifier) {
        tabs.forEach { tab ->
            ShortNavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabClick(tab) },
                icon = {
                    BadgedBox(badge = { if (tab in badges) Badge() }) {
                        Icon(tab.icon, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

/**
 * The tabs of [NextBottomBar] in a rail at the start, for wide windows, e.g. tablets and phones in
 * landscape
 */
@Composable
fun NextNavigationRail(
    /** In the order they are shown */
    tabs: List<NextTab>,
    selectedTab: NextTab,
    onTabClick: (NextTab) -> Unit,
    modifier: Modifier = Modifier,
    /** Tabs with something new, e.g. unread news about contracts, get a dot */
    badges: Set<NextTab> = emptySet(),
) {
    NavigationRail(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.weight(1f))
        tabs.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabClick(tab) },
                icon = {
                    BadgedBox(badge = { if (tab in badges) Badge() }) {
                        Icon(tab.icon, contentDescription = null)
                    }
                },
                label = { Text(stringResource(tab.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/** The tabs that are always there; those of the areas look the same */
private val previewTabs = listOf(NextTab.Overview, NextTab.Menu)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NextBottomBarPreview() {
    NextPreviewTheme {
        // Stateful, so that tapping a tab in interactive mode selects it
        var selectedTab by remember { mutableStateOf(NextTab.Overview) }
        NextBottomBar(previewTabs, selectedTab = selectedTab, onTabClick = { selectedTab = it })
    }
}

@Preview(name = "Rail", showBackground = true, heightDp = 480)
@Preview(name = "Rail, dark", showBackground = true, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NextNavigationRailPreview() {
    NextPreviewTheme {
        var selectedTab by remember { mutableStateOf(NextTab.Overview) }
        NextNavigationRail(previewTabs, selectedTab = selectedTab, onTabClick = { selectedTab = it }, badges = setOf(NextTab.Menu))
    }
}
