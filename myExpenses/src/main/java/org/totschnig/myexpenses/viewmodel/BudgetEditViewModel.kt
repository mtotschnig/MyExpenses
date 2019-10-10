package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Budget
import javax.inject.Inject

class BudgetEditViewModel(application: Application) : BudgetViewModel(application) {
    val accounts = MutableLiveData<List<Account>>()
    private val databaseHandler: DatabaseHandler = DatabaseHandler(application.contentResolver)
    @Inject
    lateinit var prefHandler: PrefHandler

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    fun loadData(budgetId: Long) {
        disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_MINIMAL_URI, null, null, null, null, false)
                .mapToList { cursor ->
                    val id = cursor.getLong(0)
                    Account(id, if (id == HOME_AGGREGATE_ID.toLong()) getApplication<MyApplication>().getString(R.string.grand_total) else cursor.getString(1), cursor.getString(2))
                }
                .subscribe {
                    accounts.postValue(it)
                    dispose()
                    if (budgetId != 0L) loadBudget(budgetId, true)
                }
    }

    fun saveBudget(budget: Budget) {
        val contentValues = budget.toContentValues()
        if (budget.id == 0L) {
            databaseHandler.startInsert(TOKEN, object : DatabaseHandler.InsertListener {
                override fun onInsertComplete(token: Int, success: Boolean) {
                    databaseResult.postValue(success)
                }
            }, TransactionProvider.BUDGETS_URI, contentValues)
        } else {
            databaseHandler.startUpdate(TOKEN, object : DatabaseHandler.UpdateListener {
                override fun onUpdateComplete(token: Int, result: Int) {
                    databaseResult.postValue(result == 1)
                }
            }, ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budget.id),
                    contentValues, null, null)
        }
    }

    companion object {
        internal const val TOKEN = 0
    }
}

data class Account(val id: Long, val label: String, val currency: String) {
    override fun toString(): String {
        return label
    }
}