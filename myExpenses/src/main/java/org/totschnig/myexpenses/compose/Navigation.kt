package org.totschnig.myexpenses.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import arrow.core.Either
import org.totschnig.myexpenses.R

@SuppressLint("PrivateResource")
@Composable
fun Navigation(
    onNavigation: () -> Unit = {},
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Navigation(
        onNavigation = onNavigation,
        title = { Text(text = title, style = MaterialTheme.typography.h6) },
        actions = actions,
        content = content
    )
}

@SuppressLint("PrivateResource")
@Composable
fun Navigation(
    onNavigation: () -> Unit = {},
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onNavigation) {
                        Icon(
                            painterResource(id = R.drawable.ic_menu_back),
                            contentDescription = stringResource(R.string.abc_action_bar_up_description)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.toolbarBackground),
                actions = actions
            )
        },
        content = content
    )
}

data class Menu<T>(val entries: List<MenuEntry<T>>)
data class MenuEntry<T>(
    val icon: ImageVector? = null,
    val label: Int,
    val content: Either<(T) -> Unit, Menu<T>>
) {
    constructor(icon: ImageVector? = null, label: Int, action: (T) -> Unit) : this(
        icon,
        label,
        Either.Left(action)
    )

    constructor(icon: ImageVector? = null, label: Int, subMenu: Menu<T>) : this(
        icon,
        label,
        Either.Right(subMenu)
    )
    companion object {
        fun <T> delete(action: (T) -> Unit) = MenuEntry(
            icon = Icons.Filled.Delete,
            label = R.string.menu_delete,
            action = action
        )

        fun <T> edit(action: (T) -> Unit) = MenuEntry(
            icon = Icons.Filled.Edit,
            label = R.string.menu_edit,
            action = action
        )

        fun <T> select(action: (T) -> Unit) = MenuEntry(
            icon = Icons.Filled.Check,
            label = R.string.select,
            action = action
        )

        fun <T> toggle(isSealed: Boolean, action: (T) -> Unit) = MenuEntry(
            icon = if (isSealed) Icons.Filled.LockOpen else Icons.Filled.Lock,
            label = if (isSealed) R.string.menu_reopen else R.string.menu_close,
            action = action
        )
    }
}
typealias GenericMenuEntry = MenuEntry<Unit>

@Composable
fun <T> OverFlowMenu(
    modifier: Modifier = Modifier,
    menu: Menu<T>,
    target: T
) {
    val showMenu = remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { showMenu.value = true }) {
            Icon(
                Icons.Filled.MoreVert,
                stringResource(id = R.string.abc_action_menu_overflow_description)
            )
        }
        HierarchicalMenu(expanded = showMenu, menu = menu, target = target)
    }
}

/**
 * the submenus are rendered as an expandable sub list.
 * We tried to render proper submenus, but were not able to get the position of the submenu right
 */
@Composable
fun <T> HierarchicalMenu(
    expanded: MutableState<Boolean>,
    menu: Menu<T>,
    target: T
) {
    DropdownMenu(
        modifier = Modifier.testTag(TEST_TAG_CONTEXT_MENU),
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        EntryListRenderer(expanded, menu, target)
    }
}

@Composable
private fun RowScope.EntryContent(entry: MenuEntry<*>, offset: Dp = 0.dp) {
    Spacer(modifier = Modifier.width(offset))
    entry.icon?.let {
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
private fun <T> EntryListRenderer(expanded: MutableState<Boolean>, menu: Menu<T>, target: T, offset: Dp = 0.dp) {
    menu.entries.forEach { entry ->
        entry.content.fold(ifLeft = { function ->
            DropdownMenuItem(
                onClick = {
                    expanded.value = false
                    function.invoke(target)
                }
            ) {
                EntryContent(entry, offset)
            }
        }, ifRight = { submenu ->
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
                EntryListRenderer(expanded = expanded, menu = submenu, target, offset = offset + 12.dp)
            }
        })
    }
}


@Preview
@Composable
fun Activity() {
    Navigation(
        title = "Main Title"
    ) { Text(text = "Main Content") }
}

@Preview
@Composable
fun EntryContent() {
    Column {
        DropdownMenuItem(onClick = {}) {
            EntryContent(GenericMenuEntry(icon = Icons.Filled.Edit, label = R.string.menu_edit) {})
        }
        DropdownMenuItem(onClick = {}) {
            EntryContent(GenericMenuEntry(icon = myiconpack.ArrowsAlt, label = R.string.menu_move) {})
        }
    }
}

@Preview
@Composable
fun Overflow() {
    fun emptyEntry(label: Int) = GenericMenuEntry(label = label) {}
    OverFlowMenu(
        menu = Menu(
            entries = listOf(
                emptyEntry(R.string.menu_learn_more),
                MenuEntry(
                    label = R.string.menu_hide, subMenu = Menu(
                        entries = listOf(
                            MenuEntry(icon = Icons.Filled.Edit, label = R.string.menu_edit) {},
                            MenuEntry(icon = myiconpack.ArrowsAlt, label = R.string.menu_move) {}
                        )
                    )
                )
            )
        ),
        target = Unit
    )
}

