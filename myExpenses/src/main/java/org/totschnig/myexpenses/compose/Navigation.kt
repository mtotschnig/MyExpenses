package org.totschnig.myexpenses.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    title: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = colorResource(id = R.color.toolbarBackground),
            ) {
                Row {
                    IconButton(onClick = onNavigation) {
                        Icon(
                            painterResource(id = R.drawable.ic_menu_back),
                            contentDescription = stringResource(R.string.abc_action_bar_up_description)
                        )
                    }
                }
                title.invoke()
            }
        },
        content = content
    )
}

data class Menu(val entries: List<MenuEntry>)
data class MenuEntry(val label: String, val content: Either<() -> Unit, Menu>) {
    constructor(label: String, action: () -> Unit) : this(label, Either.Left(action))
    constructor(label: String, subMenu: Menu) : this(label, Either.Right(subMenu))

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
                stringResource(id = R.string.abc_action_menu_overflow_description)
            )
        }
        HierarchicalMenu(expanded = showMenu, menu = menu)
    }
}

/**
 * the submenus are rendered in a flattened list with indentation.
 * We tried to render proper submenus, but were not able to get the position of the submenu right
 */
@Composable
fun HierarchicalMenu(
    expanded: MutableState<Boolean>,
    menu: Menu
) {
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        EntryListRenderer(expanded, menu)
    }
}

@Composable
private fun EntryListRenderer(expanded: MutableState<Boolean>, menu: Menu, offset: Dp = 0.dp) {
    menu.entries.forEach { entry ->
        entry.content.fold(ifLeft = { function ->
            DropdownMenuItem(
                onClick = {
                    expanded.value = false
                    function.invoke()
                }
            ) {
                Text(modifier = Modifier.padding(start = offset), text = entry.label)
            }
        }, ifRight = { submenu ->
            var subMenuVisible by remember { mutableStateOf(false) }
            DropdownMenuItem(
                onClick = { subMenuVisible = !subMenuVisible }
            ) {
                Text(
                    modifier = Modifier
                        .padding(end = 5.dp), text = entry.label
                )
                Icon(
                    if (subMenuVisible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    stringResource(
                        if (subMenuVisible) R.string.content_description_collapse
                        else R.string.content_description_expand
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
            if (subMenuVisible) {
                EntryListRenderer(expanded = expanded, menu = submenu, offset = offset + 10.dp)
            }
        })
    }
}


@Preview
@Composable
fun Activity() {
    Navigation(
        title = { Text(text = "Main Title") },
        content = { Text(text = "Main Content") }
    )
}

@Preview
@Composable
fun Overflow() {
    fun emptyEntry(label: String) = MenuEntry(label, Either.Left {})
    OverFlowMenu(
        menu = Menu(
            entries = listOf(
                emptyEntry("Option 1"),
                MenuEntry(
                    "Option 2", Menu(
                            entries = listOf(
                                emptyEntry("Option 2.1"),
                                emptyEntry("Option 2.2")
                        )
                    )
                )
            )
        )
    )
}

