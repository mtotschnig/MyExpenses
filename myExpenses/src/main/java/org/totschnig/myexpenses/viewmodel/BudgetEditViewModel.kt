package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.saveBudget
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.FilterPersistenceV2
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.asSimpleList
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetEditViewModel(application: Application) : BudgetViewModel(application) {
    /**
     * provides id of budget on success, -1 on error
     */
    val databaseResult = MutableLiveData<Long>()

    val criteria: MutableSet<SimpleCriterion<*>> = mutableSetOf()

    fun budget(budgetId: Long) = liveData(context = coroutineContext()) {
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId),
            null, null, null, null
        )?.use {
            if (it.moveToFirst()) emit(repository.budgetCreatorFunction(it))
        }
    }

    fun saveBudget(budget: Budget, initialAmount: Long?) {
        viewModelScope.launch(coroutineContext()) {
            val result = repository.saveBudget(budget, initialAmount)
            if (result > -1) persistPreferences(result)
            databaseResult.postValue(result)
        }
    }

    fun initWith(budgetId: Long) {
        criteria.addAll(
            FilterPersistenceV2(
                prefHandler,
                prefNameForCriteriaV2(budgetId)
            ).whereFilter.asSimpleList
        )
    }

    fun persistPreferences(budgetId: Long) {
        FilterPersistenceV2(
            prefHandler,
            prefNameForCriteriaV2(budgetId),
            restoreFromPreferences = false
        ).whereFilter = AndCriterion(criteria)
    }
}

