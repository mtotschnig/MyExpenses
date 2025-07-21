package org.totschnig.myexpenses.test.provider

import android.content.ContentResolver
import androidx.test.rule.provider.ProviderTestRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.totschnig.myexpenses.provider.AccountInfo
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

//TODO test grouping
@RunWith(Parameterized::class)
class AccountSortOrderTest(private val sortOrder: String, private val expectedData: List<String>) {

    companion object {
        val currency: String = Utils.getSaveDefault().currencyCode
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(KEY_LABEL, listOf("Account 0", "Account 1", "Account 2", currency)),
            arrayOf("$KEY_USAGES DESC", listOf("Account 1", "Account 2", "Account 0", currency)),
            arrayOf("$KEY_LAST_USED DESC", listOf("Account 2", "Account 0", "Account 1", currency))
        )
    }

    @get:Rule
    val providerRule: ProviderTestRule =
        ProviderTestRule.Builder(TransactionProvider::class.java, TransactionProvider.AUTHORITY)
            .build()

    private val resolver: ContentResolver
        get() = providerRule.resolver

    private val testAccounts = arrayOf(
        //TODO setup account type
        AccountInfo(label = "Account 0", type = 1, openingBalance = 0, usages = 1, lastUsed = 5, currency = currency),
        AccountInfo(label = "Account 1", type = 1, openingBalance = 100, usages = 4, lastUsed = 0, currency = currency),
        AccountInfo(label = "Account 2", type = 1, openingBalance = -100, usages = 2, lastUsed = 10, currency = currency)
    )

    private fun insertData() {
        for (account in testAccounts) {
            resolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                account.contentValues
            )
        }
    }

    @After
    fun clearDb() {
        resolver.delete(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null
        )
    }

    @Test
    fun testAggregateQuerySortOrder() {
        insertData()
        resolver.query(
            TransactionProvider.ACCOUNTS_URI.buildUpon()
                .appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES)
                .build(),
            null,
            null,
            null,
            sortOrder
        )!!.useAndAssert {
            val columnIndexLabel = actual.getColumnIndex(KEY_LABEL)
            hasCount(4)
            expectedData.forEach {
                actual.moveToNext()
                hasString(columnIndexLabel, it)
            }
        }
    }
}