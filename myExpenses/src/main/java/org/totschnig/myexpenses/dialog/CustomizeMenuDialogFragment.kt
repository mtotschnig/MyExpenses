package org.totschnig.myexpenses.dialog

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.livefront.sealedenum.GenSealedEnum
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import timber.log.Timber
import java.util.Collections

@Parcelize
sealed class MenuItem(
    @IdRes val id: Int,
    @StringRes private val labelRes: Int,
    @DrawableRes val icon: Int? = null,
    @MenuRes val subMenu: Int? = null,
    val isCheckable: Boolean = false,
    val isEnabledByDefault: Boolean = true
): Parcelable {
    open fun getLabel(context: Context) = context.getString(labelRes)

    data object Search : MenuItem(
        R.id.SEARCH_COMMAND,
        R.string.menu_search,
        R.drawable.ic_menu_search,
        R.menu.main_search,
        true
    )
    data object Templates : MenuItem(
        R.id.MANAGE_TEMPLATES_COMMAND,
        R.string.menu_templates,
        R.drawable.ic_menu_template
    )

    data object Budget : MenuItem(
        R.id.BUDGET_COMMAND,
        R.string.menu_budget,
        R.drawable.ic_budget
    )

    data object Distribution : MenuItem(
        R.id.DISTRIBUTION_COMMAND,
        R.string.menu_distribution,
        R.drawable.ic_menu_chart
    )

    data object History : MenuItem(
        R.id.HISTORY_COMMAND,
        R.string.menu_history,
        R.drawable.ic_history
    )

    data object Parties : MenuItem(
        R.id.MANAGE_PARTIES_COMMAND,
        0,
        R.drawable.ic_group
    ) {
        override fun getLabel(context: Context) = TextUtils.concatResStrings(
            context, " / ", R.string.pref_manage_parties_title, R.string.debts
        )
    }

    data object ScanMode: MenuItem(
        R.id.SCAN_MODE_COMMAND,
        R.string.menu_scan_mode,
        R.drawable.ic_scan,
        isCheckable = true
    )

    data object Reset : MenuItem(
        R.id.RESET_COMMAND,
        R.string.menu_reset,
        R.drawable.ic_menu_download
    )

    data object Sync : MenuItem(
        R.id.SYNC_COMMAND,
        R.string.menu_sync_now,
        R.drawable.ic_sync
    )

    data object FinTsSync : MenuItem(
        R.id.FINTS_SYNC_COMMAND,
        0,
        R.drawable.ic_bank
    ) {
        override fun getLabel(context: Context) =
            (context as? BaseActivity)?.bankingFeature?.syncMenuTitle(context) ?: "FinTS"
    }

    data object ShowStatusHandle: MenuItem(
        R.id.SHOW_STATUS_HANDLE_COMMAND,
        R.string.status,
        R.drawable.ic_square
    )

    data object Balance : MenuItem(
        R.id.BALANCE_COMMAND,
        R.string.menu_balance,
        R.drawable.ic_action_balance
    )

    data object Sort : MenuItem(
        R.id.SORT_MENU,
        R.string.display_options_sort_list_by,
        R.drawable.ic_menu_sort,
        R.menu.main_sort
    )

    data object Grouping: MenuItem(
        R.id.GROUPING_COMMAND,
        R.string.menu_grouping,
        R.drawable.ic_action_group,
        R.menu.grouping
    )

    data object Print : MenuItem(
        R.id.PRINT_COMMAND,
        R.string.menu_print,
        R.drawable.ic_menu_print
    )

    data object Manage : MenuItem(
        R.id.MANAGE_ACCOUNT_COMMAND,
        R.string.account,
        R.drawable.ic_menu_edit,
        R.menu.main_manage
    )

    data object Share : MenuItem(
        R.id.SHARE_COMMAND,
        R.string.menu_share,
        R.drawable.ic_share
    )

    data object Settings : MenuItem(
        R.id.SETTINGS_COMMAND,
        R.string.settings_label,
        R.drawable.ic_settings
    )

    data object Help: MenuItem(
        R.id.HELP_COMMAND,
        R.string.menu_help,
        R.drawable.ic_menu_help
    )

    data object Backup: MenuItem(
        R.id.BACKUP_COMMAND,
        R.string.menu_backup,
        isEnabledByDefault = false
    )

    @GenSealedEnum
    companion object {
        val defaultConfiguration = values.filter { it.isEnabledByDefault }
    }
}

