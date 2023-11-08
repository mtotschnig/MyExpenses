package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLocalDateIfExists
import org.totschnig.myexpenses.provider.getLong
import java.time.LocalDate

sealed interface HeaderDataResult {
    val account: PageAccount
    fun calculateGroupId(transaction: Transaction2) = account.grouping.calculateGroupId(transaction.year, getSecond(transaction))
    private fun getSecond(transaction: Transaction2) = when(account.grouping) {
        Grouping.DAY -> transaction.day
        Grouping.WEEK -> transaction.week
        Grouping.MONTH -> transaction.month
        else -> 0
    }
}

data class HeaderDataEmpty(override val account: PageAccount): HeaderDataResult
data class HeaderDataError(override val account: PageAccount): HeaderDataResult

data class HeaderData(
    override val account: PageAccount,
    val groups: Map<Int, HeaderRow>,
    val dateInfo: DateInfo,
    val isFiltered: Boolean
): HeaderDataResult {

    companion object {
        fun fromSequence(account: PageAccount, sequence: Sequence<Cursor>): Map<Int, HeaderRow> =
            buildMap {
                var previousBalance = account.openingBalance
                for (cursor in sequence) {
                    val value = HeaderRow.rowFromCursor(previousBalance, account.currencyUnit, cursor)
                    put(account.grouping.calculateGroupId(value.year, value.second), value)
                    previousBalance = value.interimBalance.amountMinor
                }
            }
    }
}

data class HeaderRow(
    val year: Int,
    val second: Int,
    val incomeSum: Money,
    val expenseSum: Money,
    val transferSum: Money,
    val previousBalance: Money,
    val delta: Money,
    val interimBalance: Money,
    val weekStart: LocalDate?
) {

    companion object {

        fun create(
            year: Int,
            second: Int,
            currency: CurrencyUnit,
            incomeSum: Long,
            expenseSum: Long,
            transferSum: Long,
            previousBalance: Long,
            weekStart: LocalDate?
        ): HeaderRow {
            val delta = incomeSum + expenseSum + transferSum
            return HeaderRow(
                year,
                second,
                Money(currency, incomeSum),
                Money(currency, expenseSum),
                Money(currency, transferSum),
                Money(currency, previousBalance),
                Money(currency, delta),
                Money(currency, previousBalance + delta),
                weekStart
            )
        }

        fun rowFromCursor(previousBalance: Long, currency: CurrencyUnit, cursor: Cursor) = create(
            year = cursor.getInt(KEY_YEAR),
            second = cursor.getInt(KEY_SECOND_GROUP),
            currency = currency,
            incomeSum = cursor.getLong(KEY_SUM_INCOME),
            expenseSum = cursor.getLong(KEY_SUM_EXPENSES),
            transferSum = cursor.getLong(KEY_SUM_TRANSFERS),
            previousBalance = previousBalance,
            weekStart = cursor.getLocalDateIfExists(KEY_WEEK_START)
        )
    }
}

data class BudgetData(val budgetId: Long, val data: List<BudgetRow>)

data class BudgetRow(val headerId: Int, val amount: Long, val oneTime: Boolean)
