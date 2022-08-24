package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import arrow.core.Tuple4
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.util.*
import javax.inject.Inject

open class BudgetViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    val data = MutableLiveData<List<Budget>>()
    val budget = MutableLiveData<Budget>()
    /**
     * provides id of budget on success, -1 on error
     */
    val databaseResult = MutableLiveData<Long>()

    val budgetLoaderFlow = MutableSharedFlow<Pair<Int, Budget>>()

    @Inject
    lateinit var licenceHandler: LicenceHandler
    private val databaseHandler: DatabaseHandler = DatabaseHandler(application.contentResolver)
    private val budgetCreatorFunction: (Cursor) -> Budget = { cursor ->
        val currency = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY))
        val currencyUnit = if (currency.equals(AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE))
            Utils.getHomeCurrency() else currencyContext.get(currency)
        val budgetId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID))
        val accountId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ACCOUNTID))
        val grouping = Grouping.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUPING)))
        Budget(
                budgetId,
                accountId,
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                currencyUnit,
                grouping,
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COLOR)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_START)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_END)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_LABEL)),
                getDefault(accountId, grouping) == budgetId
        )
    }

    fun loadAllBudgets() {
        disposable = createQuery(null, null)
                .mapToList(budgetCreatorFunction)
                .subscribe {
                    data.postValue(it)
                }
    }

    fun loadBudget(budgetId: Long, once: Boolean) {
        disposable = createQuery("${q(KEY_ROWID)} = ?", arrayOf(budgetId.toString()))
                .mapToOne(budgetCreatorFunction)
                .subscribe {
                    postBudget(it)
                    if (once) dispose()
                }
    }

    open fun postBudget(budget: Budget) {
        this.budget.postValue(budget)
    }

    @OptIn(FlowPreview::class)
    val spent: Flow<Tuple4<Int, Long, Long, Long>> = budgetLoaderFlow.map {
        val (position, budget) = it
        val builder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
        if (prefHandler.getBoolean(PrefKey.BUDGET_AGGREGATE_TYPES, true)) {
            builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_AGGREGATE_TYPES, "1")
                .build()
        }
        val isTotalAccount = budget.accountId == AggregateAccount.HOME_AGGREGATE_ID
        if (!isTotalAccount) {
            if (budget.accountId < 0) {
                builder.appendQueryParameter(KEY_CURRENCY, budget.currency.code)
            } else {
                builder.appendQueryParameter(KEY_ACCOUNTID, budget.accountId.toString())
            }
        }
        val filterPersistence = FilterPersistence(prefHandler, prefNameForCriteria(budget.id), null, false)
        var filterClause = buildDateFilterClause(budget)
        val selectionArgs: Array<String>?
        if (!filterPersistence.whereFilter.isEmpty) {
            filterClause += " AND " + filterPersistence.whereFilter.getSelectionForParts(VIEW_WITH_ACCOUNT)
            selectionArgs = filterPersistence.whereFilter.getSelectionArgs(true)
        } else {
            selectionArgs = null
        }
        combine(
            contentResolver.observeQuery(builder.build(),
                null, filterClause, selectionArgs, null, true)
                .mapToOne { cursor -> cursor.getLong(0) },
            contentResolver.observeQuery(
                uri = ContentUris.withAppendedId(
                    ContentUris.withAppendedId(
                        TransactionProvider.BUDGETS_URI,
                        budget.id
                    ),
                    0
                ).buildUpon()
                    .appendQueryParameter(KEY_YEAR, THIS_YEAR)
                    .appendQueryParameter(KEY_SECOND_GROUP, thisSecond(budget.grouping))
                    .build()
            ).mapToOne(0) { it.getLong(0) }
        )  { spent, allocated ->
            Tuple4(position, budget.id, spent, allocated)
        }
    }.flattenConcat()

    fun loadBudgetSpend(position: Int, budget: Budget) {
        viewModelScope.launch {
            budgetLoaderFlow.emit(position to budget)
        }
    }

    private fun buildDateFilterClause(budget: Budget): String {
        val year = "$YEAR = $THIS_YEAR"
        return when (budget.grouping) {
            Grouping.YEAR -> year
            Grouping.DAY -> "$year AND $DAY = ${thisSecond(budget.grouping)}"
            Grouping.WEEK -> getYearOfWeekStart() + " = " + getThisYearOfWeekStart() + " AND " + getWeek() + " = " + thisSecond(budget.grouping)
            Grouping.MONTH -> getYearOfMonthStart() + " = " + getThisYearOfMonthStart() + " AND " + getMonth() + " = " + thisSecond(budget.grouping)
            else -> budget.durationAsSqlFilter()
        }
    }

    private fun thisSecond(grouping: Grouping) = when(grouping) {
        Grouping.DAY -> THIS_DAY
        Grouping.WEEK -> getThisWeek()
        Grouping.MONTH -> getThisMonth()
        else -> ""
    }

    fun createQuery(selection: String?, selectionArgs: Array<String>?) =
            briteContentResolver.createQuery(TransactionProvider.BUDGETS_URI,
                    PROJECTION, selection, selectionArgs, null, true)

    fun deleteBudget(budgetId: Long) {
        databaseHandler.startDelete(TOKEN, object: DatabaseHandler.DeleteListener {
            override fun onDeleteComplete(token: Int, result: Int) {
                databaseResult.postValue(if (result == 1) budgetId else -1)
            }
        }, TransactionProvider.BUDGETS_URI, "$KEY_ROWID = ?", arrayOf(budgetId.toString()))
    }

    fun updateBudget(budgetId: Long, categoryId: Long, amount: Money) {
        val contentValues = ContentValues(1)
        contentValues.put(KEY_BUDGET, amount.amountMinor)
        val budgetUri = ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId)
        databaseHandler.startUpdate(TOKEN, null,
                if (categoryId == 0L) budgetUri else ContentUris.withAppendedId(budgetUri, categoryId),
                contentValues, null, null)
    }

    fun getDefault(accountId: Long, grouping: Grouping) = prefHandler.getLong(prefNameForDefaultBudget(accountId, grouping), 0)

    companion object {
        private const val TOKEN = 0
        val PROJECTION = arrayOf(
                q(KEY_ROWID),
                "coalesce(%1\$s, -(select %2\$s from %3\$s where %4\$s = %5\$s), %6\$d) AS %1\$s"
                        .format(Locale.ROOT, KEY_ACCOUNTID, KEY_ROWID, TABLE_CURRENCIES, KEY_CODE,
                                q(KEY_CURRENCY), AggregateAccount.HOME_AGGREGATE_ID),
                KEY_TITLE,
                q(KEY_DESCRIPTION),
                "coalesce(%1\$s.%2\$s, %3\$s.%2\$s) AS %2\$s"
                        .format(TABLE_BUDGETS, KEY_CURRENCY, TABLE_ACCOUNTS),
                q(KEY_GROUPING),
                KEY_COLOR,
                KEY_START,
                KEY_END,
                "$TABLE_ACCOUNTS.$KEY_LABEL AS $KEY_ACCOUNT_LABEL"
        )

        fun q(column:String) = "$TABLE_BUDGETS.$column"

        fun prefNameForCriteria(budgetId: Long): String =
                "budgetFilter_%%s_%d".format(Locale.ROOT, budgetId)

        fun prefNameForDefaultBudget(accountId: Long, grouping: Grouping): String =
                "defaultBudget_%d_%s".format(Locale.ROOT, accountId, grouping)
    }
}
