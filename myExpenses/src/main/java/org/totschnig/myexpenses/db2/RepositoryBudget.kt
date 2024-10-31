package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.net.Uri
import kotlinx.coroutines.flow.first
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfWeekStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getEnumOrNull
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.util.GroupingInfo
import org.totschnig.myexpenses.util.GroupingNavigator
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2.Companion.aggregateNeutralPrefKey
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.BudgetProgress
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.DateInfoExtra
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.temporal.ChronoUnit


data class BudgetDuration(val start: LocalDate, val end: LocalDate)
data class BudgetPeriod(
    val year: Int,
    val second: Int,
    val duration: BudgetDuration,
    val description: String,
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
    groupingInfo: GroupingInfo,
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
    year: String?,
    second: String?,
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

fun Repository.sumLoaderForBudget(
    budget: Budget,
    aggregateNeutral: Boolean,
    period: Pair<Int, Int>?,
): Triple<Uri, String, Array<String>?> {
    val sumBuilder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
    budget.queryParameter?.let {
        sumBuilder.appendQueryParameter(it.first, it.second)
    }
    sumBuilder.appendQueryParameter(
        TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL,
        aggregateNeutral.toString()
    )
    val filterPersistence =
        FilterPersistence(prefHandler, BudgetViewModel.prefNameForCriteria(budget.id), null, false)
    var filterClause = if (period == null) buildDateFilterClauseCurrentPeriod(budget) else
        dateFilterClause(budget.grouping, period.first, period.second)
    val selectionArgs: Array<String>? = if (!filterPersistence.whereFilter.isEmpty) {
        filterClause += " AND " + filterPersistence.whereFilter.getSelectionForParts(
            DatabaseConstants.VIEW_WITH_ACCOUNT
        )
        filterPersistence.whereFilter.getSelectionArgs(true)
    } else null
    return Triple(sumBuilder.build(), filterClause, selectionArgs)
}

private fun buildDateFilterClauseCurrentPeriod(budget: Budget): String {
    val year = "${DatabaseConstants.YEAR} = ${DatabaseConstants.THIS_YEAR}"
    return when (budget.grouping) {
        Grouping.YEAR -> year
        Grouping.DAY -> "$year AND $DAY = ${budget.grouping.queryArgumentForThisSecond}"
        Grouping.WEEK -> "${getYearOfWeekStart()} = ${getThisYearOfWeekStart()} AND ${getWeek()} = ${budget.grouping.queryArgumentForThisSecond}"
        Grouping.MONTH -> "${getYearOfMonthStart()} = ${getThisYearOfMonthStart()} AND ${getMonth()} = ${budget.grouping.queryArgumentForThisSecond}"
        Grouping.NONE -> budget.durationAsSqlFilter()
    }
}

fun dateFilterClause(grouping: Grouping, year: Int, second: Int): String {
    val yearExpression = "${DatabaseConstants.YEAR} = $year"
    return when (grouping) {
        Grouping.YEAR -> yearExpression
        Grouping.DAY -> "$yearExpression AND $DAY = $second"
        Grouping.WEEK -> "${getYearOfWeekStart()} = $year AND ${getWeek()} = $second"
        Grouping.MONTH -> "${getYearOfMonthStart()} = $year AND ${getMonth()} = $second"
        else -> throw IllegalArgumentException()
    }
}

fun Repository.getGrouping(budgetId: Long): Grouping? = contentResolver.query(
    ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId),
    arrayOf(KEY_GROUPING), null, null, null
)?.use {
    if (it.moveToFirst()) it.getEnumOrNull<Grouping>(0) else null
}

suspend fun Repository.loadBudgetProgress(budgetId: Long, period: Pair<Int, Int>?) =
    contentResolver.query(
        TransactionProvider.BUDGETS_URI,
        BudgetViewModel.PROJECTION,
        "${BudgetViewModel.q(DatabaseConstants.KEY_ROWID)} = ?",
        arrayOf(budgetId.toString()),
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val budget = budgetCreatorFunction(cursor)
        val grouping = budget.grouping
        val groupingInfo = if (grouping == Grouping.NONE) BudgetPeriod(
            year = 0,
            second = 0,
            duration = BudgetDuration(
                start = cursor.getLocalDate(KEY_START),
                end = cursor.getLocalDate(KEY_END)
            ),
            description = budget.durationPrettyPrint()
        ) else {
            val dateInfo = DateInfo.load(contentResolver)
            val info = period?.let { GroupingInfo(grouping, it.first, it.second) }
                ?: GroupingNavigator.current(grouping, dateInfo)
            var weekStart: LocalDate? = null

            BudgetPeriod(
                year = info.year,
                second = info.second,
                duration = when (grouping) {
                    Grouping.DAY -> {
                        with(LocalDate.ofYearDay(info.year, info.second)) {
                            BudgetDuration(this, this)
                        }
                    }

                    Grouping.WEEK -> {
                        weekStart = DateInfoExtra.load(context.contentResolver, info).weekStart!!
                        BudgetDuration(
                            weekStart,
                            Grouping.getWeekEndFromStart(weekStart)
                        )
                    }

                    Grouping.MONTH -> with(
                        Grouping.getMonthRange(
                            info.year,
                            info.second,
                            prefHandler.monthStart
                        )
                    ) {
                        BudgetDuration(this.first, this.second)
                    }

                    Grouping.YEAR -> BudgetDuration(
                        LocalDate.ofYearDay(info.year, 1),
                        LocalDate.of(info.year, 12, 31)
                    )

                    else -> throw IllegalStateException()
                },
                description = grouping.getDisplayTitle(
                    context,
                    info.year,
                    info.second,
                    dateInfo,
                    weekStart,
                    true
                )
            )
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
        val aggregateNeutral = dataStore.data.first()[aggregateNeutralPrefKey(budgetId)] == true
        val (sumUri, sumSelection, sumSelectionArguments) =
            sumLoaderForBudget(budget, aggregateNeutral, period)
        val spent = contentResolver.query(
            sumUri,
            arrayOf(KEY_SUM_EXPENSES),
            sumSelection,
            sumSelectionArguments,
            null
        ).use {
            if (it?.moveToFirst() != true) 0 else it.getLong(0)
        }

        BudgetProgress(
            budget.title,
            budget.currency,
            groupingInfo,
            allocated,
            -spent,
            totalDays,
            currentDay
        )
    }

fun Repository.deleteBudget(id: Long) =
    contentResolver.delete(
        ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, id),
        null,
        null
    )
