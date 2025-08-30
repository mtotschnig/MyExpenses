package org.totschnig.myexpenses.provider

import androidx.core.content.contentValuesOf
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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_MINIMAL_URI
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal

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
        accountGrouping: AccountGrouping = AccountGrouping.NONE,
        withAggregates: Boolean = false,
        withCustomSort: Boolean = false,
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
            if (withCustomSort) KEY_SORT_KEY else null
        )!!.useAndMapToList {
            AccountMinimal.fromCursor(application, it)
        }.map { it.label }
        if (withCustomSort) {
            assertThat(accounts).containsExactly(*expected).inOrder()
        } else {
            assertThat(accounts).containsExactly(*expected)
        }
    }

    //Grouping does not influence the result, we just test there are is no side effect,
    // that would make the query fail
    @Test
    fun testUngrouped() {
        runQueryTest(
            accountGrouping = AccountGrouping.NONE,
            withAggregates = false,
            withCustomSort = false,
            testAccount1,
            testAccount2,
            testAccount3
        )
    }

    //Grouping does not influence the result, we just test there are is no side effect,
    // that would make the query fail
    @Test
    fun testGroupedByType() {
        runQueryTest(
            accountGrouping = AccountGrouping.TYPE,
            withAggregates = false,
            withCustomSort = false,
            testAccount1,
            testAccount2,
            testAccount3
        )
    }

    @Test
    fun testSorted() {
        val customSort = arrayOf(testAccount2, testAccount1, testAccount3)
        customSort.forEachIndexed { index, s ->
            contentResolver.update(TransactionProvider.ACCOUNTS_URI,
                contentValuesOf(KEY_SORT_KEY to index),
                "$KEY_LABEL=?",
                arrayOf(s))
        }
        runQueryTest(
            accountGrouping = AccountGrouping.TYPE,
            withAggregates = false,
            withCustomSort = true,
            expected = customSort
        )
    }

    @Test
    fun testUngroupedWithAggregate() {
        runQueryTest(
            accountGrouping = AccountGrouping.NONE,
            withAggregates = true,
            withCustomSort = false,
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
            accountGrouping = AccountGrouping.TYPE,
            withAggregates = true,
            withCustomSort = false,
            testAccount1,
            testAccount2,
            testAccount3,
            currencyContext.homeCurrencyString,
            application.getString(R.string.grand_total)
        )
    }
}