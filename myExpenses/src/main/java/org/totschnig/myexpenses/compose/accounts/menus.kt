package org.totschnig.myexpenses.compose.accounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.compose.TooltipIconMenu
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.model.AccountGrouping


@Composable
fun ViewOptionsMenu(
    activeGrouping: AccountGrouping<*>,
    onGroupingChange: (AccountGrouping<*>) -> Unit,
    onSort: () -> Unit
) {

    val groupingOptions = remember { AccountGrouping.ALL_VALUES }

    TooltipIconMenu(
        imageVector = Icons.Default.Tune,
        tooltip = stringResource(R.string.options),
        menu = listOf(
            SubMenuEntry(
                label = R.string.menu_grouping,
                subMenu = groupingOptions.map { option ->
                    CheckableMenuEntry(
                        label = UiText.StringValue(option.toString()),
                        isChecked = activeGrouping == option,
                        command = "CHANGE_GROUPING",
                        isRadio = true
                    ) { onGroupingChange(option) }
                },
                icon = Icons.Default.DateRange
            ),
            MenuEntry(
              label =   R.string.display_options_sort_list_by,
                icon = Icons.Default.SortByAlpha,
                command = "SORT",
                action = onSort
            ),
        )
    )
}

@Composable
fun ManageEntitiesMenu(onEvent: AppEventHandler) {
    TooltipIconMenu(
        tooltip = stringResource(R.string.data),
        imageVector = Icons.Default.DataObject,
        menu = listOf(
            MenuEntry(
                label = UiText.StringResource(R.string.menu_account_types)
            ) { onEvent(AppEvent.MenuItemClicked(R.id.MANAGE_ACCOUNT_TYPES_COMMAND)) },
            MenuEntry(
                label = UiText.StringResource(R.string.menu_account_flags)
            ) { onEvent(AppEvent.MenuItemClicked(R.id.ACCOUNT_FLAGS_COMMAND)) }
        )
    )
}