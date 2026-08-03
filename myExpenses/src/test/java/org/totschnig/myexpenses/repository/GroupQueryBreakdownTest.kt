package org.totschnig.myexpenses.repository

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.db2.storeExchangeRate
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.KEY_PERIOD_END
import org.totschnig.myexpenses.provider.KEY_SUM
import org.totschnig.myexpenses.provider.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_BREAKDOWN_BY_ACCOUNT
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@RunWith(ParameterizedRobolectricTestRunner::class)
class GroupQueryBreakdownTest(private val grouping: Grouping) : BaseTestWithRepository() {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Grouping: {0}")
        fun params() = Grouping.entries.map { arrayOf(it) }
    }

    private var staticAccountId: Long = 0
    private var dynamicAccountId: Long = 0
    private var categoryId: Long = 0

    @Before
    fun setup() {
        DatabaseConstants.buildLocalized(Locale.getDefault(), application, prefHandler)
        categoryId = repository.saveCategory(Category(label = "Category", type = FLAG_NEUTRAL))!!

        staticAccountId = insertAccount("Static Account", currency = "DKK")
        repository.storeExchangeRate(staticAccountId, 1.1, "DKK", currencyContext.homeCurrencyString)

        dynamicAccountId = insertAccount("Dynamic Account", currency = "DKK", dynamic = true)
    }

    @Test
    fun testBreakdownByAccount() {
        val today = LocalDate.now()
        val transactionDate = today.atStartOfDay()
        
        // Insert transaction for static account
        repository.insertTransaction(staticAccountId, 100, categoryId = categoryId, date = transactionDate)

        // Insert transaction for dynamic account and save price
        repository.insertTransaction(dynamicAccountId, 200, categoryId = categoryId, date = transactionDate)
        repository.savePrice(currencyContext.homeCurrencyString, "DKK", today, ExchangeRateSource.User, 1.2)

        val uri = BaseTransactionProvider.groupingUriBuilder(grouping)
            .appendQueryParameter(QUERY_PARAMETER_BREAKDOWN_BY_ACCOUNT, "true")
            .build()

        contentResolver.query(uri, null, null, null, null).useAndAssert {
            hasCount(2)

            val expectedPeriodEnd = calculateExpectedPeriodEnd(grouping, today)
            println("expectedPeriodEnd for $grouping: $expectedPeriodEnd")

            forEach {
                val accountId = actual.getLong(actual.getColumnIndexOrThrow(KEY_ACCOUNTID))
                if (accountId == staticAccountId) {
                    hasString(KEY_CURRENCY, "DKK")
                    hasDouble(KEY_EXCHANGE_RATE, 1.1)
                    hasLong(KEY_SUM, 100)
                    hasLong(KEY_SUM_INCOME, 110)
                } else if (accountId == dynamicAccountId) {
                    hasString(KEY_CURRENCY, "DKK")
                    hasDouble(KEY_EXCHANGE_RATE, 1.2)
                    hasLong(KEY_SUM, 200)
                    hasLong(KEY_SUM_INCOME, 240)
                }
                hasString(KEY_PERIOD_END, expectedPeriodEnd)
            }
        }
    }

    private fun calculateExpectedPeriodEnd(grouping: Grouping, date: LocalDate): String {
        return when (grouping) {
            Grouping.NONE -> LocalDate.now().toString()
            Grouping.DAY -> date.toString()
            Grouping.WEEK -> {
                val nextWeekEnd = DatabaseConstants.nextWeekEndSqlite
                // sqlite weekday: 0=Sunday, 1=Monday, ..., 6=Saturday
                // LocalDate DayOfWeek: 1=Monday, ..., 7=Sunday
                val targetDayOfWeek = if (nextWeekEnd == 0) 7 else nextWeekEnd
                var current = date
                while (current.dayOfWeek.value != targetDayOfWeek) {
                    current = current.plusDays(1)
                }
                current.toString()
            }
            Grouping.MONTH -> {
                val monthStart = DatabaseConstants.monthStartsOn
                if (monthStart == 1) {
                    date.with(TemporalAdjusters.lastDayOfMonth()).toString()
                } else {
                    // Logic for custom month start:
                    // date(date, 'unixepoch', 'localtime', '-${monthDelta} day', 'start of month', '+1 month', '-1 day', '+${monthDelta} day')
                    val monthDelta = monthStart - 1
                    date.minusDays(monthDelta.toLong())
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .plusMonths(1)
                        .minusDays(1)
                        .plusDays(monthDelta.toLong())
                        .toString()
                }
            }
            Grouping.YEAR -> date.with(TemporalAdjusters.lastDayOfYear()).toString()
        }
    }
}
