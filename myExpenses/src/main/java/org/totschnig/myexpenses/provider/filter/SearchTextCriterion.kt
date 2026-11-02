package org.totschnig.myexpenses.provider.filter

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_CATID
import org.totschnig.myexpenses.provider.KEY_COMMENT
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PAYEEID
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.TABLE_PAYEES
import org.totschnig.myexpenses.util.Utils

/**
 * Finds transactions whose note, reference number, payee or category contains [searchString],
 * the free text search of the new UI. Payees and categories are also found by the name of the one
 * above them, e.g. "Groceries" finds "Groceries:Bakery".
 *
 * Only reads columns of the transactions table, so it works on the table as well as on the views.
 *
 * @param amount in minor units, also finds transactions of this amount, as expense or income,
 * when the text is a number
 */
@Parcelize
@Serializable
@SerialName("search")
data class SearchTextCriterion(
    val searchString: String,
    val amount: Long? = null,
) : SimpleCriterion<String>() {

    @IgnoredOnParcel
    override val id = R.id.FILTER_SEARCH_TEXT_COMMAND

    @IgnoredOnParcel
    override val column = KEY_COMMENT

    override val operation get() = Operation.LIKE

    override val values get() = listOf(searchString)

    override val displayInfo: DisplayInfo
        get() = SearchTextCriterion

    override fun getSelection(forExport: Boolean) = buildString {
        val like = Operation.LIKE.getOp(1)
        // IFNULL, so that excluding the search with NOT also keeps transactions without note or payee
        append("(IFNULL($KEY_COMMENT, '') $like OR IFNULL($KEY_REFERENCE_NUMBER, '') $like")
        append(" OR IFNULL($KEY_PAYEEID, 0) IN (SELECT $KEY_ROWID FROM $TABLE_PAYEES WHERE $KEY_PAYEE_NAME_NORMALIZED $like OR $KEY_SHORT_NAME $like")
        append(" OR $KEY_PARENTID IN (SELECT $KEY_ROWID FROM $TABLE_PAYEES WHERE $KEY_PAYEE_NAME_NORMALIZED $like))")
        append(" OR IFNULL($KEY_CATID, 0) IN (SELECT $KEY_ROWID FROM $TABLE_CATEGORIES WHERE $KEY_LABEL $like")
        append(" OR $KEY_PARENTID IN (SELECT $KEY_ROWID FROM $TABLE_CATEGORIES WHERE $KEY_LABEL $like))")
        if (amount != null) append(" OR $KEY_AMOUNT IN (?, ?)")
        append(")")
    }

    override val selectionArgs: Array<String>
        get() {
            val text = "%${Utils.escapeSqlLikeExpression(searchString.trim())}%"
            val normalized = "%${Utils.escapeSqlLikeExpression(Utils.normalize(searchString.trim()))}%"
            return listOfNotNull(
                text, text, normalized, text, normalized, text, text,
                amount?.toString(), amount?.let { (-it).toString() }
            ).toTypedArray()
        }

    override fun prettyPrint(context: Context) = "\u201C${searchString.trim()}\u201D"

    companion object : DisplayInfo {
        override val title = R.string.search_text
        override val extendedTitle = R.string.search_text_extended
        override val icon = Icons.Default.Search
        override val isPartial = true
        override val clazz = SearchTextCriterion::class
    }
}
