package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.MutableLiveData
import io.reactivex.disposables.Disposable
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Budget

class TransactionListViewModel(application: Application) : BudgetViewModel(application) {
    val account = MutableLiveData<Account>()
    val budgetAmount = MutableLiveData<Money>()
    var accuntDisposable: Disposable? = null

    fun loadAccount(accountId: Long) {
        accuntDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
        val base = if (accountId > 0) TransactionProvider.ACCOUNTS_URI else TransactionProvider.ACCOUNTS_AGGREGATE_URI
        accuntDisposable = briteContentResolver.createQuery(ContentUris.withAppendedId(base, accountId),
                Account.PROJECTION_BASE, null, null, null, true)
                .mapToOne { Account.fromCursor(it) }
                .subscribe {
                    account.postValue(it)
                    loadBudget(it)
                }
    }

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
}
