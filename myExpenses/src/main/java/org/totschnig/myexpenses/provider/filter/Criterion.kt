package org.totschnig.myexpenses.provider.filter

import android.content.Context
import android.os.Parcelable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.CTE_SEARCH

@Serializable
sealed interface Criterion : Parcelable {
    fun getSelectionForParts(tableName: String = CTE_SEARCH): String
    fun getSelectionForParents(tableName: String = CTE_SEARCH, forExport: Boolean = false): String
    fun prettyPrint(context: Context): String
    fun getSelectionArgs(queryParts: Boolean): Array<String>

    val displayTitle: Int
        get() = when (this) {
            is SimpleCriterion<*> -> displayInfo.title
            is NotCriterion -> criterion.displayTitle
            else -> throw NotImplementedError("Nested complex not supported")
        }

    val displayIcon: ImageVector
        get() = when (this) {
            is SimpleCriterion<*> -> displayInfo.icon
            is NotCriterion -> criterion.displayIcon
            else -> throw NotImplementedError("Nested complex not supported")
        }

    val displaySymbol: Pair<Char, Int>
        get() = when (this) {
            is SimpleCriterion<*> -> if (displayInfo.isPartial)
                '∋' to R.string.contains
            else
                '=' to R.string.filter_is

            is NotCriterion -> if ((criterion as? SimpleCriterion<*>)?.displayInfo?.isPartial == true)
                '∌' to R.string.does_not_contain
            else
                '≠' to R.string.filter_is_not

            else -> throw NotImplementedError("Nested complex not supported")
        }

    fun contentDescription(context: Context): String {
        val title = context.getString(displayTitle)
        val symbolDescription = context.getString(displaySymbol.second)
        val prettyPrint = ((this as? NotCriterion)?.criterion ?: this).prettyPrint(context)
        return "$title $symbolDescription $prettyPrint"
    }
}

val Criterion?.asSet: Set<Criterion>
    get() = (this as? ComplexCriterion)?.criteria
        ?: this?.let { setOf(it) }
        ?: emptySet()

val Criterion?.asSimpleList: List<SimpleCriterion<*>>
    get() = asSet.filterIsInstance(SimpleCriterion::class.java)