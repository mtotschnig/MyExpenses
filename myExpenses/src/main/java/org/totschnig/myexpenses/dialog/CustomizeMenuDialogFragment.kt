package org.totschnig.myexpenses.dialog

import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.livefront.sealedenum.GenSealedEnum
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbar
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Parcelize
@Stable
sealed class MenuItem(
    @IdRes val id: Int,
    @StringRes private val labelRes: Int,
    @DrawableRes val icon: Int? = null,
    @MenuRes val subMenu: Int? = null,
    val isCheckable: Boolean = false,
    val isEnabledByDefault: Boolean = true,
) : Parcelable {
    open fun getLabel(context: Context) = context.getString(labelRes)

    data object Search : MenuItem(
        R.id.SEARCH_COMMAND,
        R.string.menu_search,
        R.drawable.ic_menu_search,
        isCheckable = true
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

    data object ScanMode : MenuItem(
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

    data object ShowStatusHandle : MenuItem(
        R.id.SHOW_STATUS_HANDLE_COMMAND,
        R.string.status,
        R.drawable.ic_square,
        isCheckable = true
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

    data object Grouping : MenuItem(
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

    data object Archive : MenuItem(
        R.id.ARCHIVE_COMMAND,
        R.string.action_archive,
        R.drawable.ic_archive,
        isEnabledByDefault = true
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

    data object Help : MenuItem(
        R.id.HELP_COMMAND,
        R.string.menu_help,
        R.drawable.ic_menu_help
    )

    data object Backup : MenuItem(
        R.id.BACKUP_COMMAND,
        R.string.menu_backup,
        R.drawable.ic_menu_save,
        isEnabledByDefault = false
    )

    data object WebUI : MenuItem(
        R.id.WEB_UI_COMMAND,
        R.string.title_webui,
        R.drawable.ic_computer,
        isEnabledByDefault = false,
        isCheckable = true
    )

    data object Restore : MenuItem(
        R.id.RESTORE_COMMAND,
        R.string.pref_restore_title,
        R.drawable.settings_backup_restore,
        isEnabledByDefault = false
    )

    @GenSealedEnum
    companion object {
        val defaultConfiguration: List<MenuItem>
            get() = values.filter { it.isEnabledByDefault }
    }
}

class CustomizeMenuDialogFragment : ComposeBaseDialogFragment3() {

    override val horizontalPadding = 12.dp

    override val fullScreenIfNotLarge = true

    override val title: CharSequence
        get() = TextUtils.concatResStrings(
            requireContext(),
            " : ",
            R.string.menu,
            R.string.customize
        )

    @Composable
    override fun ColumnScope.MainContent() {
        val activeItems = rememberMutableStateListOf(prefHandler.mainMenu)
        val inactiveItems = rememberMutableStateListOf(MenuItem.values - activeItems)

        MenuConfigurator(activeItems, inactiveItems, Modifier.weight(1f))
        ButtonRow {
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
                prefHandler.putOrderedStringSet(
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
private fun MenuConfigurator(
    activeItems: SnapshotStateList<MenuItem>,
    inactiveItems: SnapshotStateList<MenuItem>,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    fun onMove(from: Int, to: Int) {
        if (to < activeItems.size) {
            activeItems.add(to, activeItems.removeAt(from))
        }
    }

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.index, to.index)
    }
    val moveUpLabel = stringResource(id = R.string.action_move_up)
    val moveDownLabel = stringResource(id = R.string.action_move_down)
    val moveTopLabel = stringResource(id = R.string.action_move_top)
    val moveBottomLabel = stringResource(id = R.string.action_move_bottom)

    LazyColumnWithScrollbar(
        modifier = modifier,
        state = lazyListState,
        itemsAvailable = MenuItem.values.size,
    ) {
        itemsIndexed(activeItems, key = { _, item -> item.id }) { index, item ->
            ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                val customActions = buildList {
                    if (index > 0) {
                        add(
                            CustomAccessibilityAction(
                                label = moveUpLabel,
                                action = {
                                    onMove(index, index - 1)
                                    true
                                }
                            ))
                        add(
                            CustomAccessibilityAction(
                                label = moveTopLabel,
                                action = {
                                    onMove(index, 0)
                                    true
                                }
                            ))
                    }
                    if (index < activeItems.lastIndex) {
                        add(
                            CustomAccessibilityAction(
                                label = moveDownLabel,
                                action = {
                                    onMove(index, index + 1)
                                    true
                                }
                            ))
                        add(
                            CustomAccessibilityAction(
                                label = moveBottomLabel,
                                action = {
                                    onMove(index, activeItems.lastIndex)
                                    true
                                }
                            ))
                    }
                }
                ItemRow(
                    reorderScope = this,
                    item = item,
                    index = index,
                    checked = true,
                    onCheckedChange = if (item != MenuItem.Settings) {
                        {
                            activeItems.remove(item)
                            inactiveItems.add(item)
                        }
                    } else null,
                    customActions = customActions
                )
            }
        }
        if (activeItems.isNotEmpty() && inactiveItems.isNotEmpty()) {
            item {
                ReorderableItem(reorderableLazyListState, 0, enabled = false) {
                    HorizontalDivider(thickness = 1.dp)
                }
            }
        }
        itemsIndexed(inactiveItems, key = { _, item -> item.id }) { index, item ->
            ReorderableItem(reorderableLazyListState, item.id, enabled = false) {
                ItemRow(
                    reorderScope = null,
                    item = item,
                    index = activeItems.size + index,
                    checked = false,
                    onCheckedChange = {
                        activeItems.add(item)
                        inactiveItems.remove(item)
                    },
                    customActions = null
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    reorderScope: ReorderableCollectionItemScope?,
    item: MenuItem,
    index: Int,
    checked: Boolean,
    onCheckedChange: (() -> Unit)?,
    customActions: List<CustomAccessibilityAction>?,
) {
    Row(
        modifier = Modifier.semantics {
            collectionItemInfo = CollectionItemInfo(
                rowIndex = index,
                rowSpan = 1,
                columnIndex =  0,
                columnSpan = 1
            )
        }
            .padding(12.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                enabled = onCheckedChange != null,
                onValueChange = { onCheckedChange?.invoke() }
            )
            .optional(customActions?.takeIf { it.isNotEmpty() }) {
                semantics { this.customActions = it }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, enabled = onCheckedChange != null, onCheckedChange = null)
        item.icon?.let {
            Icon(
                modifier = Modifier.padding(horizontal = 8.dp),
                painter = painterResource(id = it),
                contentDescription = null
            )
        }
        Text(
            text = item.getLabel(LocalContext.current),
            modifier = Modifier
                .weight(1f)
        )
        if (reorderScope != null) {
            with(reorderScope) {
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.draggableHandle()
                )
            }
        }
    }
}

@Preview
@Composable
fun DragAndDropDemo() {
    val activeItems: SnapshotStateList<MenuItem> =
        rememberMutableStateListOf(MenuItem.values.subList(0, 15))
    val inActiveItems: SnapshotStateList<MenuItem> = rememberMutableStateListOf(
        MenuItem.values.subList(
            15,
            MenuItem.values.size
        )
    )
    MenuConfigurator(activeItems, inActiveItems)
}