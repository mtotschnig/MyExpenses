package org.totschnig.myexpenses.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
data class MenuEntry(val label: String, val content: Either<() -> Unit, Menu>)

@SuppressLint("PrivateResource")
@Composable
fun OverFlowMenu(
    menu: Menu
) {
    val showMenu = remember { mutableStateOf(false) }
    IconButton(
        onClick = { showMenu.value = true }) {
        Icon(
            painterResource(id = R.drawable.abc_ic_menu_overflow_material),
            stringResource(id = R.string.abc_action_menu_overflow_description)
        )
    }
    HierarchicalMenu(expanded = showMenu, menu = menu)
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
            DropdownMenuItem(
                enabled = false,
                onClick = {}
            ) {
                Text(modifier = Modifier.alpha(0.5f), text = entry.label)
            }
            EntryListRenderer(expanded = expanded, menu = submenu, offset = offset + 5.dp)
        })
    }
}


@Preview
@Composable
fun Activity() {
    Navigation(
        title = { Text(text = "Mein Title") },
        content = { Text(text = "Mein Content") }
    )
}