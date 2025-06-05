package org.totschnig.myexpenses.model

import org.totschnig.myexpenses.R

enum class SortDirection(val commandId: Int) {
    ASC(R.id.SORT_ASCENDING_COMMAND), DESC(R.id.SORT_DESCENDING_COMMAND);

    companion object {
        fun fromCommandId(id: Int) = if (id == R.id.SORT_ASCENDING_COMMAND) ASC else DESC
    }
}
