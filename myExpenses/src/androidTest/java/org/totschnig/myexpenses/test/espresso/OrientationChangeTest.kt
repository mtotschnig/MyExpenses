package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.content.OperationApplicationException
import android.content.pm.ActivityInfo
import android.os.RemoteException
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.IAccount
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import java.util.*


@RunWith(AndroidJUnit4::class)
class OrientationChangeTest {
    @get:Rule
    var mActivityRule = ActivityTestRule(ExpenseEdit::class.java, false, false)
    private val accountLabel1 = "Test label 1"
    private var account1: Account? = null
    private var currency1: CurrencyUnit? = null

    private val accountLabel2 = "Test label 2"
    private var account2: Account? = null
    private var currency2: CurrencyUnit? = null
    
    @Before
    fun fixture() {
        currency1 = CurrencyUnit.create(Currency.getInstance("USD"))
        account1 = Account(accountLabel1, currency1, 0, "", AccountType.BANK, Account.DEFAULT_COLOR).apply { save() }
        currency2 = CurrencyUnit.create(Currency.getInstance("EUR"))
        account2 = Account(accountLabel2, currency2, 0, "", AccountType.CASH, Account.DEFAULT_COLOR).apply { save() }
    }

    @After
    @Throws(RemoteException::class, OperationApplicationException::class)
    fun tearDown() {
        account1?.let {
            Account.delete(it.id)
        }
        account2?.let {
            Account.delete(it.id)
        }
    }

    @Test
    fun shouldKeepAccountAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1!!.id)
        transaction.amount = Money(currency1, 500L)
        transaction.save()
        val i = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        mActivityRule.launchActivity(i)
        onView(withId(R.id.Account)).perform(click())
        onData(allOf(instanceOf(IAccount::class.java), withLabel(accountLabel2))).perform(click())
        onView(withId(R.id.Account)).check(matches(withSpinnerText(containsString(accountLabel2))))
        mActivityRule.getActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onView(withId(R.id.Account)).check(matches(withSpinnerText(containsString(accountLabel2))))
    }

    @Test
    fun shouldKeepMethodAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1!!.id)
        transaction.amount = Money(currency1, -500L)
        transaction.methodId = PaymentMethod.find(PaymentMethod.PreDefined.DIRECTDEBIT.name)
        transaction.save()
        val i = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        mActivityRule.launchActivity(i)
        onView(withId(R.id.Method)).perform(click())
        val string = mActivityRule.activity.getString(PaymentMethod.PreDefined.CREDITCARD.resId)
        onData(allOf(instanceOf(org.totschnig.myexpenses.viewmodel.data.PaymentMethod::class.java), withMethod(string))).perform(click())
        onView(withId(R.id.Method)).check(matches(withSpinnerText(containsString(string))))
        mActivityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onView(withId(R.id.Method)).check(matches(withSpinnerText(containsString(string))))
    }

    fun withMethod(label: String): Matcher<Any> =
            object : BoundedMatcher<Any, org.totschnig.myexpenses.viewmodel.data.PaymentMethod>(org.totschnig.myexpenses.viewmodel.data.PaymentMethod::class.java) {
                override fun matchesSafely(myObj: org.totschnig.myexpenses.viewmodel.data.PaymentMethod): Boolean {
                    return myObj.label().equals(label)
                }

                override fun describeTo(description: Description) {
                    description.appendText("with label '${label}'")
                }
            }

    fun withLabel(content: String): Matcher<Any> =
            object : BoundedMatcher<Any, IAccount>(IAccount::class.java) {
                override fun matchesSafely(myObj: IAccount): Boolean {
                    return myObj.toString().equals(content)
                }

                override fun describeTo(description: Description) {
                    description.appendText("with label '$content'")
                }
            }
}