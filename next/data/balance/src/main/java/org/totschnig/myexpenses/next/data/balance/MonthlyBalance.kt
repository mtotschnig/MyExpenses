package org.totschnig.myexpenses.next.data.balance

import org.totschnig.myexpenses.next.data.contracts.Contract
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Period the balance is computed for.
 *
 * @param start first day
 * @param end first day after the period, the day the next salary is expected
 * @param isSalaryCycle true, if the period runs from salary to salary, false for the calendar month
 */
data class BalancePeriod(
    val start: LocalDate,
    val end: LocalDate,
    val isSalaryCycle: Boolean,
) {
    operator fun contains(date: LocalDate) = !date.isBefore(start) && date.isBefore(end)

    /** Days from [today] until the end, at least 1 */
    fun daysLeft(today: LocalDate) = ChronoUnit.DAYS.between(today, end).coerceAtLeast(1)

    companion object {
        /**
         * From the last payment of [salary] to the next one. Without salary, the calendar month.
         */
        fun of(salary: Contract?, today: LocalDate): BalancePeriod {
            // Salaries booked in advance do not start a new period before they arrive
            val lastSalary = salary?.transactions?.map { it.date }?.filter { !it.isAfter(today) }?.maxOrNull()
            if (salary != null && lastSalary != null) {
                val next = lastSalary.plus(salary.interval.step)
                // A late salary is expected any day now
                return BalancePeriod(lastSalary, if (next.isAfter(today)) next else today.plusDays(1), true)
            }
            val firstOfMonth = today.withDayOfMonth(1)
            return BalancePeriod(firstOfMonth, firstOfMonth.plusMonths(1), false)
        }
    }
}

/**
 * Transaction of a daily account as input for [MonthlyBalance.compute]. Amount in minor units of
 * the home currency.
 *
 * @param transferAccountId the other account, if this is a transfer
 */
data class BalanceTransaction(
    val id: Long,
    val date: LocalDate,
    val amount: Long,
    val accountId: Long,
    val transferAccountId: Long? = null,
)

/**
 * What came in and went out of the daily accounts in a [period]. Amounts in minor units of the
 * home currency; money going out is negative.
 *
 * @param incomeUpcoming further salaries expected until the end of the period, not booked yet
 * @param contractsUpcoming contract debits expected until the end of the period, not booked yet
 * @param savings money put aside ("Rücklagen"): transfers between daily accounts and other accounts,
 * negative if money was saved, and payments of contracts that are reserves
 * @param savingsUpcoming payments of contracts that are reserves, expected until the end of the period
 */
data class MonthlyBalance(
    val period: BalancePeriod,
    val incomeBooked: Long,
    val incomeUpcoming: Long,
    val contractsBooked: Long,
    val contractsUpcoming: Long,
    val savings: Long,
    val savingsUpcoming: Long = 0,
    val other: Long,
    val incomeTransactionIds: List<Long>,
    val savingsTransactionIds: List<Long>,
    val otherTransactionIds: List<Long>,
) {
    val income: Long get() = incomeBooked + incomeUpcoming

    val contracts: Long get() = contractsBooked + contractsUpcoming

    val reserves: Long get() = savings + savingsUpcoming

    /** Everything going out, as positive amount */
    val expenses: Long get() = -(contracts + reserves + other)

    val available: Long get() = income + contracts + reserves + other

    companion object {
        /**
         * @param transactions of all accounts, only those of [dailyAccountIds] within [period] count
         * @param contracts detected contracts, without those the user dismissed
         * @param salaries see [salaries], the first one determines the period, the others are
         * expected within it
         */
        fun compute(
            period: BalancePeriod,
            transactions: List<BalanceTransaction>,
            dailyAccountIds: Set<Long>,
            contracts: List<Contract>,
            salaries: List<Contract> = emptyList(),
        ): MonthlyBalance {
            val (reserveContracts, expenseContracts) = contracts.filter { !it.isIncome }.partition { it.isReserve }
            val contractTransactionIds = expenseContracts.flatMapTo(HashSet()) { contract -> contract.transactions.map { it.id } }
            val reserveTransactionIds = reserveContracts.flatMapTo(HashSet()) { contract -> contract.transactions.map { it.id } }
            val income = mutableListOf<BalanceTransaction>()
            val contractDebits = mutableListOf<BalanceTransaction>()
            val savings = mutableListOf<BalanceTransaction>()
            val other = mutableListOf<BalanceTransaction>()
            transactions
                .filter { it.accountId in dailyAccountIds && it.date in period }
                .forEach {
                    when {
                        // Moving money between daily accounts, e.g. paying the credit card, changes nothing
                        it.transferAccountId != null ->
                            if (it.transferAccountId !in dailyAccountIds) savings += it

                        it.amount > 0 -> income += it
                        it.id in contractTransactionIds -> contractDebits += it
                        it.id in reserveTransactionIds -> savings += it
                        else -> other += it
                    }
                }
            return MonthlyBalance(
                period = period,
                incomeBooked = income.sumOf { it.amount },
                incomeUpcoming = salaries.upcoming(period, dailyAccountIds),
                contractsBooked = contractDebits.sumOf { it.amount },
                contractsUpcoming = -expenseContracts.upcoming(period, dailyAccountIds),
                savings = savings.sumOf { it.amount },
                savingsUpcoming = -reserveContracts.upcoming(period, dailyAccountIds),
                other = other.sumOf { it.amount },
                incomeTransactionIds = income.map { it.id },
                savingsTransactionIds = savings.map { it.id },
                otherTransactionIds = other.map { it.id }
            )
        }

        /**
         * Sum of the payments still expected in [period] on daily accounts
         */
        private fun List<Contract>.upcoming(period: BalancePeriod, dailyAccountIds: Set<Long>) =
            filter { it.isActive && it.lastTransaction.accountId in dailyAccountIds }
                .sumOf { it.lastAmount * it.upcomingPayments(period) }
    }
}

/**
 * Number of payments still expected in [period]. Includes payments of the period that are
 * a few days late, since they are still going to come.
 */
fun Contract.upcomingPayments(period: BalancePeriod): Int {
    var count = 0
    var date = nextExpectedDate
    while (date.isBefore(period.end)) {
        if (date in period) count++
        date = date.plus(interval.step)
    }
    return count
}
