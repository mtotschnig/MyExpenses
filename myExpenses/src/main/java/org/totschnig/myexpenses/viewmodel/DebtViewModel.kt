package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject

class DebtViewModel(application: Application): ContentResolvingAndroidViewModel(application) {
    @Inject
    lateinit var currencyContext: CurrencyContext

    fun saveDebt(debt: Debt): LiveData<Unit> = liveData(context = coroutineContext()) {
        emit(repository.saveDebt(debt))
    }

    fun loadDebt(debtId: Long): LiveData<Debt> = liveData(context = coroutineContext()) {
        contentResolver.query(ContentUris.withAppendedId(TransactionProvider.DEBTS_URI, debtId), null, null, null, null)?.use {
            if (it.moveToFirst()) {
                emit(Debt.fromCursor(it))
            }
        }

    }
}