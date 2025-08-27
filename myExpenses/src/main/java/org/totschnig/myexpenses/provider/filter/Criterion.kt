package org.totschnig.myexpenses.provider.filter

import android.content.Context
import android.os.Parcelable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.CTE_SEARCH

@Serializable
sealed interface Criterion : Parcelable {
    fun getSelectionForParts(tableName: String? = null): String
    fun getSelectionForParents(tableName: String? = null, forExport: Boolean = false): String
    fun prettyPrint(context: Context): String
    fun getSelectionArgs(queryParts: Boolean): Array<String>

    val isNullable
        get() = false

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
            is SimpleCriterion<*> -> if (displayInfo.isPartial && !isNull)
                '∋' to R.string.contains
            else
                '=' to R.string.filter_is

            is NotCriterion -> {
                val base = criterion as? SimpleCriterion<*>
                if (base?.displayInfo?.isPartial == true && !base.isNull)
                    '∌' to R.string.does_not_contain
                else
                    '≠' to R.string.filter_is_not
            }

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