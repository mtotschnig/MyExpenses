package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.serialization.Serializable

@Serializable
sealed class ComplexCriterion : Criterion {
    abstract val criteria: Set<Criterion>
    abstract val operator: String
    abstract val symbol: Char
    abstract val description: Int
    override fun getSelectionForParts(
        tableName: String
    ) = criteria.joinToString(" $operator ", prefix = "(", postfix = ")") {
        it.getSelectionForParts(tableName)
    }

    override fun getSelectionForParents(
        tableName: String,
        forExport: Boolean,
    ) = criteria.joinToString(" $operator ", prefix = "(", postfix = ")") {
        it.getSelectionForParents(tableName, forExport)
    }

    override fun getSelectionArgs(queryParts: Boolean) =
        criteria.flatMap { it.getSelectionArgs(queryParts).toList() }.toTypedArray()

    override fun prettyPrint(context: Context) = criteria.joinToString {
        it.prettyPrint(context)
    }
}