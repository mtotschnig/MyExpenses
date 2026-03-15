package org.totschnig.myexpenses.compose.transactions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
import org.totschnig.myexpenses.provider.KEY_DATE
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
                isChecked = activeFilter == null,
                command = "RESET_FILTER",
                isRadio = true
            ) { onFilterChange(null) }
        ) + availableFilters.map { filter ->
            CheckableMenuEntry(
                label = UiText.StringValue(filter.title(context)),
                isChecked = activeFilter == filter,
                command = "CHANGE_FILTER",
                isRadio = true
            ) { onFilterChange(filter) }
        }
    )
}

@Composable
private fun viewOptions(currentAccount: BaseAccount, onEvent: AppEventHandler) =
    listOfNotNull(
        SubMenuEntry(
            label = R.string.display_options_sort_list_by,
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
            },
            icon = Icons.Default.SortByAlpha
        ),
        if (currentAccount.sortBy == KEY_DATE)
            SubMenuEntry(
                label = R.string.menu_grouping,
                subMenu = Grouping.entries.map { grouping ->
                    CheckableMenuEntry(
                        label = UiText.StringResource(grouping.label),
                        isChecked = grouping == currentAccount.grouping,
                        isRadio = true
                    ) { onEvent(AppEvent.SetTransactionGrouping(grouping)) }
                },
                icon = Icons.Default.DateRange
            ) else null
    )

@Composable
fun ViewOptionsMenu(
    currentAccount: BaseAccount,
    onEvent: AppEventHandler,
) {
    TooltipIconMenu(
        imageVector = Icons.Default.Tune,
        tooltip = stringResource(R.string.options),
        menu = viewOptions(currentAccount, onEvent)
    )
}

@Composable
fun ActionMenu(
    currentAccount: BaseAccount,
    items: List<MenuItem>,
    onEvent: AppEventHandler,
    isChecked: @Composable (MenuItem) -> Boolean
) {
    if (items.isNotEmpty()) {
        TooltipIconMenu(
            imageVector = Icons.Default.MoreVert,
            tooltip = stringResource(R.string.actions),
            menu = items
                .map {
                    when {
                        it == MenuItem.Tune -> SubMenuEntry(
                            label = UiText.StringValue(it.getLabel(LocalContext.current)),
                            icon = { it.painter },
                            subMenu = viewOptions(currentAccount, onEvent)
                        )
                        it.isCheckable -> {
                            val isChecked = isChecked(it)
                            CheckableMenuEntry(
                                label = UiText.StringValue(it.getLabel(LocalContext.current)),
                                isChecked = isChecked
                            ) {
                                onEvent(AppEvent.MenuItemClicked(it.id, !isChecked))
                            }
                        }
                        else -> MenuEntry(
                            label = UiText.StringValue(it.getLabel(LocalContext.current)),
                            icon = { it.painter }
                        ) { onEvent(AppEvent.MenuItemClicked(it.id)) }
                    }
                }
        )
    }
}