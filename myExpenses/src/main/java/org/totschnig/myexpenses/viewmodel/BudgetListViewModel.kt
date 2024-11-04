package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.budgetAllocationQueryUri
import org.totschnig.myexpenses.db2.sumLoaderForBudget
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2.Companion.aggregateNeutralPrefKey
import org.totschnig.myexpenses.viewmodel.data.Budget
import timber.log.Timber

class BudgetListViewModel(application: Application) : BudgetViewModel(application) {

    data class BudgetViewItem(val budget: Budget, val budgetInfo: BudgetInfo?)
    data class BudgetInfo(val allocated: Long, val spent: Long)

    fun init() {
        viewModelScope.launch {
            data.collect {
                _enrichedData.value = it.map { BudgetViewItem(it, null) }
            }
        }
    }

    private val _enrichedData = MutableStateFlow<List<BudgetViewItem>>(emptyList())
    val enrichedData: StateFlow<List<BudgetViewItem>> = _enrichedData

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadBudgetAmounts(budget: Budget) {
        Timber.d("Debugg: Loading, %d", budget.id)
        viewModelScope.launch {
            dataStore.data.map {
                it[aggregateNeutralPrefKey(budget.id)] == true
            }.flatMapLatest { aggregateNeutral ->
                val (sumUri, sumSelection, sumSelectionArguments) = repository.sumLoaderForBudget(
                    budget, aggregateNeutral, null
                )

                val allocationUri = budgetAllocationQueryUri(
                    budget.id,
                    0,
                    budget.grouping,
                    THIS_YEAR,
                    budget.grouping.queryArgumentForThisSecond
                )

                combine(
                    contentResolver.observeQuery(
                        sumUri,
                        arrayOf(KEY_SUM_EXPENSES), sumSelection, sumSelectionArguments, null, true
                    )
                        .mapToOne { it.getLong(KEY_SUM_EXPENSES) },
                    contentResolver.observeQuery(allocationUri)
                        .mapToOne(0) { it.getLong(KEY_BUDGET) + it.getLong(KEY_BUDGET_ROLLOVER_PREVIOUS) }
                ) { spent, allocated -> Triple(budget.id, spent, allocated) }
            }.collect { (id, spent, allocated) ->
                _enrichedData.value = _enrichedData.value.map {
                    if (it.budget.id == id)
                        BudgetViewItem(it.budget, BudgetInfo(allocated, spent))
                    else it
                }
            }
        }
    }
}