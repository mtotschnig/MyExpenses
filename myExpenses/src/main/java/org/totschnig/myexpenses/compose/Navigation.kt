package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    val label: UiText
    val icon: ImageVector?
}

sealed interface IActionMenuEntry : IMenuEntry {
    val action: () -> Unit
    val command: String?
}

data class SubMenuEntry(
    override val icon: ImageVector? = null,
    override val label: UiText,
    val subMenu: Menu
) : IMenuEntry {
    constructor(icon: ImageVector? = null, label: Int, subMenu: Menu) :
            this(icon, UiText.StringResource(label), subMenu)
}

data class CheckableMenuEntry(
    override val label: UiText,
    override val command: String? = null,
    val isChecked: Boolean,
    val isRadio: Boolean = false,
    override val action: () -> Unit
) : IActionMenuEntry {
    constructor(label: Int, command: String, isChecked: Boolean, isRadio: Boolean = false, action: () -> Unit) :
            this(UiText.StringResource(label), command, isChecked, isRadio, action)

    override val icon: ImageVector
        get() = when {
            isRadio && isChecked -> Icons.Filled.RadioButtonChecked
            isRadio -> Icons.Filled.RadioButtonUnchecked
            isChecked -> Icons.Filled.CheckBox
            else -> Icons.Filled.CheckBoxOutlineBlank
        }
}

data class MenuEntry(
    override val icon: ImageVector? = null,
    override val label: UiText,
    override val command: String? = null,
    override val action: () -> Unit
) : IActionMenuEntry {
    constructor(icon: ImageVector? = null, label: Int, command: String, action: () -> Unit) :
            this(icon, UiText.StringResource(label), command, action)
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
            command = command + "_ " + if (isSealed) "_REOPEN" else "_CLOSE",
            action = action
        )
    }
}

@Composable
fun OverFlowMenu(
    modifier: Modifier = Modifier,
    menu: Menu
) {
    val showMenu = rememberSaveable { mutableStateOf(false) }
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
    menu: Menu,
    title: String? = null
) {
    DropdownMenu(
        modifier = Modifier.testTag(TEST_TAG_CONTEXT_MENU),
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        title?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp))
        }
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
    Text(text = entry.label.asString(), modifier = Modifier.weight(1f))
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
                    modifier = Modifier.optional(entry.command) {
                        testTag(it)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EntryContent(entry, offset)
                        }
                    },
                    onClick = {
                        expanded.value = false
                        entry.command?.let { tracker.trackCommand(it) }
                        entry.action()
                    }
                )
            }

            is SubMenuEntry -> {
                var subMenuVisible by remember { mutableStateOf(false) }
                DropdownMenuItem(
                    text = {
                        Row {
                            EntryContent(entry, offset)
                            Icon(
                                imageVector = if (subMenuVisible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(
                                    if (subMenuVisible) R.string.collapse
                                    else R.string.expand
                                ),
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(start = 5.dp)
                            )
                        }
                    },
                    onClick = { subMenuVisible = !subMenuVisible }
                )
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
private fun Entry() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EntryContent(
            MenuEntry(
                icon = Icons.Filled.Edit,
                label = R.string.menu_edit,
                command = ""
            ) {})
    }
}

@Preview
@Composable
private fun Overflow() {
    fun emptyEntry(label: Int) = MenuEntry(label = UiText.StringResource(label), command = "") {}
    OverFlowMenu(
        menu = Menu(
            entries = listOf(
                emptyEntry(R.string.menu_learn_more),
                SubMenuEntry(
                    label = R.string.hide, subMenu = Menu(
                        entries = listOf(
                            MenuEntry(
                                icon = Icons.Filled.Edit,
                                label = R.string.menu_edit,
                                command = ""
                            ) {},
                            MenuEntry(
                                icon = myiconpack.ArrowsAlt,
                                label = R.string.menu_move,
                                command = ""
                            ) {}
                        )
                    )
                )
            )
        )
    )
}

