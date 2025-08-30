package org.totschnig.myexpenses.provider

import androidx.datastore.preferences.core.edit
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_MINIMAL_URI
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal

// We do not test order of results, because current behaviour is hacky and will be refactored
@RunWith(RobolectricTestRunner::class)
class AccountsMinimalTest : BaseTestWithRepository() {
    private val testAccount1 = "Test account"
    private val testAccount2 = "Bank account"
    private val testAccount3 = "Foreign account"

    @Before
    fun setup() {
        insertAccount(testAccount1, accountType = PREDEFINED_NAME_CASH)
        insertAccount(testAccount2, accountType = PREDEFINED_NAME_BANK)
        insertAccount(testAccount3, currency = "DKK")
    }

    fun runQueryTest(
        accountGrouping: AccountGrouping,
        withAggregates: Boolean,
        vararg expected: String
    ) {
        val prefKey = prefHandler.getStringPreferencesKey(PrefKey.ACCOUNT_GROUPING)
        runBlocking {
            dataStore.edit {
                it[prefKey] = accountGrouping.name
            }
        }

        val accounts = contentResolver.query(
            if (withAggregates) ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES else ACCOUNTS_MINIMAL_URI,
            null,
            null,
            null,
            null
        )!!.useAndMapToList {
            AccountMinimal.fromCursor(application, it)
        }.map { it.label }
        assertThat(accounts).containsExactly(*expected)
    }

    @Test
    fun testUngrouped() {
        runQueryTest(
            AccountGrouping.NONE,
            false,
            testAccount1,
            testAccount2,
            testAccount3
        )
    }

    @Test
    fun testGroupedByType() {
        runQueryTest(
            AccountGrouping.TYPE,
            false,
            testAccount1,
            testAccount2,
            testAccount3
        )
    }

    @Test
    fun testUngroupedWithAggregate() {
        runQueryTest(
            AccountGrouping.NONE,
            true,
            testAccount1,
            testAccount2,
            testAccount3,
            currencyContext.homeCurrencyString,
            application.getString(R.string.grand_total)
        )
    }

    @Test
    fun testGroupedByTypeWithAggregate() {
        runQueryTest(
            AccountGrouping.TYPE,
            true,
            testAccount1,
            testAccount2,
            testAccount3,
            currencyContext.homeCurrencyString,
            application.getString(R.string.grand_total)
        )
    }
}