package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCurrencies
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference


class ManageCurrenciesTest : BaseUiTest() {
    @get:Rule
    var scenarioRule = ActivityScenarioRule(ManageCurrencies::class.java)

    @Test
    fun changeOfFractionDigitsWithUpdateShouldKeepTransactionSum() {
        testHelper(true)
    }

    @Test
    fun changeOfFractionDigitsWithoutUpdateShouldChangeTransactionSum() {
        testHelper(false)
    }

    private fun testHelper(withUpdate: Boolean) {
        val appComponent = app.appComponent
        val currencyContext = appComponent.currencyContext()
        val currencyUnit = currencyContext[CURRENCY_CODE]
        val account = Account("TEST ACCOUNT", currencyUnit, 5000L, "", AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        waitForAdapter()
        try {
            val op = Transaction.getNewInstance(account.id)
            op.amount = Money(currencyUnit, -1200L)
            op.save()
            val before = account.totalBalance
            assertThat(before.amountMajor).isEqualByComparingTo(BigDecimal(38))
            val currency = AtomicReference<Currency>()
            testScenario.onActivity { activity: ProtectedFragmentActivity? -> currency.set(create(CURRENCY_CODE, activity)) }
            onData(Matchers.`is`(currency.get()))
                    .inAdapterView(withId(android.R.id.list)).perform(click())
            onView(withId(R.id.edt_currency_fraction_digits))
                    .perform(replaceText("3"), closeSoftKeyboard())
            if (withUpdate) {
                onView(withId(R.id.checkBox)).perform(click())
            }
            onView(withText(android.R.string.ok)).perform(click())
            onView(withText(allOf(containsString(currency.get().toString()), containsString("3")))).check(matches(isDisplayed()))
            val after = Account.getInstanceFromDb(account.id).totalBalance
            if (withUpdate) {
                assertThat(after.amountMajor).isEqualByComparingTo(before.amountMajor)
                assertThat((after.amountMinor)).isEqualTo(before.amountMinor * 10)
            } else {
                assertThat(after.amountMajor).isEqualByComparingTo(before.amountMajor.divide(BigDecimal(10)))
                assertThat((after.amountMinor)).isEqualTo(before.amountMinor)
            }
        } finally {
            Account.delete(account.id)
            currencyContext.storeCustomFractionDigits(CURRENCY_CODE, 2)
        }
    }

    override val testScenario: ActivityScenario<out ProtectedFragmentActivity?>
        get() = scenarioRule.scenario
    override val listId: Int
        get() = android.R.id.list

    companion object {
        private const val CURRENCY_CODE = "EUR"
    }
}