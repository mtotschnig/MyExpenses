package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.adevinta.android.barista.interaction.BaristaCheckboxInteractions
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCurrencies
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.PreferencesCurrencyContext
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create


@TestShard3
class ManageCurrenciesTest : BaseUiTest<ManageCurrencies>() {

    @get:Rule
    var scenarioRule = ActivityScenarioRule(ManageCurrencies::class.java)


    lateinit var account: Account

    @Before
    fun setup() {
        testScenario = scenarioRule.scenario
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
            PreferencesCurrencyContext.resetFractionDigits(prefHandler, CURRENCY_CODE)
        }
    }

    @Test
    fun changeOfFractionDigitsWithUpdateShouldKeepTransactionSum() {
        testHelper(true)
    }

    @Test
    fun changeOfFractionDigitsWithoutUpdateShouldChangeTransactionSum() {
        testHelper(false)
    }

    private fun getTotalAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance + repository.getTransactionSum(account)

    private fun testHelper(withUpdate: Boolean) {
        val appComponent = app.appComponent
        val currencyContext = appComponent.currencyContext()
        val currencyUnit = currencyContext[CURRENCY_CODE]
        account = repository.createAccount(
            Account(
                label = "TEST ACCOUNT",
                openingBalance = 5000L,
                currency = CURRENCY_CODE,
                type = repository.findAccountType(PREDEFINED_NAME_CASH)!!
            )
        )
        val op = Transaction.getNewInstance(account.id, currencyUnit)
        op.amount = Money(currencyUnit, -1200L)
        op.save(contentResolver)
        val before = getTotalAccountBalance(account)
        assertThat(before).isEqualTo(3800)
        val currency = create(CURRENCY_CODE, targetContext)
        onData(Matchers.`is`(currency))
            .inAdapterView(withId(android.R.id.list)).perform(click())
        onView(withId(R.id.edt_currency_fraction_digits))
            .perform(scrollTo(), replaceText("3"))
        onView(withId(R.id.checkBox)).perform(scrollTo())
        if (withUpdate) {
            BaristaCheckboxInteractions.check(R.id.checkBox)
        } else {
            BaristaCheckboxInteractions.uncheck(R.id.checkBox)
        }
        onView(withId(android.R.id.button1)).perform(click())
        onData(Matchers.`is`(currency))
            .inAdapterView(withId(android.R.id.list)).check(matches(withText(containsString("3"))))
        val after = getTotalAccountBalance(account)
        if (withUpdate) {
            assertThat(after).isEqualTo(before * 10)
        } else {
            assertThat((after)).isEqualTo(before)
        }
    }

    companion object {
        private const val CURRENCY_CODE = "EUR"
    }
}