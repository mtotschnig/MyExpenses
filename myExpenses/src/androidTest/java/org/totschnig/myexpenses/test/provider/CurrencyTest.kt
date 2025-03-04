package org.totschnig.myexpenses.test.provider

import junit.framework.TestCase
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import java.lang.IllegalArgumentException

class CurrencyTest : BaseDbTest() {
    private val TEST_CURRENCY = CurrencyInfo("Bitcoin", "BTC")
    private val TEST_ACCOUNT = AccountInfo("Account 0", AccountType.CASH, 0, TEST_CURRENCY.code)

    fun testShouldNotDeleteFrameworkCurrency() {
        try {
            getMockContentResolver().delete(
                TransactionProvider.CURRENCIES_URI.buildUpon().appendPath("EUR").build(), null, null
            )
            fail("Expected deletion of framework currency to fail")
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun testShouldDeleteUnUsedCurrency() {
        mDb
            .insert(
                DatabaseConstants.TABLE_CURRENCIES,
                TEST_CURRENCY.getContentValues()
            )
        val result = getMockContentResolver().delete(
            TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(TEST_CURRENCY.code).build(),
            null,
            null
        )
        TestCase.assertEquals(1, result)
    }

    fun testShouldNotDeleteUsedCurrency() {
        mDb
            .insert(
                DatabaseConstants.TABLE_CURRENCIES,
                TEST_CURRENCY.getContentValues()
            )
        mDb
            .insert(
                DatabaseConstants.TABLE_ACCOUNTS,
                TEST_ACCOUNT.contentValues
            )
        val result = getMockContentResolver().delete(
            TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(TEST_CURRENCY.code).build(),
            null,
            null
        )
        TestCase.assertEquals(0, result)
    }
}
