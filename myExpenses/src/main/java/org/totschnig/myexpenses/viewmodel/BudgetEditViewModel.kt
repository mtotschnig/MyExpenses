package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IAccount
import org.totschnig.myexpenses.model.Account.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetEditViewModel(application: Application) : BudgetViewModel(application) {
    val accounts = MutableLiveData<List<Account>>()
    private val databaseHandler: DatabaseHandler = DatabaseHandler(application.contentResolver)

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

    fun saveBudget(budget: Budget, whereFilter: WhereFilter) {
        val contentValues = budget.toContentValues()
        if (budget.id == 0L) {
            databaseHandler.startInsert(TOKEN, object : DatabaseHandler.InsertListener {
                override fun onInsertComplete(token: Int, uri: Uri?) {
                    val result = uri?.let { ContentUris.parseId(it) } ?: -1
                    if (result > -1) persistPreferences(result, whereFilter, budget)
                    databaseResult.postValue(result)
                }
            }, TransactionProvider.BUDGETS_URI, contentValues)
        } else {
            databaseHandler.startUpdate(TOKEN, object : DatabaseHandler.UpdateListener {
                override fun onUpdateComplete(token: Int, resultCount: Int) {
                    val result = if (resultCount == 1) budget.id else -1
                    if (result > -1) persistPreferences(result, whereFilter, budget)
                    databaseResult.postValue(result)
                }
            }, ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budget.id),
                    contentValues, null, null)
        }
    }

    fun persistPreferences(budgetId: Long, whereFilter: WhereFilter, budget: Budget) {
        val filterPersistence = FilterPersistence(prefHandler, prefNameForCriteria(budgetId), null, false, false)
        whereFilter.criteria.forEach { filterPersistence.addCriteria(it) }
        filterPersistence.persistAll()
        if (budget.grouping != Grouping.NONE && whereFilter.isEmpty) {
            val prefKey = prefNameForDefaultBudget(budget.accountId, budget.grouping)
            if (budget.default) {
                prefHandler.putLong(prefKey, budget.id)
            } else {
                if(prefHandler.getLong(prefKey, 0) == budget.id) {
                    prefHandler.remove(prefKey)
                }
            }
        }
    }

    companion object {
        internal const val TOKEN = 0
    }
}

data class Account(override val id: Long, val label: String, val currency: String): IAccount {
    override fun toString(): String {
        return label
    }
}