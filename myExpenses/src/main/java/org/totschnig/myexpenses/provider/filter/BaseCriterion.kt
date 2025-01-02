package org.totschnig.myexpenses.provider.filter

import android.content.Context
import android.os.Parcelable
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.provider.CTE_SEARCH

@Serializable
sealed interface BaseCriterion : Parcelable {
    fun getSelectionForParts(tableName: String = CTE_SEARCH): String
    fun getSelectionForParents(tableName: String = CTE_SEARCH, forExport: Boolean = false): String
    fun prettyPrint(context: Context): String
    val shouldApplyToSplitTransactions: Boolean
        get() = true
    val shouldApplyToArchive: Boolean
        get() = true
    val selectionArgs: Array<String>
    val id: Int
    val column: String
    val title: Int
    val key: String

    fun getSelectionArgsList(queryParts: Boolean) = (
            when {
                queryParts || (shouldApplyToArchive xor shouldApplyToSplitTransactions) -> selectionArgs + selectionArgs
                shouldApplyToSplitTransactions && shouldApplyToArchive -> selectionArgs + selectionArgs + selectionArgs
                else -> selectionArgs
            })
        .asList()

    fun getSelectionArgs(queryParts: Boolean) = getSelectionArgsList(queryParts).toTypedArray()

    fun getSelectionArgsIfNotEmpty(queryParts: Boolean) = getSelectionArgs(queryParts)
        .takeIf { it.isNotEmpty() }
}