package org.totschnig.myexpenses.compose.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.TooltipIconMenu
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.viewmodel.data.BaseAccount

@Composable
fun AccountGroupingMenu(
    activeGrouping: AccountGrouping<*>,
    onGroupingChange: (AccountGrouping<*>) -> Unit,
) {
    val groupingOptions = remember { AccountGrouping.ALL_VALUES }

    TooltipIconMenu(
        tooltip = stringResource(R.string.menu_grouping),
        imageVector = Icons.Default.GridView,
        menu = groupingOptions.map { option ->
            CheckableMenuEntry(
                label = UiText.StringValue(option.toString()),
                action = { onGroupingChange(option) },
                command = "CHANGE_GROUPING",
                isChecked = activeGrouping == option,
                isRadio = true
            )
        }
    )
}

@Composable
fun AccountFilterMenu(
    activeFilter: AccountGroupingKey?,
    availableFilters: List<AccountGroupingKey>,
    onFilterChange: (AccountGroupingKey?) -> Unit,
) {
    val context = LocalContext.current

    TooltipIconMenu(
        tooltip = stringResource(R.string.filter),
        imageVector = Icons.Default.FilterAlt,
        menu = listOf(
            CheckableMenuEntry(
                label = UiText.StringResource(R.string.show_all),
                action = { onFilterChange(null) },
                command = "RESET_FILTER",
                isChecked = activeFilter == null,
                isRadio = true
            )
        ) + availableFilters.map { filter ->
            CheckableMenuEntry(
                label = UiText.StringValue(filter.title(context)),
                action = { onFilterChange(filter) },
                command = "CHANGE_FILTER",
                isChecked = activeFilter == filter,
                isRadio = true
            )
        }
    )
}

@Composable
fun TransactionSortMenu(
    currentAccount: BaseAccount,
    onSortChange: (TransactionSort) -> Unit,
) {

    TooltipIconMenu(
        tooltip = stringResource(R.string.display_options_sort_list_by),
        imageVector = Icons.Default.SortByAlpha,
        menu = TransactionSort.entries.map {
            CheckableMenuEntry(
                label = UiText.StringValue("${stringResource(it.label)} / ${stringResource(it.sortDirection.label)}"),
                action = { onSortChange(it) },
                isChecked = currentAccount.sortDirection == it.sortDirection && currentAccount.sortBy == it.column,
                isRadio = true
            )
        }
    )
}

@Composable
fun TransactionGroupingMenu(
    currentGroup: Grouping,
    onGroupingChange: (Grouping) -> Unit,
) {

    TooltipIconMenu(
        tooltip = stringResource(R.string.menu_grouping),
        imageVector = Icons.Default.GridView,
        menu = Grouping.entries.map { grouping ->
            CheckableMenuEntry(
                label = UiText.StringResource(grouping.label),
                action = { onGroupingChange(grouping) },
                isChecked = grouping == currentGroup,
                isRadio = true
            )
        }
    )
}