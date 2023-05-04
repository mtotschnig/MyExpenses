package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.viewmodel.data.Transaction2

class TransactionListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun loadTransactions(): Flow<List<Transaction2>> = flow {
        emit(listOf(Transaction2(
                id = -1,
        _date = System.currentTimeMillis() / 1000,
        amount = Money(homeCurrencyProvider.homeCurrencyUnit, -7000),
        methodLabel = "CHEQUE",
        referenceNumber = "1",
        accountId = -1,
        catId = 1,
        label = getString(R.string.testData_transaction2Comment),
        comment = getString(R.string.testData_transaction4Payee),
        icon = "cart-shopping",
        year = 0,
        month = 0,
        day = 0,
        week = 0,
        tagList = listOf(getString(R.string.testData_tag_project)),
        pictureUri = Uri.EMPTY
        )))
    }

}

