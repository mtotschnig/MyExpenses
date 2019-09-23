package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.provider.TransactionProvider

class TransactionListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    val account = MutableLiveData<Account>()

    fun loadAccount(accountId: Long) {
        dispose()
        val base = if (accountId > 0) TransactionProvider.ACCOUNTS_URI else TransactionProvider.ACCOUNTS_AGGREGATE_URI
        disposable = briteContentResolver.createQuery(ContentUris.withAppendedId(base, accountId),
                Account.PROJECTION_EXTENDED, null, null, null, true)
                .mapToOne { Account.fromCursor(it) }
                .subscribe { account.postValue(it) }
    }
}
