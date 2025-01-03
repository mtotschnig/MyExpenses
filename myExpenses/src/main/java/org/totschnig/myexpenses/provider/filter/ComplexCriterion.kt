package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.serialization.Serializable

@Serializable
sealed class ComplexCriterion() : BaseCriterion {
    abstract val criteria: List<BaseCriterion>
    abstract val operator: String
    override fun getSelectionForParts(
        tableName: String
    ) = criteria.joinToString(" $operator ") {
        it.getSelectionForParts(tableName)
    }

    override fun getSelectionForParents(
        tableName: String,
        forExport: Boolean,
    ) = criteria.joinToString(" $operator ") {
        it.getSelectionForParents(tableName, forExport)
    }

    override fun getSelectionArgs(queryParts: Boolean) =
        criteria.flatMap { it.getSelectionArgs(queryParts).toList() }.toTypedArray()

    override fun prettyPrint(context: Context) = criteria.joinToString {
        it.prettyPrint(context)
    }
}