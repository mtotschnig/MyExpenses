package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.data.Budget

class TransactionListViewModel(application: Application) : BudgetViewModel(application) {
    val budgetAmount = MutableLiveData<Money>()
    private var accuntDisposable: Disposable? = null
    private var cloneAndRemapProgressInternal = MutableLiveData<Pair<Int, Int>>()

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

    val cloneAndRemapProgress: LiveData<Pair<Int, Int>>
        get() = cloneAndRemapProgressInternal

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

    fun cloneAndRemap(transactionIds: LongArray, column: String, rowId: Long)  {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var successCount = 0
                var failureCount = 0
                for (id in transactionIds) {
                    val transaction = Transaction.getInstanceFromDb(id)
                    transaction.prepareForEdit(true, false)
                    val ops = transaction.buildSaveOperations(true)
                    val newUpdate = ContentProviderOperation.newUpdate(TRANSACTIONS_URI).withValue(column, rowId)
                    if (transaction.isSplit) {
                        newUpdate.withSelection(KEY_ROWID + " = ?", arrayOf(transaction.id.toString()))
                    } else {
                        newUpdate.withSelection(KEY_ROWID + " = ?", arrayOf(""))//replaced by back reference
                                .withSelectionBackReference(0, 0)
                    }
                    ops.add(newUpdate.build())
                    if (getApplication<MyApplication>().contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).size == ops.size) {
                        failureCount++
                    } else {
                        failureCount++
                    }
                    cloneAndRemapProgressInternal.postValue(Pair(successCount, failureCount))
                }
            }
        }
    }

    fun remap(transactionIds: LongArray, column: String, rowId: Long): LiveData<Int> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(run {
            var selection = "%s %s".format(KEY_ROWID, WhereFilter.Operation.IN.getOp(transactionIds.size))
            var selectionArgs = transactionIds.map(Long::toString).toTypedArray()
            if (column.equals(DatabaseConstants.KEY_ACCOUNTID)) {
                selection += " OR %s %s".format(DatabaseConstants.KEY_PARENTID, WhereFilter.Operation.IN.getOp(transactionIds.size))
                selectionArgs = arrayOf(*selectionArgs, *selectionArgs)
            }
            getApplication<MyApplication>().contentResolver.update(TRANSACTIONS_URI,
                    ContentValues().apply { put(column, rowId) },
                    selection,
                    selectionArgs)
        })
    }
}

