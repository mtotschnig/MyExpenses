package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.DateInfo2
import org.totschnig.myexpenses.viewmodel.data.DateInfo3
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo
import java.util.*

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

    protected val _filterPersistence: MutableStateFlow<FilterPersistence?> = MutableStateFlow(null)
    val filterPersistence: StateFlow<FilterPersistence?> = _filterPersistence

    protected val _aggregateTypes = MutableStateFlow(true)
    private val _incomeType = MutableStateFlow(false)
    val groupingInfoFlow: Flow<GroupingInfo?>
        get() = savedStateHandle.getLiveData<GroupingInfo?>(KEY_GROUPING_INFO, null).asFlow()

    val aggregateTypes: Boolean
        get() = _aggregateTypes.value

    val incomeType: Boolean
        get() = _incomeType.value

    val grouping: Grouping
        get() = groupingInfo?.grouping ?: Grouping.NONE

    fun setAggregateTypes(newValue: Boolean) {
        _aggregateTypes.tryEmit(newValue)
    }

    fun setIncomeType(newValue: Boolean) {
        _incomeType.tryEmit(newValue)
    }

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
                dateInfo.filterNotNull().collect {
                    groupingInfo = GroupingInfo(
                        grouping = grouping,
                        year = when (grouping) {
                            Grouping.WEEK -> it.yearOfWeekStart
                            Grouping.MONTH -> it.yearOfMonthStart
                            else -> it.year
                        },
                        second = when (grouping) {
                            Grouping.DAY -> it.day
                            Grouping.WEEK -> it.week
                            Grouping.MONTH -> it.month
                            else -> 0
                        }
                    )
                }
            }
        }
    }

    fun GroupingInfo.next(dateInfo: DateInfo3): GroupingInfo {
        val nextSecond = second + 1
        val overflow = nextSecond > dateInfo.maxValue
        return copy(
            year = if (overflow) year + 1 else year,
            second = if (overflow) grouping.minValue else nextSecond
        )
    }

    fun GroupingInfo.previous(dateInfo: DateInfo3): GroupingInfo {
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

    private val dateInfo: Flow<DateInfo2?> = contentResolver.observeQuery(
        uri = TransactionProvider.DUAL_URI,
        projection = arrayOf(
            "${getThisYearOfWeekStart()} AS $KEY_THIS_YEAR_OF_WEEK_START",
            "${getThisYearOfMonthStart()} AS $KEY_THIS_YEAR_OF_MONTH_START",
            "$THIS_YEAR AS $KEY_THIS_YEAR",
            "${getThisMonth()} AS $KEY_THIS_MONTH",
            "${getThisWeek()} AS $KEY_THIS_WEEK",
            "$THIS_DAY AS $KEY_THIS_DAY"
        ),
        selection = null, selectionArgs = null, sortOrder = null, notifyForDescendants = false
    ).transform { query ->
        withContext(Dispatchers.IO) {
            query.run()?.use { cursor ->
                cursor.moveToFirst()
                DateInfo2.fromCursor(cursor)
            }
        }?.let {
            emit(it)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val dateInfoExtra: StateFlow<DateInfo3?> =
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
                    add(
                        DbUtils.weekEndFromGroupSqlExpression(
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
            ).transform { query ->
                withContext(Dispatchers.IO) {
                    query.run()?.use { cursor ->
                        cursor.moveToFirst()
                        DateInfo3.fromCursor(cursor)
                    }
                }?.let {
                    emit(it)
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
        } else if (dateInfo != null && dateInfoExtra != null) {
            groupingInfo.grouping.getDisplayTitle(
                localizedContext, groupingInfo.year, groupingInfo.second,
                DateInfo(
                    dateInfo.day,
                    dateInfo.week,
                    dateInfo.month,
                    dateInfo.year,
                    dateInfo.yearOfWeekStart,
                    dateInfo.yearOfMonthStart,
                    dateInfoExtra.maxValue,
                    dateInfoExtra.weekStart,
                    dateInfoExtra.weekEnd
                ), userLocaleProvider.getUserPreferredLocale()
            )
        } else null
    }.filterNotNull()

    open val defaultDisplayTitle: String?
        get() = getString(R.string.menu_aggregates)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTreeForDistribution = combine(
        _accountInfo.filterNotNull(),
        _aggregateTypes,
        _incomeType,
        groupingInfoFlow.filterNotNull()
    ) { accountInfo, aggregateTypes, incomeType, grouping ->
        Triple(accountInfo, if (aggregateTypes) null else incomeType, grouping)
    }.flatMapLatest { (accountInfo, incomeType, grouping) ->
        categoryTreeWithSum(
            accountInfo = accountInfo,
            incomeType = incomeType,
            groupingInfo = grouping,
            keepCriteria = { it.sum != 0L }
        )
    }.map { it.sortChildrenBySumRecursive() }

    fun categoryTreeWithSum(
        accountInfo: T,
        incomeType: Boolean?,
        groupingInfo: GroupingInfo,
        queryParameter: Map<String, String> = emptyMap(),
        filterPersistence: FilterPersistence? = null,
        selection: String? = null,
        keepCriteria: ((Category) -> Boolean)? = null
    ): Flow<Category> =
        categoryTree(
            selection = selection,
            projection = buildList {
                add("$TREE_CATEGORIES.*")
                add(sumColumn(accountInfo, incomeType, groupingInfo, filterPersistence))
                if (accountInfo is Budget) {
                    add(KEY_BUDGET)
                    add(KEY_BUDGET_ROLLOVER_PREVIOUS)
                    add(KEY_BUDGET_ROLLOVER_NEXT)
                    add(KEY_ONE_TIME)
                }
            }.toTypedArray(),
            additionalSelectionArgs = buildList {
                (accountInfo as? Budget)?.id?.let { add(it.toString()) }
                filterPersistence?.whereFilter?.getSelectionArgs(true)?.let {
                    addAll(it)
                }
            }.toTypedArray(),
            queryParameter = queryParameter,
            keepCriteria = keepCriteria
        )

    private fun sumColumn(
        accountInfo: T,
        incomeType: Boolean?,
        grouping: GroupingInfo,
        filterPersistence: FilterPersistence?
    ): String {
        val accountSelection: String?
        var amountCalculation = KEY_AMOUNT
        var table = VIEW_COMMITTED
        when {
            accountInfo.accountId == Account.HOME_AGGREGATE_ID -> {
                accountSelection = null
                amountCalculation =
                    getAmountHomeEquivalent(VIEW_WITH_ACCOUNT)
                table = VIEW_WITH_ACCOUNT
            }
            accountInfo.accountId < 0 -> {
                accountSelection =
                    " IN (SELECT $KEY_ROWID from $TABLE_ACCOUNTS WHERE $KEY_CURRENCY = '${accountInfo.currency.code}' AND $KEY_EXCLUDE_FROM_TOTALS = 0 )"
            }
            else -> {
                accountSelection = " = ${accountInfo.accountId}"
            }
        }
        var catFilter =
            "FROM $table WHERE ${WHERE_NOT_VOID}${if (accountSelection == null) "" else " AND +${KEY_ACCOUNTID}$accountSelection"} AND $KEY_CATID = $TREE_CATEGORIES.${KEY_ROWID}"
        if (incomeType != null) {
            catFilter += " AND " + KEY_AMOUNT + (if (incomeType) ">" else "<") + "0"
        }
        buildFilterClause(grouping, filterPersistence, table).takeIf { it.isNotEmpty() }?.let {
            catFilter += " AND $it"
        }
        return "(SELECT sum($amountCalculation) $catFilter) AS $KEY_SUM"
    }

    val filterClause: String
        get() = buildFilterClause(groupingInfo!!, _filterPersistence.value, VIEW_EXTENDED)

    private fun buildFilterClause(
        groupingInfo: GroupingInfo,
        filterPersistence: FilterPersistence?,
        table: String
    ): String {
        return listOfNotNull(
            dateFilterClause(groupingInfo),
            filterPersistence?.whereFilter?.getSelectionForParts(table)?.takeIf { it.isNotEmpty() }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val sums: Flow<Pair<Long, Long>> = combine(
        _accountInfo.filterNotNull(),
        groupingInfoFlow,
        _filterPersistence
    ) { accountInfo, grouping, filterPersistence ->
        grouping?.let {
            Triple(
                accountInfo,
                grouping,
                filterPersistence
            )
        }
    }
        .filterNotNull()
        .flatMapLatest { (accountInfo, grouping, filterPersistence) ->
            val builder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_GROUPED_BY_TYPE, "1")
            val id = accountInfo.accountId
            if (id != Account.HOME_AGGREGATE_ID) {
                if (id < 0) {
                    builder.appendQueryParameter(KEY_CURRENCY, accountInfo.currency.code)
                } else {
                    builder.appendQueryParameter(KEY_ACCOUNTID, id.toString())
                }
            }
            //if we have no income or expense, there is no row in the cursor
            contentResolver.observeQuery(
                builder.build(),
                null,
                buildFilterClause(grouping, filterPersistence, VIEW_WITH_ACCOUNT),
                filterPersistence?.whereFilter?.getSelectionArgs(true),
                null, true
            ).transform { query ->
                withContext(Dispatchers.IO) {
                    query.run()?.use { cursor ->
                        var income: Long = 0
                        var expense: Long = 0
                        for (pair in cursor.asSequence) {
                            val type = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TYPE))
                            val sum = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SUM))
                            if (type > 0) {
                                income = sum
                            } else {
                                expense = sum
                            }
                        }
                        Pair(income, expense)
                    }
                }?.let {
                    emit(it)
                }
            }
        }

    @Parcelize
    data class GroupingInfo(
        val grouping: Grouping = Grouping.NONE,
        val year: Int = -1,
        val second: Int = -1
    ) : Parcelable
}