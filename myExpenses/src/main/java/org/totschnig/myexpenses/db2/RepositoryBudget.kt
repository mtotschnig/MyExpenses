package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfWeekStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.util.toDayOfWeek
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2.Companion.aggregateNeutralPrefKey
import org.totschnig.myexpenses.viewmodel.DistributionViewModelBase
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.BudgetProgress
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale


class BudgetDuration(val start: LocalDate, val end: LocalDate)
class BudgetPeriod(
    val year: Int,
    val second: Int,
    val duration: BudgetDuration,
    val description: String
)

fun budgetAllocationUri(budgetId: Long, categoryId: Long) = ContentUris.withAppendedId(
    ContentUris.withAppendedId(
        TransactionProvider.BUDGETS_URI,
        budgetId
    ),
    categoryId
)

fun budgetAllocationQueryUri(
    budgetId: Long,
    categoryId: Long,
    groupingInfo: DistributionViewModelBase.GroupingInfo
): Uri = budgetAllocationQueryUri(
    budgetId,
    categoryId,
    groupingInfo.grouping,
    groupingInfo.year.toString(),
    groupingInfo.second.toString()
)

fun budgetAllocationQueryUri(
    budgetId: Long,
    categoryId: Long,
    grouping: Grouping,
    year: String,
    second: String
): Uri = with(budgetAllocationUri(budgetId, categoryId)) {
    if (grouping != Grouping.NONE) {
        val builder = buildUpon()
        builder.appendQueryParameter(KEY_YEAR, year)
        if (grouping != Grouping.YEAR) {
            builder.appendQueryParameter(KEY_SECOND_GROUP, second)
        }
        builder.build()
    } else this
}

fun Repository.sumLoaderForBudget(budget: Budget, aggregateNeutral: Boolean): Triple<Uri, String, Array<String>?> {
    val sumBuilder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
    sumBuilder.appendQueryParameter(KEY_TYPE, FLAG_EXPENSE.toString())
    budget.queryParameter?.let {
        sumBuilder.appendQueryParameter(it.first, it.second)
    }
    sumBuilder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL, aggregateNeutral.toString())
    val filterPersistence =
        FilterPersistence(prefHandler, BudgetViewModel.prefNameForCriteria(budget.id), null, false)
    var filterClause = buildDateFilterClause(budget)
    val selectionArgs: Array<String>? = if (!filterPersistence.whereFilter.isEmpty) {
        filterClause += " AND " + filterPersistence.whereFilter.getSelectionForParts(
            DatabaseConstants.VIEW_WITH_ACCOUNT
        )
        filterPersistence.whereFilter.getSelectionArgs(true)
    } else null
    return Triple(sumBuilder.build(), filterClause, selectionArgs)
}

private fun buildDateFilterClause(budget: Budget): String {
    val year = "${DatabaseConstants.YEAR} = ${DatabaseConstants.THIS_YEAR}"
    return when (budget.grouping) {
        Grouping.YEAR -> year
        Grouping.DAY -> "$year AND $DAY = ${budget.grouping.queryArgumentForThisSecond}"
        Grouping.WEEK -> "${getYearOfWeekStart()} = ${getThisYearOfWeekStart()} AND ${getWeek()} = ${budget.grouping.queryArgumentForThisSecond}"
        Grouping.MONTH -> "${getYearOfMonthStart()} = ${getThisYearOfMonthStart()} AND ${getMonth()} = ${budget.grouping.queryArgumentForThisSecond}"
        Grouping.NONE -> budget.durationAsSqlFilter()
    }
}


