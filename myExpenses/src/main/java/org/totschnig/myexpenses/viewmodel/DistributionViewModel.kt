package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.viewmodel.data.Category2
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo

class DistributionViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    CategoryViewModel(application, savedStateHandle) {
    val selectionState: MutableState<Category2?> = mutableStateOf(null)
    val expansionState: SnapshotStateList<Category2> = SnapshotStateList()
    private val accountInfo = MutableStateFlow<DistributionAccountInfo?>(null)

    val _aggregateTypes = MutableStateFlow<Boolean>(true)
    val _incomeType = MutableStateFlow<Boolean>(false)

    val aggregateTypes: Boolean
        get() = _aggregateTypes.value

    val incomeType: Boolean
        get() = _incomeType.value

    fun setAggregateTypes(newValue: Boolean) {
        _aggregateTypes.tryEmit(newValue)
    }

    fun setIncomeType(newValue: Boolean) {
        _incomeType.tryEmit(newValue)
    }

    fun initWithAccount(accountId: Long) {
        viewModelScope.launch {
            account(accountId, true).asFlow().collect {
                accountInfo.tryEmit(
                    DistributionAccountInfo(
                        it.id,
                        it.getLabelForScreenTitle(getApplication()),
                        it.currencyUnit,
                        it.color
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTreeWithSum: StateFlow<Category2> = combine(
        accountInfo.filterNotNull(),
        _aggregateTypes,
        _incomeType
    ) { accountInfo, aggregateTypes, incomeType ->
        accountInfo to if (aggregateTypes) null else incomeType
    }.flatMapLatest { (accountInfo, incomeType) ->
        categoryTree(
            null,
            null,
            arrayOf("*", sumColumn(accountInfo, incomeType)),
            true
        ) { it.sum != 0L }
    }.stateIn(viewModelScope, SharingStarted.Lazily, Category2.EMPTY)

    private fun sumColumn(accountInfo: DistributionAccountInfo, incomeType: Boolean?): String {
        val accountSelection: String?
        var amountCalculation = DatabaseConstants.KEY_AMOUNT
        var table = DatabaseConstants.VIEW_COMMITTED
        when {
            accountInfo.id == Account.HOME_AGGREGATE_ID -> {
                accountSelection = null
                amountCalculation =
                    DatabaseConstants.getAmountHomeEquivalent(DatabaseConstants.VIEW_WITH_ACCOUNT)
                table = DatabaseConstants.VIEW_WITH_ACCOUNT
            }
            accountInfo.id < 0 -> {
                accountSelection =
                    " IN (SELECT ${DatabaseConstants.KEY_ROWID} from ${DatabaseConstants.TABLE_ACCOUNTS} WHERE ${DatabaseConstants.KEY_CURRENCY} = '${accountInfo.currency.code}' AND ${DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS} = 0 )"
            }
            else -> {
                accountSelection = " = ${accountInfo.id}"
            }
        }
        var catFilter =
            "FROM $table WHERE ${DatabaseConstants.WHERE_NOT_VOID}${if (accountSelection == null) "" else " AND +${DatabaseConstants.KEY_ACCOUNTID}$accountSelection"} AND ${DatabaseConstants.KEY_CATID} = Tree.${DatabaseConstants.KEY_ROWID}"
        if (incomeType != null) {
            catFilter += " AND " + KEY_AMOUNT + (if (incomeType) ">" else "<") + "0"
        }
/*        val dateFilter = buildFilterClause(table)
        if (dateFilter != null) {
            catFilter += " AND $dateFilter"
        }*/
        //val extraColumn = extraColumn
        return "(SELECT sum($amountCalculation) $catFilter) AS ${DatabaseConstants.KEY_SUM}"
/*        if (extraColumn != null) {
            projection.add(extraColumn)
        }*/
    }
}