package org.totschnig.myexpenses.repository

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.model2.IAccount
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.AGGREGATE_HOME_CURRENCY_CODE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

/**
 * For the moment, this tests if the queries are valid,
 * TODO test the calculations
 */
@RunWith(RobolectricTestRunner::class)
class SumQueryTest : BaseTestWithRepository() {

    private var testAccountId: Long = 0
    private val account = object : IAccount {
        override val accountId = testAccountId
        override val currency = "USD"

    }
    private val currency: IAccount = object : IAccount {
        override val accountId = -1L
        override val currency = "USD"
    }
    private val grandTotal: IAccount = object : IAccount {
        override val accountId = HOME_AGGREGATE_ID
        override val currency = AGGREGATE_HOME_CURRENCY_CODE
    }

    @Before
    fun setup() {
        testAccountId = insertAccount("Test account")
    }

    private fun uriBuilder(accountInfo: IAccount, aggregateNeutral: Boolean) =
        TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon().apply {
            accountInfo.queryParameter?.let { appendQueryParameter(it.first, it.second) }
        }.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL, aggregateNeutral.toString())

    @Test
    fun testSumQueriesAccount() {
        runAndVerifyQuery(account, false)
    }

    @Test
    fun testSumQueriesCurrency() {
        runAndVerifyQuery(currency, false)
    }

    @Test
    fun testSumQueriesGrandTotal() {
        runAndVerifyQuery(grandTotal, false)
    }

    @Test
    fun testSumQueriesAggregateNeutralAccount() {
        runAndVerifyQuery(account, true)
    }

    @Test
    fun testSumQueriesAggregateNeutralCurrency() {
        runAndVerifyQuery(currency, true)
    }

    @Test
    fun testSumQueriesAggregateNeutralGrandTotal() {
        runAndVerifyQuery(grandTotal, true)
    }

    private fun runAndVerifyQuery(accountInfo: IAccount, aggregateNeutral: Boolean) {
        val projection = if (aggregateNeutral) arrayOf(KEY_SUM_EXPENSES)
        else arrayOf(KEY_SUM_EXPENSES, KEY_SUM_INCOME)
        contentResolver.query(
            uriBuilder(accountInfo, aggregateNeutral).build(),
            projection, null, null, null
        ).useAndAssert {
            movesToFirst()
            hasColumnCount(projection.size)
            isNull(0)
            isNull(1)
        }
    }
}