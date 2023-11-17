package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import arrow.core.Tuple4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo

class DistributionViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    DistributionViewModelBase<DistributionAccountInfo>(application, savedStateHandle) {

    private val showTotalPrefKey = booleanPreferencesKey("distributionShowTotal")

    val showTotal
        get() = dataStore.data.map { preferences ->
            preferences[showTotalPrefKey] ?: false
        }

    suspend fun persistShowTotal(showAll: Boolean) {
        dataStore.edit { preference ->
            preference[showTotalPrefKey] = showAll
        }
    }

    private fun getGroupingPrefKey(accountId: Long) =
        stringPreferencesKey("distributionGrouping_$accountId")

    fun initWithAccount(accountId: Long, defaultGrouping: Grouping) {
        val isAggregate = accountId < 0
        val base =
            if (isAggregate) TransactionProvider.ACCOUNTS_AGGREGATE_URI else TransactionProvider.ACCOUNTS_URI
        val projection = if (isAggregate) arrayOf(KEY_LABEL, KEY_CURRENCY) else arrayOf(
            KEY_LABEL,
            KEY_CURRENCY,
            KEY_COLOR
        )
        viewModelScope.launch(coroutineContext()) {
            contentResolver.query(
                ContentUris.withAppendedId(base, accountId),
                projection, null, null, null
            )?.use {
                it.moveToFirst()
                _accountInfo.tryEmit(object : DistributionAccountInfo {
                    val label = it.getString(0)
                    override val accountId = accountId
                    override fun label(context: Context) = label
                    override val currencyUnit = currencyContext.get(it.getString(1))
                    override val color = if (isAggregate) -1 else it.getInt(2)
                })
            }
        }
        viewModelScope.launch {
            dataStore.data.map {
                enumValueOrDefault(it[getGroupingPrefKey(accountId)], defaultGrouping)
            }.collect {
                setGrouping(it)
            }
        }
    }

    fun persistGrouping(grouping: Grouping) {
        accountInfo.value?.let {
            viewModelScope.launch {
                dataStore.edit { preference ->
                    preference[getGroupingPrefKey(it.accountId)] = grouping.name
                }
            }
        }
    }

    private val incomeTypePrefKey = booleanPreferencesKey("distributionType")
    override val aggregateNeutralPrefKey = booleanPreferencesKey("distributionAggregateNeutral")

    val incomeType: Flow<Boolean> by lazy {
        dataStore.data.map { preferences ->
            preferences[incomeTypePrefKey] ?: false
        }
    }

    suspend fun persistIncomeType(incomeType: Boolean) {
        dataStore.edit { preference ->
            preference[incomeTypePrefKey] = incomeType
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTreeForDistribution by lazy {
        combine(
            _accountInfo.filterNotNull(),
            incomeType,
            aggregateNeutral,
            groupingInfoFlow.filterNotNull()
        ) { accountInfo, incomeType, aggregateNeutral, grouping ->
            Tuple4(accountInfo, incomeType, aggregateNeutral, grouping)
        }.flatMapLatest { (accountInfo, incomeType, aggregateNeutral, grouping) ->
            categoryTreeWithSum(
                accountInfo = accountInfo,
                incomeType = incomeType,
                aggregateNeutral = aggregateNeutral,
                groupingInfo = grouping,
                keepCriteria = { it.sum != 0L }
            )
        }.map { it.sortChildrenBySumRecursive() }
    }
}
