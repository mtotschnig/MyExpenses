package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import android.os.Bundle
import org.jetbrains.annotations.NotNull
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.AccountCriteria

class SelectMultipleAccountDialogFragment : SelectFilterDialog(false) {
    override fun makeCriteria(label: String, vararg ids: Long) = AccountCriteria(label, *ids)

    override fun getUri(): Uri {
        return TransactionProvider.ACCOUNTS_BASE_URI
    }

    override fun getSelection() = if (arguments == null) null else "$KEY_CURRENCY = ?"

    override fun getSelectionArgs() = arguments?.let { arrayOf(it.getString(KEY_CURRENCY)!!) }

    override fun getColumn() = KEY_LABEL

    companion object {
        @JvmStatic
        fun newInstance(currencyCode: String): @NotNull SelectMultipleAccountDialogFragment = SelectMultipleAccountDialogFragment().apply {
            if (currencyCode != AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE) {
                arguments = Bundle(1).apply {
                    putString(KEY_CURRENCY, currencyCode)
                }
            }
        }
    }
}