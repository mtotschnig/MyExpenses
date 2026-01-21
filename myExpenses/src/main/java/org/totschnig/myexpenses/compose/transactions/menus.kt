package org.totschnig.myexpenses.compose.transactions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.compose.TooltipIconMenu
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.dialog.MenuItem
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
                        label = UiText.StringValue(
                            "${stringResource(it.label)} / ${
                                stringResource(
                                    it.sortDirection.label
                                )
                            }"
                        ),
                        isChecked = currentAccount.sortDirection == it.sortDirection && currentAccount.sortBy == it.column,
                        isRadio = true
                    ) { onEvent(AppEvent.SetTransactionSort(it)) }
                }
            ),
            SubMenuEntry(
                label = R.string.menu_grouping,
                icon = Icons.Default.GridView,
                subMenu = Grouping.entries.map { grouping ->
                    CheckableMenuEntry(
                        label = UiText.StringResource(grouping.label),
                        isChecked = grouping == currentAccount.grouping,
                        isRadio = true
                    ) { onEvent(AppEvent.SetTransactionGrouping(grouping)) }
                }
            )
        )
    )
}

@Composable
fun ActionMenu(
    onEvent: AppEventHandler,
    onPrepareMenuItem: (itemId: Int) -> Boolean = { true },
) {
    val items = listOf(
        MenuItem.Reset,
        MenuItem.Sync,
        MenuItem.FinTsSync,
        MenuItem.Balance,
        MenuItem.Print,
        MenuItem.Archive
    ).filter { onPrepareMenuItem(it.id) }
    if (items.isNotEmpty()) {
        TooltipIconMenu(
            imageVector = Icons.Default.MoreVert,
            tooltip = stringResource(R.string.actions),
            menu = items
                .map {
                    MenuEntry(
                        label = UiText.StringValue(it.getLabel(LocalContext.current)),
                        icon = { painterResource(it.icon) }
                    ) { onEvent(AppEvent.MenuItemClicked(it.id)) }
                }
        )
    }
}