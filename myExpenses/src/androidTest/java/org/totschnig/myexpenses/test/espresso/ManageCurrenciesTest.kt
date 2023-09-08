package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCurrencies
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create

class ManageCurrenciesTest : BaseUiTest<ManageCurrencies>() {
    @get:Rule
    var scenarioRule = ActivityScenarioRule(ManageCurrencies::class.java)

    @Before
    fun setup() {
        testScenario = scenarioRule.scenario
    }

    @Test
    fun changeOfFractionDigitsWithUpdateShouldKeepTransactionSum() {
        testHelper(true)
    }

    @Test
    fun changeOfFractionDigitsWithoutUpdateShouldChangeTransactionSum() {
        testHelper(false)
    }

    private fun getTotalAccountBalance(accountId: Long) =
        repository.loadAccount(accountId)!!.openingBalance + repository.getTransactionSum(accountId)

    private fun testHelper(withUpdate: Boolean) {
        val appComponent = app.appComponent
        val currencyContext = appComponent.currencyContext()
        val currencyUnit = currencyContext[CURRENCY_CODE]
        val account = Account(label = "TEST ACCOUNT", openingBalance = 5000L, currency = CURRENCY_CODE)
        val accountId = repository.createAccount(account).id
        val op = Transaction.getNewInstance(accountId, currencyUnit)
        op.amount = Money(currencyUnit, -1200L)
        op.save(contentResolver)
        val before = getTotalAccountBalance(accountId)
        assertThat(before).isEqualTo(3800)
        val currency = create(CURRENCY_CODE, targetContext)
        onData(Matchers.`is`(currency))
            .inAdapterView(withId(android.R.id.list)).perform(click())
        onView(withId(R.id.edt_currency_fraction_digits))
            .perform(replaceText("3"), closeSoftKeyboard())
        if (withUpdate) {
            onView(withId(R.id.checkBox)).perform(click())
        }
        onView(withId(android.R.id.button1)).perform(click())
        onData(Matchers.`is`(currency))
            .inAdapterView(withId(android.R.id.list)).check(matches(withText(containsString("3"))))
        val after = getTotalAccountBalance(accountId)
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