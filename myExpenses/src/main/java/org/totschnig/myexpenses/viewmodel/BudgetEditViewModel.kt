package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetEditViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    val accounts = MutableLiveData<List<Account>>()
    val databaseResult = MutableLiveData<Boolean>()
    private val asyncInsertHandler: DatabaseHandler

    init {
        asyncInsertHandler = DatabaseHandler(application.contentResolver)
    }

    fun loadAccounts() {
        disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_MINIMAL_URI, null, null, null, null, false)
                .mapToList { cursor -> Account(cursor.getLong(0), cursor.getString(1), cursor.getString(2)) }
                .subscribe { accounts.postValue(it) }
    }

    fun createBudget(budget: Budget) {
        asyncInsertHandler.startInsert(TOKEN, object: DatabaseHandler.InsertListener {
            override fun onInsertComplete(token: Int, success: Boolean) {
                databaseResult.postValue(success)
            }
        }, TransactionProvider.BUDGETS_URI,
                budget.toContentValues())
    }

    companion object {
        internal val TOKEN = 0
    }
}

data class Account(val id: Long, val label: String, val currency: String) {
    override fun toString(): String {
        return label
    }
}