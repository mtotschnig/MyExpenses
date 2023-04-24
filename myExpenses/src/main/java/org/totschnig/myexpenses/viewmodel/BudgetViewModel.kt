package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import arrow.core.Tuple4
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.util.*

open class BudgetViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    val data = contentResolver.observeQuery(
        uri = TransactionProvider.BUDGETS_URI,
        projection = PROJECTION
    ).mapToList(mapper = budgetCreatorFunction)

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
            if(it.moveToFirst()) emit((budgetCreatorFunction(it)))
        }
    }

    @OptIn(FlowPreview::class)
    val amounts: Flow<Tuple4<Int, Long, Long, Long>> = budgetLoaderFlow.map { pair ->
        val (position, budget) = pair
        val sumBuilder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
        if (prefHandler.getBoolean(PrefKey.BUDGET_AGGREGATE_TYPES, true)) {
            sumBuilder.appendBooleanQueryParameter(
                TransactionProvider.QUERY_PARAMETER_AGGREGATE_TYPES
            )
                .build()
        }
        val isTotalAccount = budget.accountId == HOME_AGGREGATE_ID
        if (!isTotalAccount) {
            if (budget.accountId < 0) {
                sumBuilder.appendQueryParameter(KEY_CURRENCY, budget.currency.code)
            } else {
                sumBuilder.appendQueryParameter(KEY_ACCOUNTID, budget.accountId.toString())
            }
        }
        val filterPersistence =
            FilterPersistence(prefHandler, prefNameForCriteria(budget.id), null, false)
        var filterClause = buildDateFilterClause(budget)
        val selectionArgs: Array<String>?
        if (!filterPersistence.whereFilter.isEmpty) {
            filterClause += " AND " + filterPersistence.whereFilter.getSelectionForParts(
                VIEW_WITH_ACCOUNT
            )
            selectionArgs = filterPersistence.whereFilter.getSelectionArgs(true)
        } else {
            selectionArgs = null
        }

        val allocationBuilder = ContentUris.withAppendedId(
            ContentUris.withAppendedId(
                TransactionProvider.BUDGETS_URI,
                budget.id
            ),
            0
        ).buildUpon()
        if (budget.grouping != Grouping.NONE) {
            allocationBuilder.appendQueryParameter(KEY_YEAR, THIS_YEAR)
            if (budget.grouping != Grouping.YEAR) {
                allocationBuilder.appendQueryParameter(
                    KEY_SECOND_GROUP,
                    thisSecond(budget.grouping)
                )
            }
        }

        combine(
            contentResolver.observeQuery(
                sumBuilder.build(),
                null, filterClause, selectionArgs, null, true
            )
                .mapToOne { cursor -> cursor.getLong(0) },
            contentResolver.observeQuery(
                uri = allocationBuilder.build()
            ).mapToOne(0) { it.getLong(0) }
        ) { spent, allocated ->
            Tuple4(position, budget.id, spent, allocated)
        }
    }.flattenMerge()

    fun loadBudgetAmounts(position: Int, budget: Budget) {
        viewModelScope.launch {
            budgetLoaderFlow.emit(position to budget)
        }
    }

    private fun buildDateFilterClause(budget: Budget): String {
        val year = "$YEAR = $THIS_YEAR"
        return when (budget.grouping) {
            Grouping.YEAR -> year
            Grouping.DAY -> "$year AND $DAY = ${thisSecond(budget.grouping)}"
            Grouping.WEEK -> getYearOfWeekStart() + " = " + getThisYearOfWeekStart() + " AND " + getWeek() + " = " + thisSecond(
                budget.grouping
            )
            Grouping.MONTH -> getYearOfMonthStart() + " = " + getThisYearOfMonthStart() + " AND " + getMonth() + " = " + thisSecond(
                budget.grouping
            )
            else -> budget.durationAsSqlFilter()
        }
    }

    private fun thisSecond(grouping: Grouping) = when (grouping) {
        Grouping.DAY -> THIS_DAY
        Grouping.WEEK -> getThisWeek()
        Grouping.MONTH -> getThisMonth()
        else -> ""
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
