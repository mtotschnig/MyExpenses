package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R

data class Menu(val entries: List<IMenuEntry>)

sealed interface IMenuEntry {
    val label: Int
    val icon: ImageVector?
}

sealed interface IActionMenuEntry: IMenuEntry {
    val action: () -> Unit
    val command: String
}

data class SubMenuEntry(
    override val icon: ImageVector? = null,
    override val label: Int,
    val subMenu: Menu
) : IMenuEntry

data class CheckableMenuEntry(
    override val label: Int,
    override val command: String,
    val isChecked: Boolean,
    override val action: () -> Unit
    ): IActionMenuEntry {
    override val icon: ImageVector
        get() = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
}

data class MenuEntry(
    override val icon: ImageVector? = null,
    override val label: Int,
    override val command: String,
    override val action: () -> Unit
) : IActionMenuEntry {
    companion object {
        fun delete(command: String, action: () -> Unit) = MenuEntry(
            icon = Icons.Filled.Delete,
            label = R.string.menu_delete,
            command = command,
            action = action
        )

        fun edit(command: String, action: () -> Unit) = MenuEntry(
            icon = Icons.Filled.Edit,
            label = R.string.menu_edit,
            command = command,
            action = action
        )

        fun select(command: String, action: () -> Unit) = MenuEntry(
            icon = Icons.Filled.Check,
            label = R.string.select,
            command = command,
            action = action
        )

        fun toggle(command: String, isSealed: Boolean, action: () -> Unit) = MenuEntry(
            icon = if (isSealed) Icons.Filled.LockOpen else Icons.Filled.Lock,
            label = if (isSealed) R.string.menu_reopen else R.string.menu_close,
            command = command + "_ " + if(isSealed) "_REOPEN" else "_CLOSE",
            action = action
        )
    }
}

@Composable
fun OverFlowMenu(
    modifier: Modifier = Modifier,
    menu: Menu
) {
    val showMenu = remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { showMenu.value = true }) {
            Icon(
                Icons.Filled.MoreVert,
                stringResource(id = androidx.appcompat.R.string.abc_action_menu_overflow_description)
            )
        }
        HierarchicalMenu(expanded = showMenu, menu = menu)
    }
}

/**
 * the submenus are rendered as an expandable sub list.
 * We tried to render proper submenus, but were not able to get the position of the submenu right
 */
@Composable
fun HierarchicalMenu(
    expanded: MutableState<Boolean>,
    menu: Menu
) {
    DropdownMenu(
        modifier = Modifier.testTag(TEST_TAG_CONTEXT_MENU),
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        EntryListRenderer(expanded, menu)
    }
}

@Composable
private fun RowScope.EntryContent(entry: IMenuEntry, offset: Dp = 0.dp) {
    Spacer(modifier = Modifier.width(offset))
    entry.icon?.also {
        Icon(
            modifier = Modifier
                .padding(end = 5.dp)
                .size(24.dp),
            imageVector = it,
            contentDescription = null
        )
    }
    Text(text = stringResource(entry.label), modifier = Modifier.weight(1f))
}

@Composable
private fun EntryListRenderer(
    expanded: MutableState<Boolean>,
    menu: Menu,
    offset: Dp = 0.dp
) {
    val tracker = LocalTracker.current
    menu.entries.forEach { entry ->
        when (entry) {
            is IActionMenuEntry -> {
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        tracker.trackCommand(entry.command)
                        entry.action()
                    }
                ) {
                    EntryContent(entry, offset)
                }
            }
            is SubMenuEntry -> {
                var subMenuVisible by remember { mutableStateOf(false) }
                DropdownMenuItem(
                    onClick = { subMenuVisible = !subMenuVisible }
                ) {
                    EntryContent(entry, offset)
                    Icon(
                        imageVector = if (subMenuVisible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            if (subMenuVisible) R.string.content_description_collapse
                            else R.string.content_description_expand
                        ),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(start = 5.dp)
                    )
                }
                if (subMenuVisible) {
                    EntryListRenderer(
                        expanded = expanded,
                        menu = entry.subMenu,
                        offset = offset + 12.dp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun EntryContent() {
    Column {
        DropdownMenuItem(onClick = {}) {
            EntryContent(MenuEntry(icon = Icons.Filled.Edit, label = R.string.menu_edit, command = "") {})
        }
        DropdownMenuItem(onClick = {}) {
            EntryContent(
                MenuEntry(
                    icon = myiconpack.ArrowsAlt,
                    label = R.string.menu_move,
                    command = ""
                ) {})
        }
    }
}

@Preview
@Composable
fun Overflow() {
    fun emptyEntry(label: Int) = MenuEntry(label = label, command = "") {}
    OverFlowMenu(
        menu = Menu(
            entries = listOf(
                emptyEntry(R.string.menu_learn_more),
                SubMenuEntry(
                    label = R.string.menu_hide, subMenu = Menu(
                        entries = listOf(
                            MenuEntry(icon = Icons.Filled.Edit, label = R.string.menu_edit, command = "") {},
                            MenuEntry(icon = myiconpack.ArrowsAlt, label = R.string.menu_move, command = "") {}
                        )
                    )
                )
            )
        )
    )
}