suspend fun Repository.loadBudgetProgress(budgetId: Long): BudgetProgress? = contentResolver.query(
    TransactionProvider.BUDGETS_URI,
    BudgetViewModel.PROJECTION,
    "${BudgetViewModel.q(DatabaseConstants.KEY_ROWID)} = ?",
    arrayOf(budgetId.toString()),
    null
)?.use { cursor ->
    if (!cursor.moveToFirst()) return null
    val budget = budgetCreatorFunction(cursor)
    val grouping = budget.grouping
    val groupingInfo: BudgetPeriod = if (grouping == Grouping.NONE) {
        BudgetPeriod(
            year = 0,
            second = 0,
            duration = BudgetDuration(
                start = cursor.getLocalDate(KEY_START),
                end = cursor.getLocalDate(KEY_END)
            ),
            description = budget.durationPrettyPrint()
        )
    } else {
        contentResolver.query(
            TransactionProvider.DUAL_URI,
            arrayOf(
                "${getThisYearOfWeekStart()} AS ${DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START}",
                "${getThisYearOfMonthStart()} AS ${DatabaseConstants.KEY_THIS_YEAR_OF_MONTH_START}",
                "${DatabaseConstants.THIS_YEAR} AS ${DatabaseConstants.KEY_THIS_YEAR}",
                "${DatabaseConstants.getThisMonth()} AS ${DatabaseConstants.KEY_THIS_MONTH}",
                "${DatabaseConstants.getThisWeek()} AS ${DatabaseConstants.KEY_THIS_WEEK}",
                "${DatabaseConstants.THIS_DAY} AS ${DatabaseConstants.KEY_THIS_DAY}"
            ),
            null, null, null, null
        ).use { dateInfoCursor ->
            if (dateInfoCursor?.moveToFirst() != true) return null
            with(DateInfo.fromCursor(dateInfoCursor)) {
                val weekStartDay =
                    prefHandler.weekStartWithFallback(Locale.getDefault()).toDayOfWeek
                val today = LocalDate.ofYearDay(year, day)
                val weekStart = today.with(TemporalAdjusters.previousOrSame(weekStartDay))
                val year = when (grouping) {
                    Grouping.WEEK -> yearOfWeekStart
                    Grouping.MONTH -> yearOfMonthStart
                    else -> year
                }
                val second = when (grouping) {
                    Grouping.DAY -> day
                    Grouping.WEEK -> week
                    Grouping.MONTH -> month
                    else -> 0
                }
                BudgetPeriod(
                    year = year,
                    second = second,
                    duration = when (grouping) {
                        Grouping.DAY -> {
                            today.dayOfWeek
                            with(today) {
                                BudgetDuration(this, this)
                            }
                        }

                        Grouping.WEEK -> {
                            BudgetDuration(
                                weekStart,
                                Grouping.getWeekEndFromStart(weekStart)
                            )
                        }

                        Grouping.MONTH -> with(
                            Grouping.getMonthRange(
                                yearOfMonthStart,
                                month,
                                prefHandler.monthStart
                            )
                        ) {
                            BudgetDuration(this.first, this.second)
                        }

                        Grouping.YEAR -> BudgetDuration(
                            LocalDate.ofYearDay(year, 1),
                            LocalDate.of(year, 12, 31)
                        )

                        else -> throw IllegalStateException()
                    },
                    description = grouping.getDisplayTitle(
                        context,
                        year,
                        second,
                        this,
                        weekStart,
                        true
                    )
                )
            }
        }
    }
    val totalDays =
        ChronoUnit.DAYS.between(groupingInfo.duration.start, groupingInfo.duration.end) + 1
    val currentDay = ChronoUnit.DAYS.between(groupingInfo.duration.start, LocalDate.now()) + 1
    val allocated = contentResolver.query(
        budgetAllocationQueryUri(
            budgetId, 0, grouping, groupingInfo.year.toString(), groupingInfo.second.toString()
        ), null, null, null, null
    ).use {
        if (it?.moveToFirst() != true) 0 else BudgetAllocation.fromCursor(it).totalAllocated
    }
    val aggregateNeutral = dataStore.data.first()[aggregateNeutralPrefKey(budgetId)] ?: false
    val (sumUri, sumSelection, sumSelectionArguments) = sumLoaderForBudget(budget, aggregateNeutral)
    val spent = contentResolver.query(sumUri, arrayOf(KEY_SUM_EXPENSES), sumSelection, sumSelectionArguments, null).use {
        if (it?.moveToFirst() != true) 0 else it.getLong(0)
    }

    BudgetProgress(
        budget.title, budget.currencyUnit, groupingInfo.description, allocated, -spent, totalDays, currentDay
    )
}