package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.util.*
import javax.inject.Inject

open class BudgetViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    val data = MutableLiveData<List<Budget>>()
    @Inject
    lateinit var currencyContext: CurrencyContext
    private val databaseHandler: DatabaseHandler
    val budgetCreatorFunction: (Cursor) -> Budget = { cursor ->
        val currency = cursor.getString(cursor.getColumnIndex(KEY_CURRENCY))
        Budget(
                cursor.getLong(cursor.getColumnIndex(KEY_ROWID)),
                cursor.getLong(cursor.getColumnIndex(KEY_ACCOUNTID)),
                cursor.getString(cursor.getColumnIndex(KEY_TITLE)),
                cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)),
                currency,
                Money(currencyContext.get(currency), cursor.getLong(cursor.getColumnIndex(KEY_BUDGET))),
                Grouping.valueOf(cursor.getString(cursor.getColumnIndex(KEY_GROUPING)))
        )
    }

    init {
        databaseHandler = DatabaseHandler(application.contentResolver)
    }

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    fun loadAllBudgets() {
        doLoad(null, null)
    }

    private fun doLoad(selection: String?, selectionArgs: Array<String>?) {
        disposable = createQuery(selection, selectionArgs)
                .mapToList(budgetCreatorFunction)
                .subscribe { data.postValue(it) }
    }

    fun createQuery(selection: String?, selectionArgs: Array<String>?) =
            briteContentResolver.createQuery(TransactionProvider.BUDGETS_URI,
                    PROJECTION, selection, selectionArgs, null, true)

    fun deleteBudgets(budgetIds: List<Long>) {
        databaseHandler.startDelete(TOKEN, object: DatabaseHandler.DeleteListener {
            override fun onDeleteComplete(token: Int, result: Int) {
                if (result != budgetIds.size) {
                    CrashHandler.report(IllegalStateException("Budget delete failed %d/d".format(Locale.ROOT, result, budgetIds.size)))
                }
            }
        }, TransactionProvider.BUDGETS_URI, KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(budgetIds.size), budgetIds.map(Long::toString).toTypedArray())
    }

    fun updateBudget(budgetId: Long, categoryId: Long, amount: Money) {
        val contentValues = ContentValues(1)
        contentValues.put(KEY_BUDGET, amount.amountMinor)
        val budgetUri = ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId)
        databaseHandler.startUpdate(TOKEN, null,
                if (categoryId == 0L) budgetUri else ContentUris.withAppendedId(budgetUri, categoryId),
                contentValues, null, null)
    }

    companion object {
        private val TOKEN = 0
        private val PROJECTION = arrayOf(KEY_ROWID,
                "coalesce(%1\$s, -(select %2\$s from %3\$s where %4\$s = %5\$s), %6\$d) AS %1\$s"
                        .format(Locale.ROOT, KEY_ACCOUNTID, KEY_ROWID, TABLE_CURRENCIES, KEY_CODE,
                                KEY_CURRENCY, AggregateAccount.HOME_AGGREGATE_ID),
                KEY_TITLE, KEY_DESCRIPTION,
                "coalesce(%1\$s, (SELECT %1\$s from %2\$s WHERE %2\$s.%3\$s = %4\$s )) AS %1\$s"
                        .format(KEY_CURRENCY, TABLE_ACCOUNTS, KEY_ROWID, KEY_ACCOUNTID),
                KEY_BUDGET, KEY_GROUPING)
    }
}