class CustomizeMenuDialogFragment : ComposeBaseDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefHandler.getStringSet(PrefKey.CUSTOMIZE_MAIN_MENU)?.forEach {
            Timber.i(it)
        }
    }

    @Composable
    override fun BuildContent() {
        val activeItems = rememberMutableStateListOf(prefHandler.mainMenu)
        val inactiveItems = rememberMutableStateListOf(MenuItem.values - activeItems)

        Column {
            Text(
                modifier = Modifier.padding(top = dialogPadding, start = dialogPadding),
                style = MaterialTheme.typography.titleLarge,
                text = TextUtils.concatResStrings(
                    LocalContext.current,
                    " : ",
                    R.string.menu,
                    R.string.customize
                )
            )
            LazyColumn(modifier = Modifier
                .padding(dialogPadding)
                .weight(1f)) {
                itemsIndexed(activeItems) { index, item ->
                    ItemRow(item, true,
                        onCheckedChange = if (item != MenuItem.Settings) {
                            {
                                activeItems.remove(item)
                                inactiveItems.add(item)
                            }
                        } else null,
                        onUp = if (index == 0) null else {
                            { Collections.swap(activeItems, index - 1, index) }
                        },
                        onDown = if (index < activeItems.lastIndex) {
                            { Collections.swap(activeItems, index, index + 1) }
                        } else null
                    )
                }
                if (activeItems.isNotEmpty() && inactiveItems.isNotEmpty()) {
                    item {
                        Divider(thickness = 1.dp)
                    }
                }
                items(inactiveItems) { item ->
                    ItemRow(item, false, onCheckedChange = {
                        activeItems.add(item)
                        inactiveItems.remove(item)
                    })
                }
            }
            ButtonRow(
                modifier = Modifier.padding(
                    bottom = dialogPadding,
                    start = dialogPadding,
                    end = dialogPadding
                )
            ) {
                TextButton(onClick = { dismiss() }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
                TextButton(onClick = {
                    inactiveItems.clear()
                    activeItems.clear()
                    activeItems.addAll(MenuItem.defaultConfiguration)
                    inactiveItems.addAll(MenuItem.values - activeItems)
                }) {
                    Text(stringResource(id = R.string.menu_reset_plan_instance))
                }
                TextButton(onClick = {
                    prefHandler.putStringSet(
                        PrefKey.CUSTOMIZE_MAIN_MENU,
                        LinkedHashSet(activeItems.map { it.name })
                    )
                    dismiss()
                }) {
                    Text(stringResource(id = R.string.confirm))
                }
            }
        }
    }

    @Composable
    private fun ItemRow(
        item: MenuItem,
        checked: Boolean,
        onCheckedChange: (() -> Unit)?,
        onUp: (() -> Unit)? = null,
        onDown: (() -> Unit)? = null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, enabled = onCheckedChange != null, onCheckedChange = {
                onCheckedChange?.invoke()
            })
            item.icon?.let { Icon(
                modifier = Modifier.padding(end = 8.dp),
                painter = painterResource(id = it),
                contentDescription = null)
            }
            Text(item.getLabel(LocalContext.current))
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.width(96.dp), horizontalArrangement = Arrangement.Center) {
                onUp?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(id = R.string.action_move_up)
                        )
                    }
                }
                onDown?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(id = R.string.action_move_down)
                        )
                    }
                }
            }

        }
    }
}