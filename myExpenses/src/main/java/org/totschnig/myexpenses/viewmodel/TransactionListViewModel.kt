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

    fun calculateGroupId(year: Int, second: Int) = year * 1000 + second

    companion object {
        fun prefNameForCriteria(accountId: Long) = "filter_%s_${accountId}"
    }
}

