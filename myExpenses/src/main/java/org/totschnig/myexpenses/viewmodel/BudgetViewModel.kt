package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import arrow.core.Tuple4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.budgetAllocationQueryUri
import org.totschnig.myexpenses.db2.sumLoaderForBudget
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_DEFAULT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGETS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES
import org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.util.Locale

open class BudgetViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    val data by lazy {
        contentResolver.observeQuery(
            uri = TransactionProvider.BUDGETS_URI,
            projection = PROJECTION
        ).mapToList(mapper = repository.budgetCreatorFunction)
    }

    /**
     * provides id of budget on success, -1 on error
     */
    val databaseResult = MutableLiveData<Long>()

    private val budgetLoaderFlow = MutableSharedFlow<Pair<Int, Budget>>()


    fun budget(budgetId: Long) = liveData(context = coroutineContext()) {
        contentResolver.query(
            TransactionProvider.BUDGETS_URI,
            PROJECTION, "${q(KEY_ROWID)} = ?", arrayOf(budgetId.toString()), null
        )?.use {
            if (it.moveToFirst()) emit((repository.budgetCreatorFunction(it)))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val amounts: Flow<Tuple4<Int, Long, Long, Long>> = budgetLoaderFlow.map { pair ->
        val (position, budget) = pair

        val (sumUri, sumSelection, sumSelectionArguments) = repository.sumLoaderForBudget(budget)

        val allocationUri = budgetAllocationQueryUri(
            budget.id,
            0,
            budget.grouping,
            THIS_YEAR,
            budget.grouping.queryArgumentForThisSecond
        )

        combine(
            contentResolver.observeQuery(
                sumUri,
                null, sumSelection, sumSelectionArguments, null, true
            )
                .mapToOne { it.getLong(KEY_SUM) },
            contentResolver.observeQuery(allocationUri)
                .mapToOne(0) { it.getLong(KEY_BUDGET) }
        ) { spent, allocated -> Tuple4(position, budget.id, spent, allocated) }
    }.flattenMerge()

    fun loadBudgetAmounts(position: Int, budget: Budget) {
        viewModelScope.launch {
            budgetLoaderFlow.emit(position to budget)
        }
    }

    companion object {
        val PROJECTION = arrayOf(
            q(KEY_ROWID),
            "coalesce($KEY_ACCOUNTID, -(select $KEY_ROWID from $TABLE_CURRENCIES where $KEY_CODE = $TABLE_BUDGETS.$KEY_CURRENCY), ${HOME_AGGREGATE_ID}) AS $KEY_ACCOUNTID",
            KEY_TITLE,
            q(KEY_DESCRIPTION),
            "coalesce($TABLE_BUDGETS.$KEY_CURRENCY, $TABLE_ACCOUNTS.$KEY_CURRENCY) AS $KEY_CURRENCY",
            q(KEY_GROUPING),
            KEY_COLOR,
            KEY_START,
            KEY_END,
            "$TABLE_ACCOUNTS.$KEY_LABEL AS $KEY_ACCOUNT_LABEL",
            KEY_IS_DEFAULT
        )

        fun q(column: String) = "$TABLE_BUDGETS.$column"

        fun prefNameForCriteria(budgetId: Long): String =
            "budgetFilter_%%s_%d".format(Locale.ROOT, budgetId)
    }
}
