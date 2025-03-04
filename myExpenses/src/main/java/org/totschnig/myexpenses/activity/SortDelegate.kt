package org.totschnig.myexpenses.activity

import android.view.Menu
import android.view.MenuItem
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.enumValueOrNull

class SortDelegate(
    val defaultSortOrder: Sort,
    val prefKey: PrefKey,
    val options: Array<Sort>,
    val prefHandler: PrefHandler,
    val collate: String
) {
    fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.SORT_COMMAND)?.subMenu?.findItem(currentSortOrder.commandId)?.isChecked =
            true
    }

    fun onOptionsItemSelected(item: MenuItem) = Sort.fromCommandId(item.itemId)
        ?.takeIf { options.contains(it) }
        ?.let {
            if (!item.isChecked) {
                prefHandler.putString(prefKey, it.name)
            }
            true
        } == true

    val currentSortOrder: Sort
        get() = enumValueOrNull<Sort>(
            prefHandler.requireString(
                prefKey,
                defaultSortOrder.name
            )
        )?.takeIf {
            options.contains(it)
        } ?: defaultSortOrder
}