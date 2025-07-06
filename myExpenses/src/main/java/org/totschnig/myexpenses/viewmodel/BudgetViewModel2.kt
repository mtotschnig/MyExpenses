package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import arrow.core.Tuple5
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.budgetAllocationQueryUri
import org.totschnig.myexpenses.db2.deleteBudget
import org.totschnig.myexpenses.db2.getCategoryInfoList
import org.totschnig.myexpenses.db2.getMethod
import org.totschnig.myexpenses.db2.getPartyName
import org.totschnig.myexpenses.db2.getTag
import org.totschnig.myexpenses.db2.getUuidForAccount
import org.totschnig.myexpenses.db2.importBudget
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.BudgetAllocationExport
import org.totschnig.myexpenses.model2.BudgetExport
import org.totschnig.myexpenses.model2.CategoryPath
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_DEFAULT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.provider.filter.asSet
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.GroupingInfo
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.widget.BudgetWidget
import org.totschnig.myexpenses.widget.WIDGET_LIST_DATA_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets

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

    val editRollOverMap = SnapshotStateMap<Long, Long>()

    private val _allocatedOnly = MutableStateFlow(false)

    fun setAllocatedOnly(newValue: Boolean) {
        _allocatedOnly.tryEmit(newValue)
    }

    val allocatedOnly: Boolean
        get() = _allocatedOnly.value

    private lateinit var budgetFlow: Flow<BudgetAllocation>
    lateinit var categoryTreeForBudget: Flow<Category>

    val sum: StateFlow<Long> by lazy {
        sums.map { it.second }.stateIn(viewModelScope, SharingStarted.Companion.Lazily, 0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun initWithBudget(budgetId: Long, groupingYear: Int, groupingSecond: Int) {

        viewModelScope.launch {
            contentResolver.observeQuery(
                uri = BaseTransactionProvider.budgetUri(budgetId),
                notifyForDescendants = true
            ).mapToOne(mapper = repository.budgetCreatorFunction).collect { budget ->
                _accountInfo.tryEmit(budget)
                if (groupingInfo == null) {
                    if (groupingYear == 0 && groupingSecond == 0) {
                        setGrouping(budget.grouping)
                    } else {
                        groupingInfo = GroupingInfo(budget.grouping, groupingYear, groupingSecond)
                    }
                }
            }
        }

        viewModelScope.launch {
            FilterPersistence(
                dataStore, BudgetViewModel.prefNameForCriteria(budgetId), viewModelScope
            ).whereFilter.collect { newValue ->
                _whereFilter.update { newValue }
            }
        }

        budgetFlow = groupingInfoFlow.filterNotNull().flatMapLatest { info ->
            contentResolver.observeQuery(budgetAllocationQueryUri(budgetId, info))
                .mapToOne(BudgetAllocation.EMPTY, mapper = BudgetAllocation.Companion::fromCursor)
        }

        categoryTreeForBudget = combine(
            _accountInfo.filterNotNull(),
            aggregateNeutral,
            _allocatedOnly,
            groupingInfoFlow.filterNotNull(),
            _whereFilter
        ) { accountInfo, aggregateNeutral, allocatedOnly, grouping, whereFilter ->
            Tuple5(
                accountInfo,
                aggregateNeutral,
                allocatedOnly,
                grouping,
                whereFilter
            )
        }.combine(budgetFlow) { tuple, budget -> tuple to budget }
            .flatMapLatest { (tuple, budget) ->
                val (accountInfo, aggregateNeutral, allocatedOnly, grouping, whereFilter) = tuple
                categoryTreeWithSum(
                    accountInfo = accountInfo,
                    isIncome = false,
                    aggregateNeutral = aggregateNeutral,
                    groupingInfo = grouping,
                    whereFilter = whereFilter,
                    keepCriterion = if (allocatedOnly) { { !(it.budget == BudgetAllocation.EMPTY && it.sum == 0L) } } else null
                ).map { it.copy(budget = budget) }
            }
    }

    override fun dateFilterClause(groupingInfo: GroupingInfo): String? {
        return if (groupingInfo.grouping == Grouping.NONE) accountInfo.value?.durationAsSqlFilter() else
            super.dateFilterClause(groupingInfo)
    }

    override val defaultDisplayTitle: String?
        get() = accountInfo.value?.durationPrettyPrint()

    private val GroupingInfo.asContentValues: ContentValues
        get() = ContentValues().also {
            if (grouping != Grouping.NONE) {
                it.put(KEY_YEAR, year)
                it.put(KEY_SECOND_GROUP, second)
            }
        }

    fun updateBudget(budgetId: Long, categoryId: Long, amount: Money, oneTime: Boolean) {
        groupingInfo?.also {
            val contentValues = it.asContentValues.apply {
                put(KEY_BUDGET, amount.amountMinor)
                if (it.grouping != Grouping.NONE) {
                    put(KEY_ONE_TIME, oneTime)
                }
            }
            viewModelScope.launch(context = coroutineContext()) {
                contentResolver.update(
                    BaseTransactionProvider.budgetAllocationUri(budgetId, categoryId),
                    contentValues,
                    null,
                    null
                )
            }
        } ?: run {
            CrashHandler.report(Exception("Trying to update budget while groupingInfo is not set"))
        }
    }

    fun deleteBudget(budgetId: Long) = liveData(context = coroutineContext()) {
        emit(repository.deleteBudget(budgetId) == 1)
    }

    fun rollOverClear() {
        viewModelScope.launch(context = coroutineContext()) {
            val budget = accountInfo.value!!
            val selection =
                "$KEY_BUDGETID = ? AND $KEY_YEAR = ? AND $KEY_SECOND_GROUP = ?"
            val nextGrouping = nextGrouping()!!
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
                    .withValues(ContentValues().apply { putNull(KEY_BUDGET_ROLLOVER_NEXT) })
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
                    .withValues(ContentValues().apply { putNull(KEY_BUDGET_ROLLOVER_PREVIOUS) })
                    .build()
            )
            contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
        }
    }

    private suspend fun saveRollOverList(rollOverList: List<Pair<Long, Long>>) {
        val budget = accountInfo.value!!
        val nextGrouping = nextGrouping()!!
        val ops = ArrayList<ContentProviderOperation>()
        rollOverList.forEach {
            val (categoryId, rollOver) = it
            val budgetAllocationUri =
                BaseTransactionProvider.budgetAllocationUri(budget.id, categoryId)
            ops.add(
                ContentProviderOperation.newUpdate(budgetAllocationUri)
                    .withValues(groupingInfo!!.asContentValues.apply {
                        put(KEY_BUDGET_ROLLOVER_NEXT, rollOver)
                    }).build()
            )
            ops.add(
                ContentProviderOperation.newUpdate(budgetAllocationUri)
                    .withValues(nextGrouping.asContentValues.apply {
                        put(KEY_BUDGET_ROLLOVER_PREVIOUS, rollOver)
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
                        0L to tree.budget.totalAllocated + sum.value
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
                    (category.budget.totalAllocated + category.aggregateSum).takeIf { it != 0L }
                        ?.let {
                            category.id to it
                        }
                })
            }
        }
    }

    fun rollOverSave() {
        duringRollOverSave = true
        viewModelScope.launch(context = coroutineContext()) {
            saveRollOverList(editRollOverMap.toList())
            duringRollOverSave = false
            editRollOverMap.clear()
        }
    }

    override val aggregateNeutralPrefKey by lazy {
        aggregateNeutralPrefKey(savedStateHandle[KEY_ROWID]!!)
    }

    override suspend fun persistAggregateNeutral(aggregateNeutral: Boolean) {
        super.persistAggregateNeutral(aggregateNeutral)
        updateWidgets(getApplication(), BudgetWidget::class.java, WIDGET_LIST_DATA_CHANGED)
    }

    override val withIncomeSum = false

    fun exportBudget(accountName: String? = null) {
        val budget = accountInfo.value!!
        val budgetId = budget.id
        val accountName = accountName ?: getSyncAccountName(budget) ?: return
        viewModelScope.launch(context = coroutineContext()) {
            GenericAccountService.getSyncBackendProvider(localizedContext, accountName)
                .mapCatching { backend ->
                    contentResolver.query(
                        BaseTransactionProvider.budgetUri(budgetId),
                        null, null, null, null
                    )?.use {
                        if (it.moveToFirst()) {
                            val grouping: Grouping = it.getEnum(KEY_GROUPING, Grouping.NONE)
                            val criteria = whereFilter.value.asSet
                            it.getString(KEY_UUID) to BudgetExport(
                                title = it.getString(KEY_TITLE),
                                description = it.getString(KEY_DESCRIPTION),
                                grouping = grouping,
                                accountUuid = it.getStringOrNull(KEY_ACCOUNT_UUID),
                                currency = it.getString(KEY_CURRENCY),
                                start = if (grouping == Grouping.NONE) it.getString(KEY_START) else null,
                                end = if (grouping == Grouping.NONE) it.getString(KEY_END) else null,
                                isDefault = it.getBoolean(KEY_IS_DEFAULT),
                                categoryFilter = criteria
                                    .filterIsInstance<CategoryCriterion>()
                                    .firstOrNull()
                                    ?.values
                                    ?.mapNotNull { categoryInfo.getValue(it) },
                                partyFilter = criteria
                                    .filterIsInstance<PayeeCriterion>()
                                    .firstOrNull()
                                    ?.values
                                    ?.mapNotNull { repository.getPartyName(it) },
                                methodFilter = criteria
                                    .filterIsInstance<MethodCriterion>()
                                    .firstOrNull()
                                    ?.values
                                    ?.mapNotNull { repository.getMethod(it) },
                                statusFilter = criteria
                                    .filterIsInstance<CrStatusCriterion>()
                                    .firstOrNull()
                                    ?.values
                                    ?.map { it.name },
                                tagFilter = criteria
                                    .filterIsInstance<TagCriterion>()
                                    .firstOrNull()
                                    ?.values
                                    ?.mapNotNull { repository.getTag(it) },
                                accountFilter = criteria
                                    .filterIsInstance<AccountCriterion>()
                                    .firstOrNull()
                                    ?.values
                                    ?.mapNotNull { repository.getUuidForAccount(it) },
                                allocations = contentResolver.query(
                                    TransactionProvider.BUDGET_ALLOCATIONS_URI,
                                    arrayOf(
                                        KEY_CATID,
                                        KEY_YEAR,
                                        KEY_SECOND_GROUP,
                                        KEY_BUDGET,
                                        KEY_BUDGET_ROLLOVER_PREVIOUS,
                                        KEY_BUDGET_ROLLOVER_NEXT,
                                        KEY_ONE_TIME
                                    ),
                                    "$KEY_BUDGETID = ?",
                                    arrayOf(budgetId.toString()),
                                    null
                                )?.useAndMapToList { cursor ->
                                    BudgetAllocationExport(
                                        cursor.getLong(KEY_CATID).takeIf { it != 0L }
                                            ?.let { categoryInfo.getValue(it) },
                                        cursor.getIntOrNull(KEY_YEAR),
                                        cursor.getIntOrNull(KEY_SECOND_GROUP),
                                        cursor.getLong(KEY_BUDGET),
                                        cursor.getLongOrNull(KEY_BUDGET_ROLLOVER_PREVIOUS),
                                        cursor.getLongOrNull(KEY_BUDGET_ROLLOVER_NEXT),
                                        cursor.getBoolean(KEY_ONE_TIME)
                                    )
                                } ?: emptyList()
                            )
                        } else null
                    }?.let {
                        backend.writeBudget(it.first, it.second)
                    }
                }.fold(
                    onSuccess = {
                        setSynced(accountName)
                        it
                    },
                    onFailure = {
                        CrashHandler.report(it)
                        getString(R.string.write_fail_reason_cannot_write) + ": " + it.message
                    }
                )?.let { message -> _syncResult.update { message } }
        }
    }

    fun importBudget() {
        val budget = accountInfo.value!!
        val uuid = budget.uuid ?: return
        val accountName = getSyncAccountName(budget) ?: return
        viewModelScope.launch(context = coroutineContext()) {
            GenericAccountService.getSyncBackendProvider(localizedContext, accountName)
                .mapCatching { backend ->
                    repository.importBudget(
                        backend.getBudget(uuid),
                        budget.id,
                        budget.accountId,
                        null
                    )
                }
        }
    }

    fun setSynced(accountName: String) {
        accountInfo.value?.let {
            setBudgetSynced(it.id, it.accountId, prefHandler, accountName)
        }
    }

    fun getSyncAccountName(budget: Budget) =
        if (budget.accountId > 0) budget.syncAccountName else
            getSyncAccountForAggregateBudget(budget.id)

    fun isSynced() = accountInfo.value?.let { budget ->
        if (budget.accountId > 0) budget.syncAccountName != null &&
                prefHandler.getBoolean(KEY_BUDGET_IS_SYNCED + budget.id, false)
        else getSyncAccountForAggregateBudget(budget.id) != null
    } == true

    private fun getSyncAccountForAggregateBudget(budgetId: Long) =
        prefHandler.getString(KEY_BUDGET_AGGREGATE_SYNC_ACCOUNT_NAME + budgetId, null).takeIf { sync ->
            GenericAccountService.getAccounts(getApplication()).any {
                it.name == sync
            }
        }

    val categoryInfo: Map<Long, CategoryPath> = lazyMap {
        repository.getCategoryInfoList(it) ?: emptyList()
    }

    companion object {

        fun aggregateNeutralPrefKey(budgetId: Long) =
            booleanPreferencesKey(AGGREGATE_NEUTRAL_PREF_KEY_PREFIX + budgetId)

        private const val AGGREGATE_NEUTRAL_PREF_KEY_PREFIX = "budgetAggregateNeutral_"
    }
}