package org.totschnig.myexpenses.viewmodel

import android.app.Application
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_DEFAULT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGETS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Budget
import timber.log.Timber
import java.util.Locale

open class BudgetViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    val data: Flow<List<Budget>> by lazy {
        contentResolver.observeQuery(
            uri = TransactionProvider.BUDGETS_URI,
            projection = PROJECTION,
            notifyForDescendants = true,
            sortOrder = KEY_TITLE

        ).onEach {
            Timber.i("budget data received")
        }
            .mapToList(mapper = repository.budgetCreatorFunction)
    }

    companion object {
        val PROJECTION = arrayOf(
            q(KEY_ROWID),
            "coalesce($KEY_ACCOUNTID, -(select $KEY_ROWID from $TABLE_CURRENCIES where $KEY_CODE = $TABLE_BUDGETS.$KEY_CURRENCY), ${HOME_AGGREGATE_ID}) AS $KEY_ACCOUNTID",
            KEY_TITLE,
            q(KEY_DESCRIPTION),
            "coalesce($TABLE_BUDGETS.$KEY_CURRENCY, $TABLE_ACCOUNTS.$KEY_CURRENCY) AS $KEY_CURRENCY",
            q(KEY_GROUPING),
            KEY_COLOR,
            KEY_START,
            KEY_END,
            "$TABLE_ACCOUNTS.$KEY_LABEL AS $KEY_ACCOUNT_LABEL",
            KEY_IS_DEFAULT
        )

        fun q(column: String) = "$TABLE_BUDGETS.$column"

        fun prefNameForCriteria(budgetId: Long): String =
            "budgetFilter_%%s_%d".format(Locale.ROOT, budgetId)
    }
}
