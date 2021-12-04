package org.totschnig.myexpenses.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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

@SuppressLint("PrivateResource")
@Composable
fun OverFlowMenu(
    content: List<Pair<String, () -> Unit>>
) {
    var showMenu by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showMenu = true }) {
        Icon(
            painterResource(id = R.drawable.abc_ic_menu_overflow_material),
            stringResource(id = R.string.abc_action_menu_overflow_description)
        )
    }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        content.forEach {
            DropdownMenuItem(
                onClick = {
                    showMenu = false
                    it.second.invoke()
                }) {
                Text(it.first)
            }
        }
    }
}

@Preview
@Composable
fun Activity() {
    Navigation(
        title = { Text(text = "Mein Title")},
        content = { Text(text = "Mein Content")}
    )
}