package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Parcelable
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
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.updateCategoryColor
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.TREE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_WITH_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getLongIfExistsOr0
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.DateInfoExtra
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo
import java.util.Locale

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

    protected val _whereFilter: MutableStateFlow<WhereFilter> =
        MutableStateFlow(WhereFilter.empty())
    val whereFilter: StateFlow<WhereFilter> = _whereFilter

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
            groupingInfo = GroupingInfo(grouping, 0, 0)
        } else {
            viewModelScope.launch {
                groupingInfo = with(dateInfo.first()) {
                    GroupingInfo(
                        grouping = grouping,
                        year = when (grouping) {
                            Grouping.WEEK -> yearOfWeekStart
                            Grouping.MONTH -> yearOfMonthStart
                            else -> year
                        },
                        second = when (grouping) {
                            Grouping.DAY -> day
                            Grouping.WEEK -> week
                            Grouping.MONTH -> month
                            else -> 0
                        }
                    )
                }
            }
        }
    }

    fun GroupingInfo.next(dateInfo: DateInfoExtra): GroupingInfo {
        val nextSecond = second + 1
        val overflow = nextSecond > dateInfo.maxValue
        return copy(
            year = if (overflow) year + 1 else year,
            second = if (overflow) grouping.minValue else nextSecond
        )
    }

    fun GroupingInfo.previous(dateInfo: DateInfoExtra): GroupingInfo {
        val nextSecond = second - 1
        val underflow = nextSecond < grouping.minValue
        return copy(
            year = if (underflow) year - 1 else year,
            second = if (underflow) dateInfo.maxValue else nextSecond
        )
    }

    fun forward() {
        groupingInfo?.let { info ->
            if (info.grouping == Grouping.YEAR) {
                groupingInfo = info.copy(year = info.year + 1)
            } else {
                viewModelScope.launch {
                    val dateInfo = dateInfoExtra.filterNotNull().first()
                    groupingInfo = info.next(dateInfo)
                }
            }
        }
    }

    fun backward() {
        groupingInfo?.let { info ->
            if (info.grouping == Grouping.YEAR) {
                groupingInfo = info.copy(year = info.year - 1)
            } else {
                viewModelScope.launch {
                    val dateInfo = dateInfoExtra.filterNotNull().first()
                    groupingInfo = info.previous(dateInfo)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val dateInfoExtra: StateFlow<DateInfoExtra?> =
        groupingInfoFlow.filterNotNull().flatMapLatest { grouping ->
            //if we are at the beginning of the year we are interested in the max of the previous year
            val maxYearToLookUp = if (grouping.second <= 1) grouping.year - 1 else grouping.year
            val maxValueExpression = when (grouping.grouping) {
                Grouping.DAY -> String.format(
                    Locale.US,
                    "strftime('%%j','%d-12-31')",
                    maxYearToLookUp
                )

                Grouping.WEEK -> DbUtils.maximumWeekExpression(maxYearToLookUp)
                Grouping.MONTH -> "11"
                else -> "0"
            }
            val projectionList = buildList {
                add("$maxValueExpression AS $KEY_MAX_VALUE")
                if (grouping.grouping == Grouping.WEEK) {
                    //we want to find out the week range when we are given a week number
                    //we find out the first day in the year, which is the beginning of week "0" and then
                    //add (weekNumber)*7 days to get at the beginning of the week
                    add(
                        DbUtils.weekStartFromGroupSqlExpression(
                            grouping.year,
                            grouping.second
                        )
                    )
                }
            }
            contentResolver.observeQuery(
                uri = TransactionProvider.DUAL_URI,
                projection = projectionList.toTypedArray(),
                selection = null,
                selectionArgs = null,
                sortOrder = null,
                notifyForDescendants = false
            ).mapNotNull { query ->
                withContext(Dispatchers.IO) {
                    query.run()?.use { cursor ->
                        cursor.moveToFirst()
                        DateInfoExtra.fromCursor(cursor)
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val displaySubTitle: Flow<String> = combine(
        groupingInfoFlow.filterNotNull(),
        dateInfo,
        dateInfoExtra
    ) { groupingInfo, dateInfo, dateInfoExtra ->
        if (groupingInfo.grouping == Grouping.NONE) {
            defaultDisplayTitle
        } else if (dateInfoExtra != null) {
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
        incomeType: Boolean,
        aggregateNeutral: Boolean,
        groupingInfo: GroupingInfo,
        whereFilter: WhereFilter = WhereFilter.empty(),
        keepCriteria: ((Category) -> Boolean)? = null,
        queryParameter: Map<String, String> = emptyMap(),
    ): Flow<Category> =
        categoryTree(
            selection = buildFilterClause(groupingInfo, whereFilter, VIEW_WITH_ACCOUNT),
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
            additionalSelectionArgs = buildList {
                (accountInfo as? Budget)?.id?.let { add(it.toString()) }
                addAll(whereFilter.getSelectionArgs(true))
            }.toTypedArray(),
            queryParameter = queryParameter + buildMap {
                put(KEY_TYPE, incomeType.toString())
                put(
                    TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL,
                    aggregateNeutral.toString()
                )
                if (accountInfo.accountId != DataBaseAccount.HOME_AGGREGATE_ID) {
                    put(KEY_ACCOUNTID, accountInfo.accountId.toString())
                }
                if (groupingInfo.grouping != Grouping.NONE) {
                    put(DatabaseConstants.KEY_YEAR, groupingInfo.year.toString())
                    if (groupingInfo.grouping != Grouping.YEAR) {
                        put(DatabaseConstants.KEY_SECOND_GROUP, groupingInfo.second.toString())
                    }
                }
            },
            keepCriteria = keepCriteria
        ).mapNotNull {
            when (it) {
                is LoadingState.Empty -> Category.EMPTY
                is LoadingState.Data -> it.data
            }
        }

    val filterClause: String
        get() = buildFilterClause(groupingInfo!!, _whereFilter.value, VIEW_COMMITTED)

    private fun buildFilterClause(
        groupingInfo: GroupingInfo,
        whereFilter: WhereFilter,
        table: String
    ): String {
        return listOfNotNull(
            dateFilterClause(groupingInfo),
            whereFilter.getSelectionForParts(table).takeIf { it.isNotEmpty() }
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
            preferences[aggregateNeutralPrefKey] ?: false
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
            //if we ask for both sums and income, aggregateNeutral does not make sense, since
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
                    buildFilterClause(grouping, whereFilter, VIEW_WITH_ACCOUNT),
                    whereFilter.getSelectionArgs(true),
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

    @Parcelize
    data class GroupingInfo(
        val grouping: Grouping = Grouping.NONE,
        val year: Int = -1,
        val second: Int = -1
    ) : Parcelable
}