package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.first
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod.Companion.translateIfPredefined
import org.totschnig.myexpenses.model2.BudgetExport
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.budgetUri
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfWeekStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.BUDGETS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.BUDGET_ALLOCATIONS_URI
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.util.GroupingInfo
import org.totschnig.myexpenses.util.GroupingNavigator
import org.totschnig.myexpenses.viewmodel.BudgetViewModel.Companion.prefNameForCriteria
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2.Companion.aggregateNeutralPrefKey
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.BudgetProgress
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.DateInfoExtra
import java.time.LocalDate
import java.time.temporal.ChronoUnit


data class BudgetDuration(val start: LocalDate, val end: LocalDate)
data class BudgetPeriod(
    val year: Int?,
    val second: Int?,
    val duration: BudgetDuration,
    val description: String,
)

fun budgetAllocationQueryUri(
    budgetId: Long,
    groupingInfo: GroupingInfo,
): Uri = budgetAllocationQueryUri(
    budgetId,
    if (groupingInfo.grouping == Grouping.NONE) null else groupingInfo.year.toString(),
    if (groupingInfo.grouping == Grouping.NONE) null else groupingInfo.second.toString()
)

fun budgetAllocationQueryUri(
    budgetId: Long,
    year: String?,
    second: String?,
): Uri = BaseTransactionProvider.budgetAllocationQueryUri(budgetId, year, second)

suspend fun Repository.sumLoaderForBudget(
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
        FilterPersistence(dataStore, prefNameForCriteria(budget.id))
    var filterClause = if (period == null) buildDateFilterClauseCurrentPeriod(budget) else
        dateFilterClause(budget.grouping, period.first, period.second)
    val selectionArgs: Array<String>? = filterPersistence.getValue()?.let {
        filterClause += " AND " + it.getSelectionForParts()
        it.getSelectionArgs(true)
    }
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
    budgetUri(budgetId),
    null, null, null, null
)?.use {
    if (it.moveToFirst()) it.getEnum(KEY_GROUPING, Grouping.NONE) else null
}

fun Repository.loadBudget(budgetId: Long): Budget? = contentResolver.query(
    budgetUri(budgetId),
    null, null, null, null
)?.use { cursor ->
    if (cursor.moveToFirst()) budgetCreatorFunction(cursor) else null
}

