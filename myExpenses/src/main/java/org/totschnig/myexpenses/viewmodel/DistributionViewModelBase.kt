package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import arrow.core.Tuple4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.updateCategoryColor
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.TREE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.getLongIfExistsOr0
import org.totschnig.myexpenses.util.GroupingInfo
import org.totschnig.myexpenses.util.GroupingNavigator
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.DateInfoExtra
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo

private const val KEY_GROUPING_INFO = "groupingInfo"

abstract class DistributionViewModelBase<T : DistributionAccountInfo>(
    application: Application,
    savedStateHandle: SavedStateHandle
) :
    CategoryViewModel(application, savedStateHandle) {

    val selectionState: MutableState<Category?> = mutableStateOf(null)
    val expansionState: SnapshotStateList<Category> = SnapshotStateList()
    protected val _accountInfo = MutableStateFlow<T?>(null)
    val accountInfo: StateFlow<T?> = _accountInfo

    protected val _whereFilter: MutableStateFlow<Criterion?> = MutableStateFlow(null)
    val whereFilter: StateFlow<Criterion?> = _whereFilter

    val groupingInfoFlow: Flow<GroupingInfo?>
        get() = savedStateHandle.getLiveData<GroupingInfo?>(KEY_GROUPING_INFO, null).asFlow()

    val grouping: Grouping
        get() = groupingInfo?.grouping ?: Grouping.NONE


    var groupingInfo: GroupingInfo?
        get() = savedStateHandle.get<GroupingInfo>(KEY_GROUPING_INFO)
        set(value) {
            savedStateHandle[KEY_GROUPING_INFO] = value
        }

    fun setGrouping(grouping: Grouping) {
        if (grouping == Grouping.NONE) {
            groupingInfo = GroupingInfo(grouping)
        } else {
            viewModelScope.launch {
                groupingInfo = GroupingNavigator.current(grouping, dateInfo.first())
            }
        }
    }

    suspend fun nextGrouping() = groupingInfo?.let {
        GroupingNavigator.next(
            it
        ) { dateInfoExtra.filterNotNull().first() }
    }

    private suspend fun previousGrouping() = groupingInfo?.let {
        GroupingNavigator.previous(
            it
        ) { dateInfoExtra.filterNotNull().first() }
    }

    fun forward() {
        viewModelScope.launch {
            groupingInfo = nextGrouping()
        }
    }

    fun backward() {
        viewModelScope.launch {
            groupingInfo = previousGrouping()
        }
    }

    private val dateInfoExtra: StateFlow<DateInfoExtra?> =
        groupingInfoFlow.filterNotNull().map { grouping ->
            when (grouping.grouping) {
                Grouping.DAY, Grouping.WEEK -> withContext(Dispatchers.IO) {
                    DateInfoExtra.load(contentResolver, grouping)
                }

                Grouping.MONTH -> DateInfoExtra(11, null)
                else -> DateInfoExtra(0, null)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val displaySubTitle: Flow<String> = combine(
        groupingInfoFlow.filterNotNull(),
        dateInfo,
        dateInfoExtra
    ) { groupingInfo, dateInfo, dateInfoExtra ->
        if (groupingInfo.grouping == Grouping.NONE) {
            defaultDisplayTitle
        } else if (dateInfoExtra != null && (groupingInfo.grouping != Grouping.WEEK || dateInfoExtra.weekStart != null)) {
            groupingInfo.grouping.getDisplayTitle(
                localizedContext,
                groupingInfo.year,
                groupingInfo.second,
                dateInfo,
                dateInfoExtra.weekStart
            )
        } else null
    }.filterNotNull()

    open val defaultDisplayTitle: String?
        get() = getString(R.string.menu_aggregates)

    fun categoryTreeWithSum(
        accountInfo: T,
        isIncome: Boolean,
        aggregateNeutral: Boolean,
        groupingInfo: GroupingInfo,
        whereFilter: Criterion? = null,
        keepCriterion: ((Category) -> Boolean)? = null,
        queryParameter: Map<String, String> = emptyMap(),
        idMapper: (Long) -> Long = { it }
    ): Flow<Category> =
        categoryTree(
            selection = buildFilterClause(groupingInfo, whereFilter),
            projection = buildList {
                add("$TREE_CATEGORIES.*")
                add(KEY_SUM)
                if (accountInfo is Budget) {
                    add(KEY_BUDGET)
                    add(KEY_BUDGET_ROLLOVER_PREVIOUS)
                    add(KEY_BUDGET_ROLLOVER_NEXT)
                    add(KEY_ONE_TIME)
                }
            }.toTypedArray(),
            additionalSelectionArgs = whereFilter?.getSelectionArgs(true),
            queryParameter = queryParameter + buildMap {
                put(KEY_TYPE, isIncome.toString())
                put(
                    TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL,
                    aggregateNeutral.toString()
                )
                accountInfo.queryParameter?.let {
                    put(it.first, it.second)
                }
                if (accountInfo is Budget) {
                    put(DatabaseConstants.KEY_BUDGETID, accountInfo.id.toString())
                }
                if (groupingInfo.grouping != Grouping.NONE) {
                    put(DatabaseConstants.KEY_YEAR, groupingInfo.year.toString())
                    if (groupingInfo.grouping != Grouping.YEAR) {
                        put(DatabaseConstants.KEY_SECOND_GROUP, groupingInfo.second.toString())
                    }
                }
            },
            keepCriterion = keepCriterion,
            idMapper = idMapper
        ).mapNotNull {
            when (it) {
                is LoadingState.Empty -> Category.EMPTY
                is LoadingState.Data -> it.data
            }
        }

    val filterClause: String
        get() = buildFilterClause(groupingInfo!!, _whereFilter.value)

    private fun buildFilterClause(
        groupingInfo: GroupingInfo,
        whereFilter: Criterion?
    ): String {
        return listOfNotNull(
            dateFilterClause(groupingInfo),
            whereFilter?.getSelectionForParts()
        ).joinToString(" AND ")
    }

    open fun dateFilterClause(groupingInfo: GroupingInfo) = with(groupingInfo) {
        val yearExpression = "$YEAR = $year"
        when (grouping) {
            Grouping.YEAR -> yearExpression
            Grouping.DAY -> "$yearExpression AND $DAY = $second"
            Grouping.WEEK -> "${getYearOfWeekStart()} = $year AND ${getWeek()} = $second"
            Grouping.MONTH -> "${getYearOfMonthStart()} = $year AND ${getMonth()} = $second"
            else -> null
        }
    }

    fun updateColor(id: Long, color: Int) {
        viewModelScope.launch(context = coroutineContext()) {
            repository.updateCategoryColor(id, color)
        }
    }

    val aggregateNeutral: Flow<Boolean> by lazy {
        dataStore.data.map { preferences ->
            preferences[aggregateNeutralPrefKey] == true
        }
    }

    open val withIncomeSum = true

    private val sumProjection by lazy {
        if (withIncomeSum) arrayOf(KEY_SUM_EXPENSES, KEY_SUM_INCOME) else
            arrayOf(KEY_SUM_EXPENSES)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val sums: Flow<Pair<Long, Long>> by lazy {
        combine(
            _accountInfo.filterNotNull(),
            //if we ask for both expense and income, aggregateNeutral does not make sense, since
            //then transactions from neutral categories would appear on both sides, giving meaningless results
            if (withIncomeSum) flowOf(false) else aggregateNeutral,
            groupingInfoFlow,
            _whereFilter
        ) { accountInfo, aggregateNeutral, grouping, whereFilter ->
            grouping?.let {
                Tuple4(
                    accountInfo,
                    aggregateNeutral,
                    grouping,
                    whereFilter
                )
            }
        }
            .filterNotNull()
            .flatMapLatest { (accountInfo, aggregateNeutral, grouping, whereFilter) ->
                val builder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
                accountInfo.queryParameter?.let {
                    builder.appendQueryParameter(it.first, it.second)
                }
                builder.appendQueryParameter(
                    TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL,
                    aggregateNeutral.toString()
                )

                //if we have no income or expense, there is no row in the cursor
                contentResolver.observeQuery(
                    builder.build(),
                    sumProjection,
                    buildFilterClause(grouping, whereFilter),
                    whereFilter?.getSelectionArgs(true),
                    null, true
                ).mapToOne {
                    Pair(
                        it.getLongIfExistsOr0(KEY_SUM_INCOME),
                        it.getLongIfExistsOr0(KEY_SUM_EXPENSES)
                    )
                }
            }
    }

    abstract val aggregateNeutralPrefKey: Preferences.Key<Boolean>

    open suspend fun persistAggregateNeutral(aggregateNeutral: Boolean) {
        dataStore.edit { preference ->
            preference[aggregateNeutralPrefKey] = aggregateNeutral
        }
    }
}