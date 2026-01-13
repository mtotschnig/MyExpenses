package org.totschnig.myexpenses.model.sort

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import org.totschnig.myexpenses.R

enum class SortDirection(@param:IdRes val commandId: Int, @param:StringRes val label: Int) {
    ASC(R.id.SORT_ASCENDING_COMMAND, R.string.sort_direction_ascending),
    DESC(R.id.SORT_DESCENDING_COMMAND, R.string.sort_direction_descending);

    companion object {
        fun fromCommandId(id: Int) = if (id == R.id.SORT_ASCENDING_COMMAND) ASC else DESC
    }
}