suspend fun Repository.loadBudgetProgress(budgetId: Long, period: Pair<Int, Int>?) =
    loadBudget(budgetId)?.let { budget ->
        val grouping = budget.grouping
        val groupingInfo = if (grouping == Grouping.NONE) BudgetPeriod(
            year = null,
            second = null,
            duration = BudgetDuration(
                start = budget.start!!,
                end = budget.end!!
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
        val allocated = budgetAllocation(
            budgetId,
            groupingInfo.year,
            groupingInfo.second
        ) ?: 0
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

/**
 * resolves the effective values according to business rules
 */
fun Repository.budgetAllocation(
    budgetId: Long,
    year: Int?,
    second: Int?,
) = contentResolver.query(
    budgetAllocationQueryUri(
        budgetId, year?.toString(), second?.toString()
    ), arrayOf(KEY_BUDGET), null, null, null
)?.use {
    if (it.moveToFirst()) it.getLong(0) else null
}

/**
 * retrieves raw data from database for verification in tests
 */
@VisibleForTesting
fun Repository.budgetAllocation(
    budgetId: Long,
    categoryId: Long,
    period: Pair<Int, Int>?
) = contentResolver.query(
    BUDGET_ALLOCATIONS_URI,
    arrayOf(KEY_BUDGET),
    "$KEY_BUDGETID = ? AND $KEY_CATID = ?" + if (period == null) "" else  "AND $KEY_YEAR = ? AND $KEY_SECOND_GROUP = ?",
    listOfNotNull(budgetId.toString(), categoryId.toString(), period?.first?.toString(), period?.second?.toString()).toTypedArray(),
    null
)?.use {
    if (it.moveToFirst()) it.getLong(0) else null
}

fun Repository.deleteBudget(id: Long) =
    contentResolver.delete(
        budgetUri(id),
        null,
        null
    )

fun Repository.saveBudget(budget: Budget, initialAmount: Long?, uuid: String? = null): Long {
    val contentValues = budget.toContentValues(initialAmount)
    return if (budget.id == 0L) {
        contentValues.put(KEY_UUID, uuid ?: Model.generateUuid())
        contentResolver.insert(BUDGETS_URI, contentValues)
            ?.let { ContentUris.parseId(it) } ?: -1
    } else {
        contentResolver.update(
            budgetUri(budget.id),
            contentValues, null, null
        ).let {
            if (it == 1) budget.id else -1
        }
    }
}

fun Repository.saveBudgetOp(
    budget: Budget,
    initialAmount: Long?,
    uuid: String? = null,
): ContentProviderOperation {
    val contentValues = budget.toContentValues(initialAmount)
    return if (budget.id == 0L) {
        contentValues.put(KEY_UUID, uuid ?: Model.generateUuid())
        ContentProviderOperation.newInsert(BUDGETS_URI)
            .withValues(contentValues)
            .build()
    } else {
        ContentProviderOperation.newUpdate(budgetUri(budget.id))
            .withValues(contentValues)
            .build()
    }
}

suspend fun Repository.importBudget(
    budgetExport: BudgetExport,
    budgetId: Long,
    accountId: Long,
    uuid: String?,
): Long {
    val ops = ArrayList<ContentProviderOperation>()
    if (budgetId != 0L) {
        ops.add(
            ContentProviderOperation.newDelete(BUDGET_ALLOCATIONS_URI)
                .withSelection("$KEY_BUDGETID = ?", arrayOf(budgetId.toString()))
                .build()
        )
    }
    return with(budgetExport) {
        ops.add(
            saveBudgetOp(
                Budget(
                    budgetId,
                    accountId,
                    title,
                    description,
                    currency,
                    grouping,
                    0,
                    start,
                    end,
                    "",
                    isDefault,
                    null
                ), null, uuid
            )
        )
        allocations.forEach {
            ops.add(
                ContentProviderOperation.newInsert(BUDGET_ALLOCATIONS_URI)
                    .withValues(ContentValues().apply {
                        put(KEY_CATID, it.category?.let { ensureCategoryPath(it) } ?: 0L)
                        if (budgetId != 0L) {
                            put(KEY_BUDGETID, budgetId)
                        }
                        put(KEY_YEAR, it.year)
                        put(KEY_SECOND_GROUP, it.second)
                        put(KEY_BUDGET, it.budget)
                        put(KEY_BUDGET_ROLLOVER_PREVIOUS, it.rolloverPrevious)
                        put(KEY_BUDGET_ROLLOVER_NEXT, it.rolloverNext)
                        put(KEY_ONE_TIME, it.oneTime)
                    }).apply {
                        if (budgetId == 0L) {
                            withValueBackReference(KEY_BUDGETID, 0)
                        }
                    }
                    .build())
        }
        val result = contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
        val budgetId = if (budgetId != 0L) budgetId else ContentUris.parseId(result[0].uri!!)
        val filterPersistence = FilterPersistence(dataStore, prefNameForCriteria(budgetId))
        val criteria: List<Criterion> = buildList {
            categoryFilter?.mapNotNull { path ->
                ensureCategoryPath(path)?.let { path.last().label to it }
            }?.let {
                val label = it.joinToString { it.first }
                val ids = it.map { it.second }.toLongArray()
                add(CategoryCriterion(label, *ids))
            }
            partyFilter?.mapNotNull { party ->  requireParty(party)?.let { party to it } }?.let {
                val label = it.joinToString { it.first }
                val ids = it.map { it.second }.toLongArray()
                add(PayeeCriterion(label, *ids))
            }
            methodFilter?.mapNotNull { method ->
                findPaymentMethod(method)?.let { method.translateIfPredefined(context) to it }
            }?.let {
                val label = it.joinToString { it.first }
                val ids = it.map { it.second }.toLongArray()
                add(MethodCriterion(label, *ids))
            }
            statusFilter?.mapNotNull {
                try {
                    CrStatus.valueOf(it)
                } catch (_: Exception) {
                    null
                }
            }?.takeIf { it.isNotEmpty() }?.let {
                add(CrStatusCriterion(it))
            }
            tagFilter?.takeIf { it.isNotEmpty() }?.map {
                it to extractTagId(it)
            }?.let {
                val label = it.joinToString { it.first }
                val ids = it.map { it.second }.toLongArray()
                add(TagCriterion(label, *ids))
            }
            accountFilter?.mapNotNull { accountUuid ->
                findAccountByUuidWithExtraColumn(accountUuid, KEY_LABEL)
            }?.takeIf { it.isNotEmpty() }?.let {
                val label = it.joinToString { it.second!! }
                val ids = it.map { it.first }.toLongArray()
                add(AccountCriterion(label, *ids))
            }
        }
        filterPersistence.persist(criteria)
        budgetId
    }
}
