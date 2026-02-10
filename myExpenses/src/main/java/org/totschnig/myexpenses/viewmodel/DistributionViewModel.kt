package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import arrow.core.Tuple4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.Category

class DistributionViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    DistributionViewModelBase<Account>(application, savedStateHandle) {

    enum class SumLineBehaviour {
        WithoutTotal, PercentageTotal, PercentageExpense
    }

    private val sumLineBehaviourPrefKey = stringPreferencesKey("sumLineBehaviour")

    private val Preferences.sumLineBehaviour
        get() = enumValueOrDefault(this[sumLineBehaviourPrefKey], SumLineBehaviour.WithoutTotal)

    val sumLineBehaviour
        get() = dataStore.data.map { it.sumLineBehaviour }

    suspend fun cycleSumLineBehaviour() {
        dataStore.edit {
            it[sumLineBehaviourPrefKey] =
                SumLineBehaviour.entries[(it.sumLineBehaviour.ordinal + 1) % SumLineBehaviour.entries.size].name
        }
    }

    private fun getGroupingPrefKey(account: Account) =
        stringPreferencesKey(
            "distributionGrouping_" + when(account.accountGrouping) {
                AccountGrouping.NONE -> HOME_AGGREGATE_ID
                AccountGrouping.CURRENCY -> account.currency
                AccountGrouping.FLAG -> account.flagId
                AccountGrouping.TYPE -> account.typeId
                null -> account.id
            }
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun initWithAccount(
        extras: Bundle,
        defaultGrouping: Grouping,
        whereFilter: Criterion?
    ) {
        viewModelScope.launch(coroutineContext()) {
            account(extras).collect {
                _accountInfo.tryEmit(it)
            }
        }
        viewModelScope.launch {
            accountInfo
                .filterNotNull()
                .flatMapLatest { account ->
                    dataStore.data.map {
                        enumValueOrDefault(it[getGroupingPrefKey(account)], defaultGrouping)
                    }
                        .distinctUntilChanged()
                }.collect { setGrouping(it) }

            _whereFilter.update { whereFilter }
        }
    }

    fun persistGrouping(grouping: Grouping) {
        accountInfo.value?.let {
            viewModelScope.launch {
                dataStore.edit { preference ->
                    preference[getGroupingPrefKey(it)] = grouping.name
                }
            }
        }
    }

    private val incomeFlagPrefKey = booleanPreferencesKey(SHOW_INCOME_KEY)
    private val expenseFlagPrefKey = booleanPreferencesKey(SHOW_EXPENSE_KEY)
    override val aggregateNeutralPrefKey = booleanPreferencesKey("distributionAggregateNeutral")


    val typeFlags: Flow<Pair<Boolean, Boolean>> by lazy {
        dataStore.data.map { preferences: Preferences ->
            val showIncome = preferences[incomeFlagPrefKey]
                ?: savedStateHandle.get<Boolean>(SHOW_INCOME_KEY) ?: false
            val showExpense = if (!showIncome) true else preferences[expenseFlagPrefKey]
                ?: savedStateHandle.get<Boolean>(SHOW_EXPENSE_KEY) ?: true
            showIncome to showExpense
        }
    }

    /**
     * @param toggleIncome if true toggle income, if false toggle expense
     * makes sure that at least one type is active
     */
    suspend fun toggleTypeFlag(toggleIncome: Boolean) {
        val (showIncome, showExpense) = typeFlags.first()
        dataStore.edit { preference ->
            if (toggleIncome) {
                preference[incomeFlagPrefKey] = !showIncome
                if (!showExpense) {
                    preference[expenseFlagPrefKey] = true
                }
            } else {
                preference[expenseFlagPrefKey] = !showExpense
                if (!showIncome) {
                    preference[incomeFlagPrefKey] = true
                }
            }
        }
    }

    val shouldAggregateNeutral: Flow<Boolean> by lazy {
        combine(aggregateNeutral, typeFlags) { aggregateNeutral, typeFlags ->
            aggregateNeutral && !(typeFlags.first && typeFlags.second)
        }
    }


    val categoryTreeForExpenses by lazy { categoryFlow(false) }
    val categoryTreeForIncome by lazy { categoryFlow(true) }

    val combinedCategoryTree by lazy {
        combine(categoryTreeForIncome, categoryTreeForExpenses) { income, expense ->
            if (income == Category.LOADING || expense == Category.LOADING)
                Category.LOADING
            else
                Category(
                    children = listOf(income, expense)
                )
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun categoryFlow(isIncome: Boolean) = combine(
        _accountInfo.filterNotNull(),
        shouldAggregateNeutral,
        groupingInfoFlow.filterNotNull(),
        _whereFilter
    ) { accountInfo, aggregateNeutral, grouping, whereFilter ->
        Tuple4(accountInfo, aggregateNeutral, grouping, whereFilter)
    }.flatMapLatest { (accountInfo, aggregateNeutral, grouping, whereFilter) ->
        categoryTreeWithSum(
            accountInfo = accountInfo,
            isIncome = isIncome,
            aggregateNeutral = aggregateNeutral,
            groupingInfo = grouping,
            keepCriterion = { it.sum != 0L },
            whereFilter = whereFilter,
            idMapper = { if (isIncome) it else -it }
        )
    }.map { it.sortChildrenBySumRecursive() }


    fun clearFilter() {
        _whereFilter.update { null }
    }

    companion object {
        @VisibleForTesting
        const val SHOW_EXPENSE_KEY = "distributionShowExpense"

        @VisibleForTesting
        const val SHOW_INCOME_KEY = "distributionShowIncome"
    }
}
