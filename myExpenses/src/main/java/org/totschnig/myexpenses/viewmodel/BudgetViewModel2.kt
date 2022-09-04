package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import arrow.core.Tuple5
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.Category

class BudgetViewModel2(application: Application, savedStateHandle: SavedStateHandle) :
    DistributionViewModelBase<Budget>(application, savedStateHandle) {

    private val editRollOver = mutableStateOf(false)
    private var duringRollOverSave = false
    fun startRollOverEdit(): Boolean {
        if (duringRollOverSave) return false
        editRollOver.value = true
        return true
    }
    fun stopRollOverEdit() {
        editRollOver.value = false
    }
    val duringRollOverEdit: Boolean
        get() = editRollOver.value

    val editRollOverMap = SnapshotStateMap<Long, Pair<Long, Boolean>>()
    val editRollOverInValid: Boolean
        get() = editRollOverMap.any { it.value.second }

    private val _allocatedOnly = MutableStateFlow(false)

    fun setAllocatedOnly(newValue: Boolean) {
        _allocatedOnly.tryEmit(newValue)
    }

    val allocatedOnly: Boolean
        get() = _allocatedOnly.value

    private lateinit var budgetFlow: Flow<BudgetAllocation>
    lateinit var categoryTreeForBudget: Flow<Category>

    val sum: Flow<Long> = combine(sums, _aggregateTypes) { sums, aggregate ->
        if (aggregate) (sums.first - sums.second) else -sums.second
    }

    private val budgetCreatorFunction: (Cursor) -> Budget = { cursor ->
        val currency =
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_CURRENCY))
        val currencyUnit = if (currency.equals(AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE))
            Utils.getHomeCurrency() else currencyContext.get(currency)
        val budgetId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_ROWID))
        val accountId =
            cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_ACCOUNTID))
        val grouping = Grouping.valueOf(
            cursor.getString(
                cursor.getColumnIndexOrThrow(
                    DatabaseConstants.KEY_GROUPING
                )
            )
        )
        Budget(
            budgetId,
            accountId,
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_TITLE)),
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_DESCRIPTION)),
            currencyUnit,
            grouping,
            cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_COLOR)),
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_START)),
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_END)),
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_ACCOUNT_LABEL)),
            getDefault(accountId, grouping) == budgetId
        )
    }

    fun getDefault(accountId: Long, grouping: Grouping) = prefHandler.getLong(
        BudgetViewModel.prefNameForDefaultBudget(
            accountId,
            grouping
        ), 0
    )


    @OptIn(ExperimentalCoroutinesApi::class)
    fun initWithBudget(budgetId: Long, groupingYear: Int, groupingSecond: Int) {

        viewModelScope.launch {
            contentResolver.observeQuery(
                TransactionProvider.BUDGETS_URI,
                BudgetViewModel.PROJECTION,
                "${BudgetViewModel.q(DatabaseConstants.KEY_ROWID)} = ?",
                arrayOf(budgetId.toString()),
                null,
                true
            ).mapToOne(mapper = budgetCreatorFunction).collect { budget ->
                _accountInfo.tryEmit(budget)
                if (groupingInfo == null) {
                    if (groupingYear == 0 && groupingSecond == 0) {
                        setGrouping(budget.grouping)
                    } else {
                        groupingInfo = GroupingInfo(budget.grouping, groupingYear, groupingSecond)
                    }
                }
                _filterPersistence.update {
                    FilterPersistence(
                        prefHandler, BudgetViewModel.prefNameForCriteria(budgetId), null,
                        immediatePersist = false,
                        restoreFromPreferences = true
                    ).also {
                        it.reloadFromPreferences()
                    }
                }
            }
        }

        budgetFlow = groupingInfoFlow.filterNotNull().flatMapLatest { info ->
            val builder = budgetAllocationUri(budgetId, 0).buildUpon()
            if (info.grouping != Grouping.NONE) {
                builder.appendQueryParameter(
                    DatabaseConstants.KEY_YEAR,
                    info.year.toString()
                )
                if (info.grouping != Grouping.YEAR) {
                    builder.appendQueryParameter(
                        DatabaseConstants.KEY_SECOND_GROUP,
                        info.second.toString()
                    )
                }
            }
            contentResolver.observeQuery(
                uri = builder.build()
            ).mapToOne(BudgetAllocation.EMPTY) {
                BudgetAllocation(
                    budget = it.getLong(DatabaseConstants.KEY_BUDGET),
                    rollOverPrevious = it.getLong(DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS),
                    rollOverNext = it.getLong(DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT),
                    oneTime = it.getInt(DatabaseConstants.KEY_ONE_TIME) != 0
                )
            }
        }

        categoryTreeForBudget = combine(
            _accountInfo.filterNotNull(),
            _aggregateTypes,
            _allocatedOnly,
            groupingInfoFlow.filterNotNull(),
            _filterPersistence
        ) { accountInfo, aggregateTypes, allocatedOnly, grouping, filterPersistence ->
            Tuple5(
                accountInfo,
                if (aggregateTypes) null else false,
                allocatedOnly,
                grouping,
                filterPersistence
            )
        }.combine(budgetFlow) { tuple, budget -> tuple to budget }
            .flatMapLatest { (tuple, budget) ->
                val (accountInfo, incomeType, allocatedOnly, grouping, filterPersistence) = tuple
                categoryTreeWithSum(
                    accountInfo = accountInfo,
                    incomeType = incomeType,
                    groupingInfo = grouping,
                    queryParameter = buildMap {
                        if (grouping.grouping != Grouping.NONE) {
                            put(DatabaseConstants.KEY_YEAR, grouping.year.toString())
                            if (grouping.grouping != Grouping.YEAR) {
                                put(DatabaseConstants.KEY_SECOND_GROUP, grouping.second.toString())
                            }
                        }
                    },
                    filterPersistence = filterPersistence,
                    selection = if (allocatedOnly) "${DatabaseConstants.KEY_BUDGET} IS NOT NULL OR ${DatabaseConstants.KEY_SUM} IS NOT NULL" else null,
                ).map { it.copy(budget = budget) }
            }
    }

    private fun budgetAllocationUri(budgetId: Long, categoryId: Long) = ContentUris.withAppendedId(
        ContentUris.withAppendedId(
            TransactionProvider.BUDGETS_URI,
            budgetId
        ),
        categoryId
    )

    override fun dateFilterClause(groupingInfo: GroupingInfo): String? {
        return if (groupingInfo.grouping == Grouping.NONE) accountInfo.value?.durationAsSqlFilter() else
            super.dateFilterClause(groupingInfo)
    }

    override val defaultDisplayTitle: String?
        get() = accountInfo.value?.durationPrettyPrint()

    private val GroupingInfo.asContentValues: ContentValues
        get() = ContentValues().also {
            if (grouping != Grouping.NONE) {
                it.put(DatabaseConstants.KEY_YEAR, year)
                it.put(DatabaseConstants.KEY_SECOND_GROUP, second)
            }
        }

    fun updateBudget(budgetId: Long, categoryId: Long, amount: Money, oneTime: Boolean) {
        groupingInfo?.also {
            val contentValues = it.asContentValues.apply {
                put(DatabaseConstants.KEY_BUDGET, amount.amountMinor)
                if (it.grouping != Grouping.NONE) {
                    put(DatabaseConstants.KEY_ONE_TIME, oneTime)
                }
            }
            viewModelScope.launch(context = coroutineContext()) {
                contentResolver.update(
                    budgetAllocationUri(budgetId, categoryId),
                    contentValues,
                    null,
                    null
                )
            }
        } ?: run {
            CrashHandler.report(Exception("Trying to update budget while groupingInfo is not set"))
        }
    }

    fun deleteBudget(budgetId: Long, defaultKey: String?) = liveData(context = coroutineContext()) {
        emit(
            contentResolver.delete(
                ContentUris.withAppendedId(
                    TransactionProvider.BUDGETS_URI,
                    budgetId
                ), null, null
            ) == 1
        )
        defaultKey?.let { prefHandler.remove(it) }
    }

    fun rollOverClear() {
        viewModelScope.launch(context = coroutineContext()) {
            val budget = accountInfo.value!!
            val selection =
                "${DatabaseConstants.KEY_BUDGETID} = ? AND ${DatabaseConstants.KEY_YEAR} = ? AND ${DatabaseConstants.KEY_SECOND_GROUP} = ?"
            val nextGrouping = groupingInfo!!.next(dateInfoExtra.filterNotNull().first())
            val ops = arrayListOf(
                ContentProviderOperation.newUpdate(TransactionProvider.BUDGET_ALLOCATIONS_URI)
                    .withSelection(
                        selection,
                        arrayOf(
                            budget.id.toString(),
                            groupingInfo!!.year.toString(),
                            groupingInfo!!.second.toString()
                        )
                    )
                    .withValues(ContentValues().apply { putNull(DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT) })
                    .build(),
                ContentProviderOperation.newUpdate(TransactionProvider.BUDGET_ALLOCATIONS_URI)
                    .withSelection(
                        selection,
                        arrayOf(
                            budget.id.toString(),
                            nextGrouping.year.toString(),
                            nextGrouping.second.toString()
                        )
                    )
                    .withValues(ContentValues().apply { putNull(DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS) })
                    .build()
            )
            contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
        }
    }

    private suspend fun saveRollOverList(rollOverList: List<Pair<Long, Long>>) {
        val budget = accountInfo.value!!
        val nextGrouping = groupingInfo!!.next(dateInfoExtra.filterNotNull().first())
        val ops = ArrayList<ContentProviderOperation>()
        rollOverList.forEach {
            val (categoryId, rollOver) = it
            val budgetAllocationUri = budgetAllocationUri(budget.id, categoryId)
            ops.add(
                ContentProviderOperation.newUpdate(budgetAllocationUri)
                    .withValues(groupingInfo!!.asContentValues.apply {
                        put(DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT, rollOver)
                    }).build()
            )
            ops.add(
                ContentProviderOperation.newUpdate(budgetAllocationUri)
                    .withValues(nextGrouping.asContentValues.apply {
                        put(DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS, rollOver)
                    }).build()
            )
        }
        val updateCount =
            contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).sumOf { it.count!! }
        if (updateCount != rollOverList.size * 2) {
            CrashHandler.throwOrReport("Expected update count 2 times ${rollOverList.size}, but actual is $updateCount")
        }
    }

    fun rollOverTotal() {
        viewModelScope.launch(context = coroutineContext()) {
            val tree = categoryTreeForBudget.first()
            if (tree.hasRolloverNext) {
                CrashHandler.throwOrReport("Rollovers already exist")
            } else {
                saveRollOverList(
                    listOf(
                        0L to tree.budget.totalAllocated + sum.first()
                    )
                )
            }
        }
    }

    fun rollOverCategories() {
        viewModelScope.launch(context = coroutineContext()) {
            val tree = categoryTreeForBudget.first()
            if (tree.hasRolloverNext) {
                CrashHandler.throwOrReport("Rollovers already exist")
            } else {
                saveRollOverList(tree.children.mapNotNull { category ->
                    (category.budget.totalAllocated + category.aggregateSum).takeIf { it != 0L }?.let {
                        category.id to it
                    }
                })
            }
        }
    }

    fun rollOverSave() {
        duringRollOverSave = true
        viewModelScope.launch(context = coroutineContext()) {
            check(!editRollOverInValid)
            saveRollOverList(
                editRollOverMap.map { it.key to it.value.first }
            )
            duringRollOverSave = false
        }
    }
}