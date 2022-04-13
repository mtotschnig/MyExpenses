package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.*

abstract class BaseTransactionProvider : ContentProvider() {
    var dirty = false
        set(value) {
            if (!field && value) {
                (context?.applicationContext as? MyApplication)?.markDataDirty()
            }
            field = value
        }

    companion object {
        const val CURRENCIES_USAGES_TABLE_EXPRESSION =
            "$TABLE_CURRENCIES LEFT JOIN (SELECT coalesce($KEY_ORIGINAL_CURRENCY, $KEY_CURRENCY) AS currency_coalesced, count(*) AS $KEY_USAGES FROM $VIEW_EXTENDED GROUP BY currency_coalesced) on currency_coalesced = $KEY_CODE"

        val PAYEE_PROJECTION = arrayOf(
            KEY_ROWID,
            KEY_PAYEE_NAME,
            "exists (SELECT 1 FROM $TABLE_TRANSACTIONS WHERE $KEY_PAYEEID=$TABLE_PAYEES.$KEY_ROWID) AS $KEY_MAPPED_TRANSACTIONS",
            "exists (SELECT 1 FROM $TABLE_TEMPLATES WHERE $KEY_PAYEEID=$TABLE_PAYEES.$KEY_ROWID) AS $KEY_MAPPED_TEMPLATES",
            "(SELECT COUNT(*) FROM $TABLE_DEBTS WHERE $KEY_PAYEEID=$TABLE_PAYEES.$KEY_ROWID) AS $KEY_MAPPED_DEBTS"
        )
        const val DEBT_PAYEE_JOIN =
            "$TABLE_DEBTS LEFT JOIN $TABLE_PAYEES ON ($KEY_PAYEEID = $TABLE_PAYEES.$KEY_ROWID)"

        fun categoryBudgetJoin(joinType: String) = " $joinType JOIN $TABLE_BUDGET_CATEGORIES ON ($KEY_CATID = $TREE_CATEGORIES.$KEY_ROWID AND $TABLE_BUDGET_CATEGORIES.$KEY_BUDGETID = ?)"

        /**
         * @param transactionId When we edit a transaction, we want it to not be included into the debt sum, since it can be changed in the UI, and the variable amount will be calculated by the UI
         */
        fun debtProjection(transactionId: String?): Array<String> {
            val exclusionClause = transactionId?.let {
                "AND $KEY_ROWID != $it"
            } ?: ""
            return arrayOf(
                "$TABLE_DEBTS.$KEY_ROWID",
                KEY_PAYEEID,
                KEY_DATE,
                KEY_LABEL,
                KEY_AMOUNT,
                KEY_CURRENCY,
                KEY_DESCRIPTION,
                KEY_PAYEE_NAME,
                KEY_SEALED,
                "(select sum($KEY_AMOUNT) from $TABLE_TRANSACTIONS where $KEY_DEBT_ID = $TABLE_DEBTS.$KEY_ROWID $exclusionClause) AS $KEY_SUM"
            )
        }

        const val KEY_DEBT_LABEL = "debt"

        const val DEBT_LABEL_EXPRESSION = "(SELECT $KEY_LABEL FROM $TABLE_DEBTS WHERE $KEY_ROWID = $KEY_DEBT_ID) AS $KEY_DEBT_LABEL"
    }
}