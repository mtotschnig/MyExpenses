package org.totschnig.myexpenses.compose.main

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.viewmodel.data.BaseAccount

@Composable
fun AccountGroupingMenu(
    activeGrouping: AccountGrouping<*>,
    onGroupingChange: (AccountGrouping<*>) -> Unit,
) {
    Box {
        val showMenu = remember { mutableStateOf(false) }
        val groupingOptions = remember { AccountGrouping.ALL_VALUES }

        IconButton(onClick = { showMenu.value = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.List,
                contentDescription = "Filter and Group"
            )
        }
        HierarchicalMenu(
            expanded = showMenu,
            menu = Menu(groupingOptions.map { option ->
                CheckableMenuEntry(
                    label = UiText.StringValue(option.toString()),
                    action = { onGroupingChange(option) },
                    command = "CHANGE_GROUPING",
                    isChecked = activeGrouping == option,
                    isRadio = true
                )
            }),
            title = stringResource(R.string.menu_grouping)
        )
    }
}

@Composable
fun AccountFilterMenu(
    activeFilter: AccountGroupingKey?,
    availableFilters: List<AccountGroupingKey>,
    onFilterChange: (AccountGroupingKey?) -> Unit,
) {
    Box {
        val showMenu = remember { mutableStateOf(false) }
        val context = LocalContext.current
        IconButton(onClick = { showMenu.value = true }) {
            Icon(
                imageVector = Icons.Default.FilterAlt,
                contentDescription = "Filter"
            )
        }
        HierarchicalMenu(
            expanded = showMenu,
            menu = Menu(
                listOf(
                    CheckableMenuEntry(
                        label = UiText.StringResource(R.string.show_all),
                        action = { onFilterChange(null) },
                        command = "RESET_FILTER",
                        isChecked = activeFilter == null,
                        isRadio = true
                    )
                ) +
                        availableFilters.map { filter ->
                            CheckableMenuEntry(
                                label = UiText.StringValue(filter.title(context)),
                                action = { onFilterChange(filter) },
                                command = "CHANGE_FILTER",
                                isChecked = activeFilter == filter,
                                isRadio = true
                            )
                        }),
        )
    }
}

@Composable
fun TransactionSortMenu(
    currentAccount: BaseAccount,
    onSortChange: (String, SortDirection) -> Unit
) {
    val showMenu = remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu.value = true }) {
        Icon(
            painterResource(R.drawable.ic_menu_sort),
            contentDescription = "Filter"
        )
    }
    val availableSortOptions =
        listOf(
            stringResource(R.string.date) to KEY_DATE,
            stringResource(R.string.amount) to KEY_AMOUNT
        )
    HierarchicalMenu(
        expanded = showMenu,
        menu = Menu(
            entries = buildList {
                availableSortOptions.forEach { (title, key) ->
                    SortDirection.entries.forEach { direction ->
                        add(
                            CheckableMenuEntry(
                                label = UiText.StringValue("$title / ${stringResource(direction.label)}"),
                                action = { onSortChange(key, direction) },
                                isChecked = currentAccount.sortDirection == direction && currentAccount.sortBy == key,
                                isRadio = true
                            )
                        )
                    }
                }
            }
        )
    )
}

@Composable
fun TransactionGroupingMenu(
    currentGroup: Grouping,
    onGroupingChange: (Grouping) -> Unit
) {
    val showMenu = remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu.value = true }) {
        Icon(
            painterResource(R.drawable.ic_action_group),
            contentDescription = "Filter"
        )
    }
    HierarchicalMenu(
        expanded = showMenu,
        menu = Menu(
            entries = buildList {
                Grouping.entries.forEach { grouping ->
                    add(
                        CheckableMenuEntry(
                            label = UiText.StringResource(grouping.label),
                            action = { onGroupingChange(grouping) },
                            isChecked = grouping == currentGroup,
                            isRadio = true
                        )
                    )
                }
            }
        )
    )
}