package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Budget
import javax.inject.Inject

class BudgetViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    val data = MutableLiveData<List<Budget>>()
    @Inject
    lateinit var currencyContext: CurrencyContext
    private val PROJECTION = arrayOf(KEY_ROWID, KEY_ACCOUNTID, KEY_TITLE, KEY_DESCRIPTION,
            "coalesce(%1\$s, (SELECT %1\$s from %2\$s WHERE %2\$s.%3\$s = %4\$s )) "
                    .format(KEY_CURRENCY, TABLE_ACCOUNTS, KEY_ROWID, KEY_ACCOUNTID),
            KEY_BUDGET, KEY_GROUPING)

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    fun loadBudgets(accountId: Long, currencyStr: String) {
        val selection = (if (accountId > 0) KEY_ACCOUNTID else KEY_CURRENCY) + " = ?"
        val selectionArgs = arrayOf(if (accountId > 0) accountId.toString() else currencyStr)
        doLoad(selection, selectionArgs)
    }

    fun loadAllBudgets() {
        doLoad(null, null)
    }

    private fun doLoad(selection: String?, selectionArgs: Array<String>?) {
        disposable = briteContentResolver.createQuery(TransactionProvider.BUDGETS_URI,
                PROJECTION, selection, selectionArgs, null, true)
                .mapToList { cursor ->
                    val currency = cursor.getString(4)
                    Budget(
                            cursor.getLong(0),
                            cursor.getLong(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            currency,
                            Money(currencyContext.get(currency), cursor.getLong(5)),
                            Grouping.valueOf(cursor.getString(6))
                    )
                }
                .subscribe { data.postValue(it) }
    }
}
