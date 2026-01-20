package org.totschnig.myexpenses.compose.transactions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.compose.TooltipIconMenu
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.viewmodel.data.BaseAccount

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
fun ViewOptionsMenu(
    currentAccount: BaseAccount,
    onEvent: AppEventHandler,
) {
    TooltipIconMenu(
        imageVector = Icons.Default.Tune,
        tooltip = stringResource(R.string.options),
        menu = listOf(
            SubMenuEntry(
                label = R.string.display_options_sort_list_by,
                icon = Icons.Default.SortByAlpha,
                subMenu = TransactionSort.entries.map {
                    CheckableMenuEntry(
                        label = UiText.StringValue("${stringResource(it.label)} / ${stringResource(it.sortDirection.label)}"),
                        action = { onEvent(AppEvent.SetTransactionSort(it)) },
                        isChecked = currentAccount.sortDirection == it.sortDirection && currentAccount.sortBy == it.column,
                        isRadio = true
                    )
                }
            ),
            SubMenuEntry(
                label = R.string.menu_grouping,
                icon = Icons.Default.GridView,
                subMenu = Grouping.entries.map { grouping ->
                    CheckableMenuEntry(
                        label = UiText.StringResource(grouping.label),
                        action = { onEvent(AppEvent.SetTransactionGrouping(grouping)) },
                        isChecked = grouping == currentAccount.grouping,
                        isRadio = true
                    )
                }
            )
        )
    )
}