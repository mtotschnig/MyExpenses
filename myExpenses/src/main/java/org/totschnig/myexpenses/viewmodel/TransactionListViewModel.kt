package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import androidx.lifecycle.MutableLiveData
import io.reactivex.disposables.Disposable
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Event

class TransactionListViewModel(application: Application) : BudgetViewModel(application) {
    val account = MutableLiveData<Account>()
    val budgetAmount = MutableLiveData<Money>()
    val updateComplete = MutableLiveData<Event<Pair<Int, Int>>>()
    var accuntDisposable: Disposable? = null
    private val asyncDatabaseHandler: DatabaseHandler

    init {
        asyncDatabaseHandler = DatabaseHandler(application.contentResolver)
    }

    private val updateListener = object : DatabaseHandler.UpdateListener {
        override fun onUpdateComplete(token: Int, result: Int) {
            updateComplete.value = Event(Pair(token, result))
        }
    }

    fun loadAccount(accountId: Long) {
        accuntDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
        val base = if (accountId > 0) TransactionProvider.ACCOUNTS_URI else TransactionProvider.ACCOUNTS_AGGREGATE_URI
        accuntDisposable = briteContentResolver.createQuery(ContentUris.withAppendedId(base, accountId),
                Account.PROJECTION_BASE, null, null, null, true)
                .mapToOne { Account.fromCursor(it) }
                .subscribe {
                    account.postValue(it)
                    loadBudget(it)
                }
    }

    fun loadBudget(account: Account) {
        val budgetId = getDefault(account.id, account.grouping)
        if (budgetId != 0L) {
            loadBudget(budgetId, true)
        } else {
            budgetAmount.postValue(null)
        }
    }

    override fun postBudget(budget: Budget) {
        budgetAmount.postValue(budget.amount)
    }

    fun remapCategory(transactionIds: LongArray, catId: Long) {
        asyncDatabaseHandler.startUpdate(TOKEN_REMAP_CATEGORY,
                updateListener,
                TransactionProvider.TRANSACTIONS_URI,
                ContentValues().apply { put(DatabaseConstants.KEY_CATID, catId) },
                "%s %s".format(DatabaseConstants.KEY_ROWID, WhereFilter.Operation.IN.getOp(transactionIds.size)),
                transactionIds.map(Long::toString).toTypedArray())
    }

    companion object {
        const val TOKEN_REMAP_CATEGORY = 1
    }
}

