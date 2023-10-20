package org.totschnig.myexpenses.db2

import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.BudgetProgress
import org.totschnig.myexpenses.viewmodel.data.DateInfo2
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class BudgetDuration(val start: LocalDate, val end: LocalDate)
class BudgetPeriod(val year: Int, val second: Int, val duration: BudgetDuration)

fun Repository.loadBudgetProgress(budgetId: Long): BudgetProgress? {
    return contentResolver.query(
        TransactionProvider.BUDGETS_URI,
        BudgetViewModel.PROJECTION,
        "${BudgetViewModel.q(DatabaseConstants.KEY_ROWID)} = ?",
        arrayOf(budgetId.toString()),
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val currency = cursor.getString(DatabaseConstants.KEY_CURRENCY)
        val currencyUnit = if (currency == DataBaseAccount.AGGREGATE_HOME_CURRENCY_CODE)
            homeCurrencyProvider.homeCurrencyUnit else currencyContext.get(currency)
        val accountId = cursor.getLong(DatabaseConstants.KEY_ACCOUNTID)
        val title = cursor.getString(DatabaseConstants.KEY_TITLE)
        val grouping = cursor.getEnum(DatabaseConstants.KEY_GROUPING, Grouping.NONE)
        val groupingInfo: BudgetPeriod = if (grouping == Grouping.NONE) {
            BudgetPeriod(
                year = 0,
                second = 0,
                duration = BudgetDuration(
                    start = LocalDate.parse(cursor.getString(KEY_START)),
                    end = LocalDate.parse(cursor.getString(KEY_END))
                )
            )
        } else {
            contentResolver.query(
                TransactionProvider.DUAL_URI,
                arrayOf(
                    "${DatabaseConstants.getThisYearOfWeekStart()} AS ${DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START}",
                    "${DatabaseConstants.getThisYearOfMonthStart()} AS ${DatabaseConstants.KEY_THIS_YEAR_OF_MONTH_START}",
                    "${DatabaseConstants.THIS_YEAR} AS ${DatabaseConstants.KEY_THIS_YEAR}",
                    "${DatabaseConstants.getThisMonth()} AS ${DatabaseConstants.KEY_THIS_MONTH}",
                    "${DatabaseConstants.getThisWeek()} AS ${DatabaseConstants.KEY_THIS_WEEK}",
                    "${DatabaseConstants.THIS_DAY} AS ${DatabaseConstants.KEY_THIS_DAY}"
                ),
                null, null, null, null
            ).use { dateInfoCursor ->
                if (dateInfoCursor?.moveToFirst() != true) return null
                with(DateInfo2.fromCursor(dateInfoCursor)) {
                    Timber.i("DateInfo2: %s", this)
                    BudgetPeriod(
                        year = when (grouping) {
                            Grouping.WEEK -> yearOfWeekStart
                            Grouping.MONTH -> yearOfMonthStart
                            else -> year
                        },
                        second = when (grouping) {
                            Grouping.DAY -> day
                            Grouping.WEEK -> week
                            Grouping.MONTH -> month
                            else -> 0
                        },
                        duration = when (grouping) {
                            Grouping.DAY -> {
                                val today = LocalDate.ofYearDay(year, day)
                                with(today) {
                                    BudgetDuration(this, this)
                                }
                            }
                            Grouping.WEEK -> {
                                contentResolver.query(
                                    TransactionProvider.DUAL_URI,
                                    arrayOf(
                                        DbUtils.weekStartFromGroupSqlExpression(yearOfWeekStart, week)
                                    ), null, null, null
                                ).use {
                                    if (it?.moveToFirst() != true) return null
                                        BudgetDuration(
                                            LocalDate.ofEpochDay(it.getLong(0)),
                                            LocalDate.ofEpochDay(it.getLong(1))
                                        )
                                }
                            }
                            Grouping.MONTH -> with(Grouping.getMonthRange(yearOfMonthStart, month, prefHandler.monthStart)) {
                                BudgetDuration(first, second)
                            }
                            Grouping.YEAR -> BudgetDuration(
                                LocalDate.ofYearDay(year, 1),
                                LocalDate.of(year, 12, 31)
                            )
                            else -> throw IllegalStateException()
                        }
                    )
                }
            }
        }
        val totalDays = ChronoUnit.DAYS.between(groupingInfo.duration.start, groupingInfo.duration.end) + 1
        val currentDay = ChronoUnit.DAYS.between(groupingInfo.duration.start, LocalDate.now()) + 1
        BudgetProgress(
            title, 6000, 3000, totalDays, currentDay
        )
    }
}