package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN
import org.totschnig.myexpenses.provider.TransactionProvider

class MyExpensesViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val hasHiddenAccounts = MutableLiveData<Boolean>()

    fun getHasHiddenAccounts(): LiveData<Boolean> {
        return hasHiddenAccounts
    }

    fun loadHiddenAccountCount() {
        disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_URI,
                arrayOf("count(*)"), "$KEY_HIDDEN = 1", null, null, false)
                .mapToOne { cursor -> cursor.getInt(0) > 0 }
                .subscribe { hasHiddenAccounts.postValue(it) }
    }
}
