package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.budgetAllocationQueryUri
import org.totschnig.myexpenses.db2.sumLoaderForBudget
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2.Companion.aggregateNeutralPrefKey
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.getLabelForBudgetType
import timber.log.Timber
import javax.inject.Inject

class BudgetListViewModel(application: Application) : BudgetViewModel(application) {
    private val groupingSortKey = stringPreferencesKey("budgetListGrouping")

    enum class Grouping(val commandId: Int) {
        Account(R.id.GROUPING_BUDGETS_ACCOUNT_COMMAND), Grouping(R.id.GROUPING_BUDGETS_GROUPING_COMMAND)
    }

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

    private fun budgetFilterFlow(budgetId: Long) = budgetFilterPreferenceFlow.mapNotNull {
        if (it == budgetId) Unit else null
    }.onStart {
        emit(Unit)
    }

    fun grouping() = dataStore.data.map { preferences ->
        enumValueOrDefault(preferences[groupingSortKey], Grouping.Account)
    }

    suspend fun setGrouping(grouping: Grouping) {
        dataStore.edit {
            it[groupingSortKey] = grouping.name
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val dataGrouped: StateFlow<Pair<Grouping, List<Pair<String, List<Budget>>>>> by lazy {
        dataStore.data.map { preferences ->
            enumValueOrDefault(preferences[groupingSortKey], Grouping.Account)
        }.flatMapLatest { grouping ->
            data.map {
                grouping to if (grouping == Grouping.Account)
                    it.groupBy { it.accountId }
                        .values
                        .sortedWith(compareBy(
                            { it.first().accountId < 0 },
                            { it.first().accountName }
                        ))
                        .map {
                            it.first().label(localizedContext) to it
                        }
                else
                    it.groupBy { it.grouping }
                        .toSortedMap()
                        .values
                        .map {
                            localizedContext.getString(it.first().grouping.getLabelForBudgetType()) to it
                        }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, Grouping.Account to emptyList())
    }


    fun budgetCriteria(budget: Budget): Flow<List<Criterion<*>>> = budgetFilterFlow(budget.id).map {
        FilterPersistence(
            prefHandler,
            prefNameForCriteria(budget.id),
            null,
            immediatePersist = false,
            restoreFromPreferences = true
        ).whereFilter.criteria
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun budgetAmounts(budget: Budget): Flow<Pair<Long, Long>> =
        combine(budgetFilterFlow(budget.id), dataStore.data) { _, data -> data }
            .map {
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
                        .mapToOne(0) {
                            it.getLong(KEY_BUDGET) + it.getLong(
                                KEY_BUDGET_ROLLOVER_PREVIOUS
                            )
                        }
                ) { spent, allocated ->
                    Timber.d("Emitting, %d", budget.id)
                    spent to allocated
                }
            }
}
