package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.serialization.Serializable
import kotlin.collections.toTypedArray

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
    ) = criteria.joinToString(" AND ") {
        it.getSelectionForParents(tableName, forExport)
    }

    override fun prettyPrint(context: Context) = criteria.joinToString {
        it.prettyPrint(context)
    }

    override val selectionArgs: Array<String>
        get() = criteria.flatMap { it.selectionArgs.toList() }.toTypedArray()
    override val id: Int
        get() = TODO("Not yet implemented")
    override val column: String
        get() = TODO("Not yet implemented")
    override val title: Int
        get() = TODO("Not yet implemented")
    override val key: String
        get() = TODO("Not yet implemented")
}