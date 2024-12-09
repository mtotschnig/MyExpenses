package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.saveBudget
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetEditViewModel(application: Application) : BudgetViewModel(application) {
     /**
     * provides id of budget on success, -1 on error
     */
    val databaseResult = MutableLiveData<Long>()

    fun budget(budgetId: Long) = liveData(context = coroutineContext()) {
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId),
            null, null, null, null
        )?.use {
            if (it.moveToFirst()) emit(repository.budgetCreatorFunction(it))
        }
    }

    fun saveBudget(budget: Budget, initialAmount: Long?, whereFilter: WhereFilter) {
        viewModelScope.launch(coroutineContext()) {
            val result = repository.saveBudget(budget, initialAmount)
            if (result > -1) persistPreferences(result, whereFilter)
            databaseResult.postValue(result)
        }
    }

    fun persistPreferences(budgetId: Long, whereFilter: WhereFilter) {
        val filterPersistence = FilterPersistence(prefHandler, prefNameForCriteria(budgetId), null,
                immediatePersist = false, restoreFromPreferences = false)
        whereFilter.criteria.forEach { filterPersistence.addCriterion(it) }
        filterPersistence.persistAll()
    }
}

