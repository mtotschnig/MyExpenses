package org.totschnig.myexpenses.test.espresso

import android.content.ContentValues
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import kotlin.properties.Delegates

//tests if account is selected in MyExpenses view pager
//and which account is selected in form for new transaction
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
        repository.contentResolver.update(
            TransactionProvider.ACCOUNTS_URI,
            ContentValues(1).apply {
                put(KEY_LAST_USED, lastUsed)
            },
            "$KEY_ROWID = ?",
            arrayOf(account.id.toString())
        )
    }

    @Test
    fun shouldSelectAccount() {
        doTheTest(accountUSD1)
        doTheTest(accountUSD2)
        doTheTest(accountEUR)
    }

    @Test
    fun shouldSelectAccountForCurrency() {
        doTheTest(usdAggregate, accountUSD2, lazy { "USD" })
    }

    @Test
    fun shouldSelectAccountForGrandTotal() {
        doTheTest(HOME_AGGREGATE_ID, accountEUR, lazy { getString(R.string.grand_total) })
    }


    private fun doTheTest(account: Account) {
        doTheTest(account.id, account)
    }

    private fun doTheTest(
        id: Long,
        accountForForm: Account,
        accountLabelForList: Lazy<String> = lazy { accountForForm.label }
    ) {
        launch(id)
        assertDataSize(5) //3 accounts, USD and Grand Total
        onView(
            allOf(
                withText(accountLabelForList.value),
                withParent(withId(R.id.toolbar))
            )
        )
            .check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.CREATE_COMMAND)).perform(click())
        onView(withId(R.id.Account)).check(matches(withSpinnerText(`is`(accountForForm.label))))
    }
}