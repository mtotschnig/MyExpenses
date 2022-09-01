package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class BudgetAllocation(val budget: Long, val rollOverPrevious: Long, val rollOverNext: Long, val oneTime: Boolean) :
    Parcelable {
    @IgnoredOnParcel
    val totalAllocated = budget + rollOverPrevious

    companion object {
        val EMPTY = BudgetAllocation(0, 0, 0, false)
    }
}