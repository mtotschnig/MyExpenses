package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
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
import javax.inject.Inject

class BudgetListViewModel(application: Application) : BudgetViewModel(application) {

    @Inject
    lateinit var settings: SharedPreferences

    val budgetFilterPreferenceFlow by lazy {
        callbackFlow<Long> {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key?.startsWith("budgetFilter") == true) {
                    try {
                        key.substringAfterLast('_').toLong()
                    } catch (_: Exception) {
                        null
                    }?.let { trySend(it) }
                }
            }
            settings.registerOnSharedPreferenceChangeListener(listener)
            awaitClose {
                settings.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.buffer(Channel.UNLIMITED).shareIn(viewModelScope, SharingStarted.Lazily)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun budgetAmounts(budget: Budget): Flow<Pair<Long, Long>> {
        return combine(
            budgetFilterPreferenceFlow.mapNotNull {
                if (it == budget.id) Unit else null
            }.onStart {
                emit(Unit)
            },
            dataStore.data
        ) { _, data -> data }.map {
            it[aggregateNeutralPrefKey(budget.id)] == true
        }.flatMapLatest { aggregateNeutral ->
            Timber.d("(Re)Loading, %d", budget.id)
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
            ) { spent, allocated ->
                Timber.d("Emitting, %d", budget.id)
                spent to allocated
            }
        }
    }
}
