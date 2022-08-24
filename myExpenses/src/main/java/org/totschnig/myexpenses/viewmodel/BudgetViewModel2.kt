package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
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
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Category

class BudgetViewModel2(application: Application, savedStateHandle: SavedStateHandle) :
    DistributionViewModelBase<Budget>(application, savedStateHandle) {
    private val _allocatedOnly = MutableStateFlow(false)

    fun setAllocatedOnly(newValue: Boolean) {
        _allocatedOnly.tryEmit(newValue)
    }

    val allocatedOnly: Boolean
        get() = _allocatedOnly.value

    private lateinit var budgetFlow: Flow<Long>
    lateinit var categoryTreeForBudget: Flow<Category>

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
    fun initWithBudget(budgetId: Long) {

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
                    setGrouping(budget.grouping)
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
            contentResolver.observeQuery(
                uri = budgetAllocationUri(budgetId, 0).buildUpon()
                    .appendQueryParameter(DatabaseConstants.KEY_YEAR, info.year.toString())
                    .appendQueryParameter(
                        DatabaseConstants.KEY_SECOND_GROUP,
                        info.second.toString()
                    )
                    .build()
            ).mapToOne(0) { it.getLong(0) }
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
                        put(DatabaseConstants.KEY_YEAR, grouping.year.toString())
                        put(DatabaseConstants.KEY_SECOND_GROUP, grouping.second.toString())
                    },
                    filterPersistence = filterPersistence,
                    selection = if (allocatedOnly) "${DatabaseConstants.KEY_BUDGET} IS NOT NULL" else null,
                ).map { it.copy(budget = budget) }
            }
    }

    fun budgetAllocationUri(budgetId: Long, categoryId: Long) = ContentUris.withAppendedId(
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

    fun updateBudget(budgetId: Long, categoryId: Long, amount: Money, oneTime: Boolean) {
        groupingInfo?.also {
            val contentValues = ContentValues(1).apply {
                put(DatabaseConstants.KEY_BUDGET, amount.amountMinor)
                put(DatabaseConstants.KEY_YEAR, it.year)
                put(DatabaseConstants.KEY_SECOND_GROUP, it.second)
                put(DatabaseConstants.KEY_ONE_TIME, oneTime)
            }
            viewModelScope.launch(context = coroutineContext()) {
                contentResolver.update(
                    budgetAllocationUri(budgetId, categoryId),
                    contentValues,
                    null,
                    null
                )
            }
        } ?: run { CrashHandler.report("Trying to update budget while groupingInfo is not set") }
    }

    fun deleteBudget(budgetId: Long) = liveData(context = coroutineContext()) {
        emit(
            contentResolver.delete(
                ContentUris.withAppendedId(
                    TransactionProvider.BUDGETS_URI,
                    budgetId
                ), null, null
            ) == 1
        )
    }
}