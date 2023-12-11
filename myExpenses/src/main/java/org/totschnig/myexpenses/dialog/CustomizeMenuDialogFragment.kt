package org.totschnig.myexpenses.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R

data class MenuItem(val key: String, @StringRes val label: Int)

class CustomizeMenuDialogFragment : ComposeBaseDialogFragment() {
    @Composable
    override fun BuildContent() {
        val activeItems: SnapshotStateList<MenuItem> = remember {
            mutableStateListOf(
                MenuItem("templates", R.string.menu_templates),
                MenuItem("print", R.string.menu_print),
                MenuItem("reset", R.string.menu_export)
            )
        }
        val inactiveItems = remember {
            mutableStateListOf<MenuItem>()
        }
        Column {
            Text(
                modifier = Modifier.padding(top = dialogPadding, start = dialogPadding),
                style = MaterialTheme.typography.titleLarge,
                text = stringResource(id = R.string.customize)
            )
            LazyColumn(modifier = Modifier.padding(dialogPadding)) {
                itemsIndexed(activeItems) { index, item ->
                    ItemRow(checked = true,
                        label = item.label,
                        onCheckedChange = {
                            activeItems.remove(item)
                            inactiveItems.add(item)
                        }
                    )
                }
                if (activeItems.isNotEmpty() && inactiveItems.isNotEmpty()) {
                    item {
                        Divider(thickness = 1.dp)
                    }
                }
                items(inactiveItems) { item ->
                    ItemRow(checked = false, label = item.label, onCheckedChange = {
                        activeItems.add(item)
                        inactiveItems.remove(item)
                    })
                }
            }
        }
    }

    @Composable
    private fun ItemRow(
        checked: Boolean,
        label: Int,
        onCheckedChange: ((Boolean) -> Unit),
        onUp: (() -> Unit)? = null,
        onDown: (() -> Unit)? = null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(stringResource(id = label))
            Spacer(modifier = Modifier.weight(1f))
            onUp?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Localized description")
                }
            }
            onDown?.let {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
    }
}