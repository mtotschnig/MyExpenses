package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.saveBudget
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.asSimpleList
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetEditViewModel(application: Application) : BudgetViewModel(application) {
    /**
     * provides id of budget on success, -1 on error
     */
    val databaseResult = MutableLiveData<Long>()

    val _criteria : MutableStateFlow<Set<SimpleCriterion<*>>> = MutableStateFlow(emptySet())
    val criteria:  StateFlow<Set<SimpleCriterion<*>>> = _criteria

    fun budget(budgetId: Long) = liveData(context = coroutineContext()) {
        contentResolver.query(
            BaseTransactionProvider.budgetUri(budgetId),
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
        viewModelScope.launch {
            FilterPersistence(
                dataStore,
                prefNameForCriteria(budgetId)
            ).getValue()?.asSimpleList?.let { criteria ->
                _criteria.update { criteria.toSet() }
            }
        }
    }

    fun persistPreferences(budgetId: Long) {
        viewModelScope.launch {
            FilterPersistence(
                dataStore,
                prefNameForCriteria(budgetId)
            ).persist(criteria.value)
        }
    }

    fun addFilterCriterion(criterion: SimpleCriterion<*>) {
        _criteria.update {
            it.filterNot { it::class == criterion::class }.toSet() + criterion
        }
    }

    fun removeFilter(id: Int) {
        _criteria.update {
            it.filterNot { it.id == id }.toSet()
        }
    }
}

