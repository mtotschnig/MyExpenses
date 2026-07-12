package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_FAB_MENU
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_LAST_USED
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard5
import org.totschnig.myexpenses.testutils.cleanup
import kotlin.properties.Delegates

//tests if account is selected in MyExpenses view pager
//and which account is selected in form for new transaction
@TestShard5
class SelectAccountTest : BaseMyExpensesTest() {

    lateinit var accountUSD1: Account
    lateinit var accountUSD2: Account
    lateinit var accountEUR: Account
    var usdAggregate by Delegates.notNull<Long>()

    @Before
    fun fixture() {
        val currencyUnit1 = "USD"
        val currencyUnit2 = "EUR"
        accountUSD1 = buildAccount("USD 1", currency = currencyUnit1)
        accountUSD2 = buildAccount("USD 2", currency = currencyUnit1)
        accountEUR = buildAccount("EUR", currency = currencyUnit2)
        usdAggregate = repository.contentResolver.query(
            TransactionProvider.CURRENCIES_URI,
            arrayOf(KEY_ROWID),
            "$KEY_CODE = ?",
            arrayOf("USD"),
            null
        )!!.use {
            it.moveToFirst()
            -it.getLong(0)
        }
        setLastUsed(accountUSD2, 5)
        setLastUsed(accountEUR, 10)
    }

    private fun setLastUsed(account: Account, lastUsed: Long) {
        repository.updateAccount(account.id) {
            put(KEY_LAST_USED, lastUsed)
        }
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(accountUSD1.id)
            repository.deleteAccount(accountUSD2.id)
            repository.deleteAccount(accountEUR.id)
        }
    }

    @Test
    fun shouldSelectAccount() {
        doTheTest(accountUSD1)
        doTheTest(accountUSD2)
        doTheTest(accountEUR)
    }

    @Test
    fun shouldSelectAccountForCurrency() {
        doTheTest(
            id = usdAggregate,
            currencyForLaunch = "USD",
            currencyForTitle = "USD",
            accountForForm = accountUSD2,
            accountLabelForList = lazy { "USD (Σ)" },
            expectedAccountCount = 3
        )
    }

    @Test
    fun shouldSelectAccountForGrandTotal() {
        doTheTest(
            id = HOME_AGGREGATE_ID,
            currencyForTitle = homeCurrency.code,
            accountForForm = if (homeCurrency.code == "USD") accountUSD2 else accountEUR,
            accountLabelForList = lazy { getString(R.string.grand_total) })
    }


    private fun doTheTest(account: Account) {
        doTheTest(
            id = account.id,
            currencyForTitle = account.currency,
            accountForForm = account
        )
    }

    private fun doTheTest(
        id: Long,
        currencyForTitle: String,
        accountForForm: Account,
        currencyForLaunch: String? = null,
        accountLabelForList: Lazy<String> = lazy { accountForForm.label },
        expectedAccountCount: Int = 4, //3 accounts, and Grand Total
    ) {
        launch(id, currencyForLaunch)
        composeTestRule.waitForIdle()
        assertDataSize(expectedAccountCount)
        checkTitle(accountLabelForList.value, currencyContext[currencyForTitle])
        composeTestRule.onNodeWithTag(TEST_TAG_FAB_MENU).performClick()
        checkAccount(accountForForm.label)
    }
}