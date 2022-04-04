package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Category2
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo

class BudgetViewModel2(application: Application, savedStateHandle: SavedStateHandle) :
    DistributionViewModel(application, savedStateHandle) {
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
            ).mapToOne(mapper = budgetCreatorFunction).collect {
                _accountInfo.tryEmit(it)
                //_budget.tryEmit(it)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTreeForBudget: Flow<Category2> = categoryTreeForDistribution()
}