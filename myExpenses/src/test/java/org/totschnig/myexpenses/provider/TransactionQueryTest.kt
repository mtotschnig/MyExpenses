package org.totschnig.myexpenses.provider

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_SEARCH
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class TransactionQueryTest: BaseTestWithRepository() {

    @Test
    fun testSearchUriForHome() {
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI
                .buildUpon()
                .appendQueryParameter(QUERY_PARAMETER_SEARCH, "1")
                .build(),
            null,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(0)
        }
    }
}