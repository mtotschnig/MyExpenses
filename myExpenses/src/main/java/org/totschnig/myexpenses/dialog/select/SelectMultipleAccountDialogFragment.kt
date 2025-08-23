package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.AGGREGATE_HOME_CURRENCY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION

class SelectMultipleAccountDialogFragment :
    SelectFilterDialog<AccountCriterion>(false, AccountCriterion::class.java) {
    override fun makeCriteria(label: String, vararg ids: Long) = AccountCriterion(label, *ids)
    override val uri: Uri = TransactionProvider.ACCOUNTS_BASE_URI
    override val column: String = KEY_LABEL

    override val dialogTitle: Int
        get() = R.string.search_account
    override val selection: String?
        get() = if (currencyFromArguments == null) null else "$KEY_CURRENCY = ?"
    override val selectionArgs: Array<String>?
        get() = currencyFromArguments?.let { arrayOf(it) }
    private val currencyFromArguments: String?
        get() = requireArguments().getString(KEY_CURRENCY)

    companion object {
        fun newInstance(
            requestKey: String,
            currencyCode: String?,
            criterion: AccountCriterion?
        ) =
            SelectMultipleAccountDialogFragment().apply {
                arguments = configureArguments(requestKey).apply {
                    currencyCode?.let {
                        putString(KEY_CURRENCY, it)
                    }
                    putParcelable(KEY_CRITERION, criterion)
                }
            }
    }
}