package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("not")
data class NotCriterion(val criterion: Criterion): Criterion {
    override fun getSelectionForParts(tableName: String?) =
        addIsNullCheck("NOT(${criterion.getSelectionForParts(tableName)})")

    override fun getSelectionForParents(
        tableName: String?,
        forExport: Boolean,
    ) = addIsNullCheck("NOT(${criterion.getSelectionForParents(tableName, forExport)})")

    private fun addIsNullCheck(selection: String) =
        if (criterion is SimpleCriterion<*> && criterion.isNullable && !criterion.isNull)
            "($selection OR ${criterion.column} IS NULL)" else selection

    override fun prettyPrint(context: Context) = "¬(${criterion.prettyPrint(context)})"

    override fun getSelectionArgs(queryParts: Boolean) = criterion.getSelectionArgs(queryParts)
}