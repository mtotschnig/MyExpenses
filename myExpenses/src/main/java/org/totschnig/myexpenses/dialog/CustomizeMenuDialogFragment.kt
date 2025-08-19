package org.totschnig.myexpenses.dialog

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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbar
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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