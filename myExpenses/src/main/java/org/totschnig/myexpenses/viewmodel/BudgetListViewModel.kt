package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.budgetAllocationQueryUri
import org.totschnig.myexpenses.db2.findAccountByUuid
import org.totschnig.myexpenses.db2.importBudget
import org.totschnig.myexpenses.db2.sumLoaderForBudget
import org.totschnig.myexpenses.model2.BudgetExport
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.asSimpleList
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2.Companion.aggregateNeutralPrefKey
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.getLabelForBudgetType
import timber.log.Timber

class BudgetListViewModel(application: Application) : BudgetViewModel(application) {
    private val groupingSortKey = stringPreferencesKey("budgetListGrouping")

    enum class Grouping(val commandId: Int) {
        Account(R.id.GROUPING_BUDGETS_ACCOUNT_COMMAND), Grouping(R.id.GROUPING_BUDGETS_GROUPING_COMMAND)
    }

    sealed interface ImportInfo {
        val uuid: String
    }

    data class Importable(
        override val uuid: String,
        val budget: BudgetExport,
        val accountId: Long,
    ) : ImportInfo

    data class NotImportable(override val uuid: String, val title: String) : ImportInfo

    private val _importInfo = MutableStateFlow<Pair<String, List<ImportInfo>>?>(null)
    val importInfo: StateFlow<Pair<String, List<ImportInfo>>?> = _importInfo.asStateFlow()

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
                        .sortedWith(
                            compareBy(
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


    fun budgetCriteria(budget: Budget): Flow<List<SimpleCriterion<*>>> = FilterPersistence(
        dataStore,
        prefNameForCriteria(budget.id),
        viewModelScope
    ).whereFilter.map { it.asSimpleList }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun budgetAmounts(budget: Budget): Flow<Pair<Long, Long>> =
        combine(budgetCriteria(budget), dataStore.data) { _, data -> data }
            .map {
                it[aggregateNeutralPrefKey(budget.id)] == true
            }.flatMapLatest { aggregateNeutral ->
                Timber.d("(Re)Loading, %d", budget.id)
                val (sumUri, sumSelection, sumSelectionArguments) = repository.sumLoaderForBudget(
                    budget, aggregateNeutral, null
                )

                val allocationUri = budgetAllocationQueryUri(
                    budget.id,
                    if (budget.grouping == org.totschnig.myexpenses.model.Grouping.NONE) null else THIS_YEAR,
                    budget.grouping.queryArgumentForThisSecond
                )

                combine(
                    contentResolver.observeQuery(
                        sumUri,
                        arrayOf(KEY_SUM_EXPENSES), sumSelection, sumSelectionArguments, null, true
                    )
                        .mapToOne { it.getLong(KEY_SUM_EXPENSES) },
                    contentResolver.observeQuery(allocationUri, projection = arrayOf(KEY_BUDGET))
                        .mapToOne(0) {
                            it.getLong(0)
                        }
                ) { spent, allocated ->
                    Timber.d("Emitting, %d", budget.id)
                    spent to allocated
                }
            }

    val allBudgets
        get() = dataGrouped.value.second.flatMap { it.second }

    fun importBudgets(accountName: String) {
        viewModelScope.launch(context = coroutineContext()) {
            GenericAccountService.getSyncBackendProvider(localizedContext, accountName)
                .mapCatching { backend ->
                    val allBudgets = allBudgets
                    val budgets = backend.budgets
                    accountName to
                            budgets.filter { remote ->
                                allBudgets.none { local -> local.uuid == remote.first }
                            }.map { (uuid, budget) ->
                                budget.accountUuid?.let {
                                    repository.findAccountByUuid(it)?.let {
                                        Importable(uuid, budget, it)
                                    } ?: NotImportable(uuid, budget.title)
                                } ?: Importable(uuid, budget, 0L)
                            }
                }.onSuccess { result ->
                    _importInfo.update { result }
                }.onFailure {
                    CrashHandler.report(it)
                }
        }
    }

    fun importDialogDismissed() {
        _importInfo.update {
            null
        }
    }

    fun importBudgetsDo(uuids: List<String>) {
        importInfo.value?.let { (accountName, list) ->
            val budgetsToImport = list
                .filterIsInstance<Importable>()
                .filter { uuids.contains(it.uuid) }
            viewModelScope.launch(context = coroutineContext()) {
                budgetsToImport.forEach {
                    val budgetId = repository.importBudget(it.budget, 0, it.accountId, it.uuid)
                    setBudgetSynced(budgetId, it.accountId, prefHandler, accountName)
                }
            }
        }
        importDialogDismissed()
    }
}
