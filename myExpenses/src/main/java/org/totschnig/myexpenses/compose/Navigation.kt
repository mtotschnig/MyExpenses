package org.totschnig.myexpenses.compose

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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import myiconpack.ArrowsAlt
import org.totschnig.myexpenses.R

typealias Menu = List<IMenuEntry>

sealed interface IMenuEntry {
    val label: UiText
    val icon: @Composable (() -> Painter)?
    val tint: Color?
    val contentDescription: String?
}

sealed interface IActionMenuEntry : IMenuEntry {
    val action: () -> Unit
    val command: String?
}

data class SubMenuEntry(
    override val label: UiText,
    val subMenu: Menu,
    override val icon: @Composable (() -> Painter)? = null,
    override val tint: Color? = null,
    override val contentDescription: String? = null
) : IMenuEntry {
    constructor(label: Int, subMenu: Menu, icon: ImageVector? = null) : this(
        label = UiText.StringResource(label),
        subMenu = subMenu,
        icon = if (icon != null) {
            @Composable { rememberVectorPainter(image = icon) }
        } else null
    )
}

data class CheckableMenuEntry(
    override val label: UiText,
    val isChecked: Boolean,
    override val command: String? = null,
    val isRadio: Boolean = false,
    override val contentDescription: String? = null,
    override val action: () -> Unit,
) : IActionMenuEntry {
    constructor(
        label: Int,
        isChecked: Boolean,
        command: String,
        isRadio: Boolean = false,
        contentDescription: String? = null,
        action: () -> Unit,
    ) :
            this(
                UiText.StringResource(label),
                isChecked,
                command,
                isRadio,
                contentDescription,
                action
            )

    override val icon: @Composable () -> Painter
        get() = {
            rememberVectorPainter(
                when {
                    isRadio && isChecked -> Icons.Filled.RadioButtonChecked
                    isRadio -> Icons.Filled.RadioButtonUnchecked
                    isChecked -> Icons.Filled.CheckBox
                    else -> Icons.Filled.CheckBoxOutlineBlank
                }
            )
        }
    override val tint: Color? = null
}

data class MenuEntry(
    override val label: UiText,
    override val command: String? = null,
    override val icon: @Composable (() -> Painter)? = null,
    override val tint: Color? = null,
    override val contentDescription: String? = null,
    override val action: () -> Unit,
) : IActionMenuEntry {
    constructor(
        label: Int,
        command: String,
        icon: ImageVector? = null,
        tint: Color? = null,
        contentDescription: String? = null,
        action: () -> Unit,
    ) : this(
        UiText.StringResource(label),
        command,
        if (icon != null) {
            @Composable { rememberVectorPainter(image = icon) }
        } else null, tint, contentDescription, action
    )

    companion object {
        fun delete(command: String, action: () -> Unit) = MenuEntry(
            label = R.string.menu_delete,
            command = command,
            icon = Icons.Filled.Delete,
            action = action
        )

        fun edit(command: String, action: () -> Unit) = MenuEntry(
            label = R.string.menu_edit,
            command = command,
            icon = Icons.Filled.Edit,
            action = action
        )

        fun select(command: String, action: () -> Unit) = MenuEntry(
            label = R.string.select,
            command = command,
            icon = Icons.Filled.Check,
            action = action
        )

        fun toggle(command: String, isSealed: Boolean, action: () -> Unit) = MenuEntry(
            label = if (isSealed) R.string.menu_reopen else R.string.menu_close,
            command = command + "_ " + if (isSealed) "_REOPEN" else "_CLOSE",
            icon = if (isSealed) Icons.Filled.LockOpen else Icons.Filled.Lock,
            action = action
        )
    }
}

@Composable
fun OverFlowMenu(
    modifier: Modifier = Modifier,
    menu: Menu,
) {
    TooltipIconMenu(
        modifier = modifier,
        tooltip = stringResource(id = androidx.appcompat.R.string.abc_action_menu_overflow_description),
        imageVector = Icons.Filled.MoreVert,
        menu = menu,
    )
}

/**
 * the submenus are rendered as an expandable sub list.
 * We tried to render proper submenus, but were not able to get the position of the submenu right
 */
@Composable
fun HierarchicalMenu(
    expanded: MutableState<Boolean>,
    menu: Menu,
    title: String? = null,
) {
    DropdownMenu(
        modifier = Modifier.testTag(TEST_TAG_CONTEXT_MENU),
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        EntryListRenderer(expanded, menu)
    }
}

@Composable
private fun RowScope.EntryContent(entry: IMenuEntry, offset: Dp = 0.dp) {
    Spacer(modifier = Modifier.width(offset))
    entry.icon?.let {
        Icon(
            modifier = Modifier
                .padding(end = 5.dp)
                .size(24.dp),
            painter = it(),
            tint = entry.tint ?: LocalContentColor.current,
            contentDescription = null
        )
    }
    Text(text = entry.label.asString(), modifier = Modifier.weight(1f))
}

@Composable
private fun EntryListRenderer(
    expanded: MutableState<Boolean>,
    menu: Menu,
    offset: Dp = 0.dp,
) {
    val tracker = LocalTracker.current
    menu.forEach { entry ->
        when (entry) {
            is IActionMenuEntry -> {
                DropdownMenuItem(
                    modifier = Modifier.optional(entry.command) {
                        testTag(it)
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.optional(entry.contentDescription) {
                                semantics { contentDescription = it }
                            }
                        ) {
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
                label = R.string.menu_edit,
                command = "",
                icon = Icons.Filled.Edit
            ) {})
    }
}

@Preview
@Composable
private fun Overflow() {
    fun emptyEntry(label: Int) = MenuEntry(label = UiText.StringResource(label), command = "") {}
    OverFlowMenu(
        menu = listOf(
            emptyEntry(R.string.menu_learn_more),
            SubMenuEntry(
                label = R.string.hide, subMenu = listOf(
                    MenuEntry(
                        label = R.string.menu_edit,
                        command = "",
                        icon = Icons.Filled.Edit
                    ) {},
                    MenuEntry(
                        label = R.string.menu_move,
                        command = "",
                        icon = ArrowsAlt
                    ) {}
                )
            )
        )
    )
}

