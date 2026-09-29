package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.Money

@Parcelize
data class CategoryRef(
    val id: Long,
    val path: String
) : Parcelable

@Parcelize
data class CostLeg(
    val amount: Money,
    val category: CategoryRef? = null
) : Parcelable
