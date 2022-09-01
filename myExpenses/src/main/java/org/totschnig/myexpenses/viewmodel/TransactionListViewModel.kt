package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.saveTagLinks
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Tag

class TransactionListViewModel(application: Application) : BudgetViewModel(application) {
    private val budgetAmountInternal = MutableLiveData<List<Triple<Int, Long, Boolean>>>()
    val budgetAmount: LiveData<List<Triple<Int, Long, Boolean>>>
        get() = budgetAmountInternal

    private val cloneAndRemapProgressInternal = MutableLiveData<Pair<Int, Int>>()
    val cloneAndRemapProgress: LiveData<Pair<Int, Int>>
        get() = cloneAndRemapProgressInternal

    private fun loadBudgetAmounts(account: Account) {
        val budgetId = getDefaultBudget(account.id, account.grouping)
        if (budgetId != 0L) {
            viewModelScope.launch {
                contentResolver.observeQuery(
                    uri = ContentUris.withAppendedId(
                        ContentUris.withAppendedId(
                            TransactionProvider.BUDGETS_URI,
                            budgetId
                        ),
                        0
                    ),
                    projection = arrayOf(
                        KEY_YEAR,
                        KEY_SECOND_GROUP,
                        KEY_BUDGET,
                        KEY_BUDGET_ROLLOVER_PREVIOUS,
                        KEY_ONE_TIME
                    ),
                    sortOrder = "$KEY_YEAR, $KEY_SECOND_GROUP"
                )
                    .mapToList {
                        Triple(
                            calculateGroupId(it.getInt(0), it.getInt(1)),
                            it.getLong(2) + it.getLong(3),
                            it.getInt(4) == 1
                        )
                    }
                    .collect {
                        budgetAmountInternal.postValue(it)
                    }
            }
        }
    }

    override fun onAccountLoaded(account: Account) {
        if (licenceHandler.hasTrialAccessTo(ContribFeature.BUDGET)) {
            loadBudgetAmounts(account)
        }
    }

    fun cloneAndRemap(transactionIds: LongArray, column: String, rowId: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            var successCount = 0
            var failureCount = 0
            for (id in transactionIds) {
                val transaction = Transaction.getInstanceFromDb(id)
                transaction.prepareForEdit(true, false)
                val ops = transaction.buildSaveOperations(true)
                val newUpdate =
                    ContentProviderOperation.newUpdate(TRANSACTIONS_URI).withValue(column, rowId)
                if (transaction.isSplit) {
                    newUpdate.withSelection("$KEY_ROWID = ?", arrayOf(transaction.id.toString()))
                } else {
                    newUpdate.withSelection(
                        "$KEY_ROWID = ?",
                        arrayOf("")
                    )//replaced by back reference
                        .withSelectionBackReference(0, 0)
                }
                ops.add(newUpdate.build())
                if (contentResolver.applyBatch(
                        TransactionProvider.AUTHORITY,
                        ops
                    ).size == ops.size
                ) {
                    successCount++
                } else {
                    failureCount++
                }
                cloneAndRemapProgressInternal.postValue(Pair(successCount, failureCount))
            }
        }
    }

    fun remap(transactionIds: LongArray, column: String, rowId: Long): LiveData<Int> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(run {
                val list = transactionIds.joinToString()
                var selection = "$KEY_ROWID IN ($list)"
                if (column == KEY_ACCOUNTID) {
                    selection += " OR $KEY_PARENTID IN ($list)"
                }
                contentResolver.update(
                    TRANSACTIONS_URI,
                    ContentValues().apply { put(column, rowId) },
                    selection,
                    null
                )
            })
        }

    fun tag(transactionIds: LongArray, tagList: ArrayList<Tag>, replace: Boolean) {
        val tagIds = tagList.map { tag -> tag.id }
        viewModelScope.launch(coroutineDispatcher) {
            val ops = ArrayList<ContentProviderOperation>()
            for (id in transactionIds) {
                ops.addAll(saveTagLinks(tagIds, id, null, replace))
            }
            contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
        }
    }

    fun undeleteTransactions(itemIds: LongArray): LiveData<Int> =
        liveData(context = coroutineContext()) {
            emit(itemIds.sumBy {
                try {
                    Transaction.undelete(it)
                    1
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
                    0
                }
            })
        }

    fun toggleCrStatus(id: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            contentResolver.update(
                TRANSACTIONS_URI
                    .buildUpon()
                    .appendPath(id.toString())
                    .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
                    .build(),
                null, null, null
            )
        }
    }

    fun calculateGroupId(year: Int, second: Int) = year * 1000 + second

    companion object {
        fun prefNameForCriteria(accountId: Long) = "filter_%s_${accountId}"
    }
}

