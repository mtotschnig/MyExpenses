package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("not")
data class NotCriterion(val criterion: Criterion): Criterion {
    override fun getSelectionForParts(tableName: String) =
        "NOT(${criterion.getSelectionForParts(tableName)})"

    override fun getSelectionForParents(
        tableName: String,
        forExport: Boolean,
    ) = "NOT(${criterion.getSelectionForParents(tableName, forExport)})"

    override fun prettyPrint(context: Context) = "¬(${criterion.prettyPrint(context)})"

    override fun getSelectionArgs(queryParts: Boolean) = criterion.getSelectionArgs(queryParts)
}