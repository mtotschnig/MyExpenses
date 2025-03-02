package org.totschnig.myexpenses.provider

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class PricesTest: BaseTestWithRepository() {

    @Before
    fun setup() {
        repository.savePrice(
            base = "EUR",
            commodity = "USD",
            date = LocalDate.now(),
            source = ExchangeRateApi.Frankfurter,
            value = 1.5
        )
        repository.savePrice(
            base = "EUR",
            commodity = "USD",
            date = LocalDate.now().minusDays(1),
            source = ExchangeRateSource.User,
            value = 1.6
        )
    }

    @Test
    fun testQueryWithSelection() {
        contentResolver.query(
            TransactionProvider.PRICES_URI,
            arrayOf(KEY_VALUE),
            "$KEY_SOURCE != ?",
            arrayOf(ExchangeRateSource.User.name),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasDouble(0, 1.5)
        }
    }
}