package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import androidx.lifecycle.LiveData
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
    val budgetAmount = MutableLiveData<Money>()
    val updateComplete = MutableLiveData<Event<Pair<Int, Int>>>()
    var accuntDisposable: Disposable? = null
    private val asyncDatabaseHandler: DatabaseHandler

    init {
        asyncDatabaseHandler = DatabaseHandler(application.contentResolver)
    }

    private val updateListener = object : DatabaseHandler.UpdateListener {
        override fun onUpdateComplete(token: Int, resultCount: Int) {
            updateComplete.value = Event(Pair(token, resultCount))
        }
    }

    private val accountLiveData: Map<Long, LiveData<Account>> = lazyMap { accountId ->
        val liveData = MutableLiveData<Account>()
        accuntDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
        val base = if (accountId > 0) TransactionProvider.ACCOUNTS_URI else TransactionProvider.ACCOUNTS_AGGREGATE_URI
        accuntDisposable = briteContentResolver.createQuery(ContentUris.withAppendedId(base, accountId),
                Account.PROJECTION_BASE, null, null, null, true)
                .mapToOne { Account.fromCursor(it) }
                .subscribe {
                    liveData.postValue(it)
                    loadBudget(it)
                }
        return@lazyMap liveData
    }

    fun account(accountId: Long): LiveData<Account> = accountLiveData.getValue(accountId)

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

    fun remap(transactionIds: LongArray, column: String, rowId: Long) {
        var selection = "%s %s".format(DatabaseConstants.KEY_ROWID, WhereFilter.Operation.IN.getOp(transactionIds.size))
        var selectionArgs = transactionIds.map(Long::toString).toTypedArray()
        if (column.equals(DatabaseConstants.KEY_ACCOUNTID)) {
            selection += " OR %s %s".format(DatabaseConstants.KEY_PARENTID, WhereFilter.Operation.IN.getOp(transactionIds.size))
            selectionArgs = arrayOf(*selectionArgs, *selectionArgs)
        }
        asyncDatabaseHandler.startUpdate(TOKEN_REMAP_CATEGORY,
                updateListener,
                TransactionProvider.TRANSACTIONS_URI,
                ContentValues().apply { put(column, rowId) },
                selection,
                selectionArgs)
    }

    companion object {
        const val TOKEN_REMAP_CATEGORY = 1

        fun <K, V> lazyMap(initializer: (K) -> V): Map<K, V> {
            val map = mutableMapOf<K, V>()
            return map.withDefault { key ->
                val newValue = initializer(key)
                map[key] = newValue
                return@withDefault newValue
            }
        }
    }
}

