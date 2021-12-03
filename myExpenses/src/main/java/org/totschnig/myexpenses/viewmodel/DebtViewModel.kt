package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Transaction.EXTENDED_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.getAmountHomeEquivalent
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.time.LocalDate
import kotlin.math.absoluteValue
import kotlin.math.sign

class DebtViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    fun saveDebt(debt: Debt): LiveData<Unit> = liveData(context = coroutineContext()) {
        emit(repository.saveDebt(debt))
    }

    fun loadDebt(debtId: Long): LiveData<Debt> = liveData {
        contentResolver.observeQuery(
            singleDebtUri(debtId),
            null,
            null,
            null,
            null
        ).mapToOne {
            Debt.fromCursor(it)
        }.collect(this::emit)
    }

    private fun singleDebtUri(debtId: Long) =
        ContentUris.withAppendedId(TransactionProvider.DEBTS_URI, debtId)

/*    fun loadDebugTransactions(count: Int = 10): LiveData<List<Transaction>> = liveData {
        emit(
            List(count) {
                Transaction(it.toLong(), LocalDate.now(), 4000L - it, 4000L - it * it, -1)
            }
        )
    }*/

    private val transactionsLiveData: Map<Debt, LiveData<List<Transaction>>> = lazyMap { debt ->
        val liveData = MutableLiveData<List<Transaction>>()
        var runningTotal = debt.amount
        val homeCurrency = Utils.getHomeCurrency().code
        val amountColumn = if (debt.currency == homeCurrency) {
            "CASE WHEN $KEY_CURRENCY = '$homeCurrency' THEN $KEY_AMOUNT ELSE ${
                getAmountHomeEquivalent(
                    VIEW_EXTENDED
                )
            } END"
        } else {
            KEY_AMOUNT
        }
        viewModelScope.launch {
            contentResolver.observeQuery(
                uri = EXTENDED_URI,
                projection = arrayOf(KEY_ROWID, KEY_DATE, amountColumn),
                selection = "$KEY_DEBT_ID = ?",
                selectionArgs = arrayOf(debt.id.toString()),
                sortOrder = "$KEY_DATE ASC"
            ).mapToList {
                val amount = it.getLong(2)
                val previousBalance = runningTotal
                runningTotal -= amount
                val trend =
                    if (previousBalance.sign * runningTotal.sign == -1)
                        0
                    else
                        runningTotal.absoluteValue.compareTo(previousBalance.absoluteValue)
                Transaction(
                    it.getLong(0),
                    epoch2LocalDate(it.getLong(1)),
                    -amount,
                    runningTotal,
                    trend
                )
            }.collect { liveData.postValue(it) }
        }
        return@lazyMap liveData
    }

    fun loadTransactions(debt: Debt): LiveData<List<Transaction>> =
        transactionsLiveData.getValue(debt)

    fun deleteDebt(debtId: Long): LiveData<Boolean> =
        liveData(context = coroutineContext()) {
            emit(contentResolver.delete(singleDebtUri(debtId), null, null) == 1)
        }

    fun closeDebt(debtId: Long): LiveData<Boolean> =
        liveData(context = coroutineContext()) {
            emit(updateSealed(debtId, 1) == 1)
        }

    fun reopenDebt(debtId: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            updateSealed(debtId, 0)
        }
    }

    /**
     * @param isSealed 1 == Sealed, 0 == Open
     */
    private fun updateSealed(debtId: Long, isSealed: Int) = contentResolver.update(
        ContentUris.withAppendedId(TransactionProvider.DEBTS_URI, debtId),
        ContentValues(1).apply {
            put(KEY_SEALED, isSealed)
        }, null, null
    )

    data class Transaction(
        val id: Long,
        val date: LocalDate,
        val amount: Long,
        val runningTotal: Long,
        val trend: Int = 0
    )
}