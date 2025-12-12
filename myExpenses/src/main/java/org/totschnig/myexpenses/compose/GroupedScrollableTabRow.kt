package org.totschnig.myexpenses.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.model.AccountGrouping

/**
 * A generic, scrollable row of tabs that can display group headers.
 *
 * @param T The type of the data item for each selectable tab.
 * @param items The complete list of items to display in the tab row.
 * @param selectedItem The currently selected item of type [T]. This is used to highlight the correct tab.
 * @param onTabSelected A callback invoked when a selectable tab is clicked.
 * @param isHeader A lambda that determines if an item from the list is a header.
 * @param getHeaderTitle A lambda that provides the title string for a header item.
 * @param getTabTitle A lambda that provides the title string for a regular tab item.
 * @param modifier Optional [Modifier] for the [ScrollableTabRow].
 * @param edgePadding The padding on the sides of the tab row.
 * @param headerStyle The [TextStyle] for header items.
 * @param headerColor The color for header items.
 */
@Composable
fun <T> GroupedScrollableTabRow(
    items: List<Any>,
    selectedItem: T,
    onTabSelected: (T) -> Unit,
    isHeader: (Any) -> Boolean,
    getHeaderTitle: (Any) -> String,
    getTabTitle: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 8.dp,
    headerStyle: TextStyle = MaterialTheme.typography.labelSmall,
    headerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
) {
    // Find the index of the currently selected item in the full list.
    val selectedIndex = 1

    ScrollableTabRow(
        modifier = modifier,
        selectedTabIndex = if (selectedIndex != -1) selectedIndex else 0,
        edgePadding = edgePadding,
    ) {
        items.forEachIndexed { index, item ->
            if (isHeader(item)) {
                // --- This is a Group Header ---
                Tab(
                    selected = false, // Headers are never "selected"
                    onClick = { /* Do nothing, it's just a label */ },
                    enabled = false, // Make it non-interactive
                    text = {
                        Text(
                            text = getHeaderTitle(item).uppercase(),
                            style = headerStyle,
                            color = headerColor
                        )
                    }
                )
            } else {
                // --- This is a Regular Tab ---
                // We can safely cast here because isHeader was false.
                @Suppress("UNCHECKED_CAST")
                val tabItem = item as T
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onTabSelected(tabItem) },
                    text = {
                        Text(text = getTabTitle(tabItem))
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun AccountGroupingTabs() {
    val selectedGrouping: AccountGrouping = AccountGrouping.NONE
    val onTabSelected: (AccountGrouping) -> Unit = { }
    // 1. Prepare the mixed list of items.
    val tabItems = remember {
        listOf(
            "GROUP BY", // A simple String header
            AccountGrouping.TYPE,
            AccountGrouping.CURRENCY,
            "OPTIONS", // Another String header
            AccountGrouping.NONE
        )
    }

    // 2. Call the generic composable with the required lambdas.
    GroupedScrollableTabRow(
        items = tabItems,
        selectedItem = selectedGrouping,
        onTabSelected = onTabSelected,
        isHeader = { it is String }, // An item is a header if it's a String
        getHeaderTitle = { it as String }, // The header's title is the string itself
        getTabTitle = { stringResource(id = it.title) } // The tab's title comes from the enum's resource id
    )
}
