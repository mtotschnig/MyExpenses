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
import kotlinx.coroutines.flow.*
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

/*    private val _budget = MutableStateFlow<Budget?>(null)
    val budget: Flow<Budget?> = _budget*/

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
            Money(
                currencyUnit,
                cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_BUDGET))
            ),
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
                val oldGroupingInfo = groupingInfo
                groupingInfo = null
                _accountInfo.tryEmit(budget)
                if (oldGroupingInfo?.grouping == budget.grouping) {
                    groupingInfo = oldGroupingInfo
                } else {
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
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTreeForBudget: Flow<Category> = combine(
        _accountInfo.filterNotNull(),
        _aggregateTypes,
        _allocatedOnly,
        groupingInfoFlow,
        _filterPersistence
    ) { accountInfo, aggregateTypes, allocatedOnly, grouping, filterPersistence ->
        grouping?.let {
            Tuple5(
                accountInfo,
                if (aggregateTypes) null else false,
                allocatedOnly,
                it,
                filterPersistence
            )
        }
    }
        .filterNotNull()
        .flatMapLatest { (accountInfo, incomeType, allocatedOnly, grouping, filterPersistence) ->
            categoryTreeWithSum(
                accountInfo = accountInfo,
                incomeType = incomeType,
                groupingInfo = grouping,
                queryParameter = buildMap {
                    put(DatabaseConstants.KEY_YEAR, grouping.year.toString())
                    put(DatabaseConstants.KEY_SECOND_GROUP, grouping.second.toString())
                },
                filterPersistence = filterPersistence,
                selection = if (allocatedOnly) "${DatabaseConstants.KEY_BUDGET} IS NOT NULL" else null
            )
        }

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
            val budgetUri = ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId)
            viewModelScope.launch(context = coroutineContext()) {
                contentResolver.update(
                    if (categoryId == 0L) budgetUri else ContentUris.withAppendedId(
                        budgetUri,
                        categoryId
                    ),
                    contentValues, null, null
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