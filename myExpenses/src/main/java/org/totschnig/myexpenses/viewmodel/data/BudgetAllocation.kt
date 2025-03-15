package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import java.io.Serializable

@Immutable
@Parcelize
data class BudgetAllocation(
    val budget: Long,
    val rollOverPrevious: Long,
    val rollOverNext: Long,
    val oneTime: Boolean,
) : Parcelable, Serializable {
    @IgnoredOnParcel
    val totalAllocated = budget + rollOverPrevious

    companion object {
        val EMPTY = BudgetAllocation(0, 0, 0, false)

        fun fromCursor(cursor: Cursor) = BudgetAllocation(
            budget = cursor.getLong(DatabaseConstants.KEY_BUDGET),
            rollOverPrevious = cursor.getLong(DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS),
            rollOverNext = cursor.getLong(DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT),
            oneTime = cursor.getInt(DatabaseConstants.KEY_ONE_TIME) != 0
        )
    }
}