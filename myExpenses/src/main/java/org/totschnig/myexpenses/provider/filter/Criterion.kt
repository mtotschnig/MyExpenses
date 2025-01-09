package org.totschnig.myexpenses.provider.filter

import android.content.Context
import android.os.Parcelable
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.provider.CTE_SEARCH

@Serializable
sealed interface Criterion : Parcelable {
    fun getSelectionForParts(tableName: String = CTE_SEARCH): String
    fun getSelectionForParents(tableName: String = CTE_SEARCH, forExport: Boolean = false): String
    fun prettyPrint(context: Context): String
    fun getSelectionArgs(queryParts: Boolean) : Array<String>
}