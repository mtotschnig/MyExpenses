package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.collect
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject

class DebtViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    @Inject
    lateinit var currencyContext: CurrencyContext

    fun saveDebt(debt: Debt): LiveData<Unit> = liveData(context = coroutineContext()) {
        emit(repository.saveDebt(debt))
    }

    fun loadDebt(debtId: Long): LiveData<Debt> = liveData(context = coroutineContext()) {
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.DEBTS_URI, debtId),
            null,
            null,
            null,
            null
        )?.use {
            if (it.moveToFirst()) {
                emit(Debt.fromCursor(it))
            }
        }

    }

    fun loadTransactions(debtId: Long, initialDebt: Long): LiveData<List<Transaction>> =
        liveData {
            var runningTotal = initialDebt
            contentResolver.observeQuery(
                TransactionProvider.TRANSACTIONS_URI,
                arrayOf(KEY_ROWID, KEY_DATE, "-$KEY_AMOUNT"),
                "$KEY_DEBT_ID = ?",
                arrayOf(debtId.toString())
            ).mapToList {
                val amount = it.getLong(2)
                runningTotal += amount
                Transaction(it.getLong(0), LocalDate.now(), amount, runningTotal)
            }.collect { list -> emit(list) }
        }

    data class Transaction(
        val id: Long,
        val date: LocalDate,
        val amount: Long?,
        val runningTotal: Long
    )
}