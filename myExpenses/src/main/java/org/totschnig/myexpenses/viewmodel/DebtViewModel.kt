package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
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

    fun loadTransactions(debt: Debt, initialDebt: Long): LiveData<List<Transaction>> =
        liveData {
            var runningTotal = initialDebt
            val homeCurrency = Utils.getHomeCurrency().code
            val amountColumn = if (debt.currency == homeCurrency) {
                "CASE WHEN $KEY_CURRENCY = '$homeCurrency' THEN $KEY_AMOUNT ELSE ${getAmountHomeEquivalent(VIEW_EXTENDED)} END"
            } else {
                KEY_AMOUNT
            }
            contentResolver.observeQuery(
                uri = EXTENDED_URI,
                projection = arrayOf(KEY_ROWID, KEY_DATE, amountColumn),
                selection = "$KEY_DEBT_ID = ?",
                selectionArgs = arrayOf(debt.id.toString()),
                sortOrder = "$KEY_DATE ASC"
            ).mapToList {
                val amount = it.getLong(2)
                runningTotal -= amount
                Transaction(it.getLong(0), epoch2LocalDate(it.getLong(1)), -amount, runningTotal)
            }.collect(this::emit)
        }

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
        val amount: Long?,
        val runningTotal: Long
    )
}