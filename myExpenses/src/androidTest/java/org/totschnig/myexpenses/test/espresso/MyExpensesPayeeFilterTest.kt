package org.totschnig.myexpenses.test.espresso

import android.content.OperationApplicationException
import android.os.RemoteException
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.CursorMatchers
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseUiTest
import java.util.*

class MyExpensesPayeeFilterTest: BaseUiTest() {
    @get:Rule
    var scenarioRule = ActivityScenarioRule(MyExpenses::class.java)
    private lateinit var account: Account
    private var payee1 = "John Doe"
    private var payee2 = "Hinz Finz"

    @Before
    fun fixture() {
        val currency = CurrencyUnit(Currency.getInstance("EUR"))
        account = Account("Test account 1", currency, 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        val op = Transaction.getNewInstance(account.id)
        op.amount = Money(currency, -1200L)
        op.payee = payee1
        op.save()
        op.payee = payee2
        op.saveAsNew()
    }

    @Test
    fun payeeFilterShouldHideTransaction() {
        waitForAdapter()
        payeeIsDisplayed(payee1)
        payeeIsDisplayed(payee2)
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(withText(R.string.payer_or_payee)).perform(click())
        onView(withText(payee1)).perform(click())
        payeeIsDisplayed(payee1)
        payeeIsNotDisplayed(payee2)
        //switch off filter
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(withText(payee1)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        payeeIsDisplayed(payee2)
    }

    private fun payeeIsDisplayed(payee: String) {
        onData(CursorMatchers.withRowString(DatabaseConstants.KEY_PAYEE_NAME, payee))
                .inAdapterView(wrappedList).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun payeeIsNotDisplayed(payee: String) {
        onView(wrappedList)
                .check(ViewAssertions.matches(Matchers.not(org.totschnig.myexpenses.testutils.Matchers.withAdaptedData(
                        CursorMatchers.withRowString(DatabaseConstants.KEY_PAYEE_NAME, payee)))))
    }

    @After
    @Throws(RemoteException::class, OperationApplicationException::class)
    fun tearDown() {
        Account.delete(account.id)
    }

    override val testScenario: ActivityScenario<out ProtectedFragmentActivity>
        get() = scenarioRule.scenario

}