package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.livefront.sealedenum.GenSealedEnum
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import timber.log.Timber
import java.util.Collections

sealed class MenuItem(
    @IdRes val id: Int,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    data object Templates : MenuItem(
        R.id.MANAGE_TEMPLATES_COMMAND,
        R.string.menu_templates,
        R.drawable.ic_menu_template
    )

    data object Budget: MenuItem(
        R.id.BUDGET_COMMAND,
        R.string.menu_budget,
        R.drawable.ic_budget
    )

    data object Print: MenuItem(
        R.id.PRINT_COMMAND,
        R.string.menu_print,
        R.drawable.ic_menu_print
    )

    data object Settings: MenuItem(
        R.id.SETTINGS_COMMAND,
        R.string.settings_label,
        R.drawable.ic_lock
    )

    @GenSealedEnum
    companion object
}

class CustomizeMenuDialogFragment : ComposeBaseDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefHandler.getStringSet(PrefKey.CUSTOMIZE_MAIN_MENU)?.forEach {
            Timber.i(it)
        }
    }
    @Composable
    override fun BuildContent() {
        val activeItems = remember { prefHandler.mainMenu.toMutableStateList() }
        val inactiveItems = remember { (MenuItem.values - activeItems).toMutableStateList() }

        Column {
            Text(
                modifier = Modifier.padding(top = dialogPadding, start = dialogPadding),
                style = MaterialTheme.typography.titleLarge,
                text = TextUtils.concatResStrings(
                    LocalContext.current,
                    " : ",
                    R.string.menu,
                    R.string.customize
                )
            )
            LazyColumn(modifier = Modifier.padding(dialogPadding)) {
                itemsIndexed(activeItems) { index, item ->
                    ItemRow(checked = true,
                        label = item.label,
                        onCheckedChange = if (item != MenuItem.Settings) {
                            {
                                activeItems.remove(item)
                                inactiveItems.add(item)
                            }
                        } else null,
                        onUp = if (index == 0) null else {
                            { Collections.swap(activeItems, index - 1, index) }
                        },
                        onDown = if (index < activeItems.lastIndex) {
                            { Collections.swap(activeItems, index, index + 1) }
                        } else null
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
            ButtonRow(
                modifier = Modifier.padding(
                    bottom = dialogPadding,
                    start = dialogPadding,
                    end = dialogPadding
                )
            ) {
                TextButton(onClick = { dismiss() }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
                TextButton(onClick = {
                    inactiveItems.clear()
                    activeItems.clear()
                    activeItems.addAll(MenuItem.values)
                }) {
                    Text(stringResource(id = R.string.menu_reset_plan_instance))
                }
                TextButton(onClick = {
                    prefHandler.putStringSet(PrefKey.CUSTOMIZE_MAIN_MENU, LinkedHashSet(activeItems.map { it.name }))
                    dismiss()
                }) {
                    Text(stringResource(id = R.string.confirm))
                }
            }
        }
    }

    @Composable
    private fun ItemRow(
        checked: Boolean,
        label: Int,
        onCheckedChange: (() -> Unit)?,
        onUp: (() -> Unit)? = null,
        onDown: (() -> Unit)? = null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, enabled = onCheckedChange != null, onCheckedChange = {
                onCheckedChange?.invoke()
            })
            Text(stringResource(id = label))
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.width(96.dp), horizontalArrangement = Arrangement.Center) {
                onUp?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Localized description"
                        )
                    }
                }
                onDown?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }

        }
    }
}