package org.totschnig.myexpenses.repository

import android.content.ContentUris
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.IAccount
import org.totschnig.myexpenses.provider.AccountInfo
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.AGGREGATE_HOME_CURRENCY_CODE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.shared_test.CursorSubject

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
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0, "USD")
        testAccountId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                testAccount.contentValues
            )!!
        )
    }

    private fun uriBuilder(accountInfo: IAccount, aggregateNeutral: Boolean) =
        TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon().apply {
            accountInfo.queryParameter?.let { appendQueryParameter(it.first, it.second) }
        }.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL, aggregateNeutral.toString())

    @Test
    fun testSumQueriesAcount() {
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
        )!!.use {
            with(CursorSubject.assertThat(it)) {
                movesToFirst()
                hasColumnCount(projection.size)
                isNull(0)
                isNull(1)
            }
        }
    }
}