package org.totschnig.myexpenses.activity

import android.view.Menu
import android.view.MenuItem
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.enumValueOrDefault

class SortDelegate(
    val defaultSortOrder: Sort,
    val prefKey: PrefKey,
    val options: Array<Sort>,
    val prefHandler: PrefHandler
) {
    fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.SORT_COMMAND)?.let {
            val currentItem = it.subMenu.findItem(currentSortOrder.commandId)
            if (currentItem != null) {
                currentItem.isChecked = true
            }
        }
    }

    fun onOptionsItemSelected(item: MenuItem) = Sort.fromCommandId(item.itemId)?.let {
        if (!item.isChecked) {
            prefHandler.putString(prefKey, it.name)
        }
        true
    } ?: false

    val sortOrder: String
        get() = Sort.preferredOrderByRestricted(
            prefKey,
            prefHandler,
            defaultSortOrder,
            options
        )!!

    val currentSortOrder: Sort
        get() = enumValueOrDefault(
            prefHandler.getString(prefKey, null),
            defaultSortOrder
        )
}