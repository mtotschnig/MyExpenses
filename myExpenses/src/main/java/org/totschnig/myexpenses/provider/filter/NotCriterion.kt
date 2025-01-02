package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("not")
data class NotCriterion(val criterion: BaseCriterion): BaseCriterion {
    override fun getSelectionForParts(tableName: String): String {
        TODO("Not yet implemented")
    }

    override fun getSelectionForParents(
        tableName: String,
        forExport: Boolean,
    ) = "NOT(${criterion.getSelectionForParents(tableName, forExport)})"

    override fun prettyPrint(context: Context) = "!(${criterion.prettyPrint(context)})"

    override val shouldApplyToSplitTransactions: Boolean
        get() = criterion.shouldApplyToSplitTransactions
    override val shouldApplyToArchive: Boolean
        get() = criterion.shouldApplyToArchive
    override val selectionArgs: Array<String>
        get() = criterion.selectionArgs
    override val id: Int
        get() = TODO("Not yet implemented")
    override val column: String
        get() = TODO("Not yet implemented")
    override val title: Int
        get() = TODO("Not yet implemented")
    override val key: String
        get() = TODO("Not yet implemented")
}