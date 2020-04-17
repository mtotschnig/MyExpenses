package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.content.OperationApplicationException
import android.os.RemoteException
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.IAccount
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.withAccount
import org.totschnig.myexpenses.testutils.withMethod
import org.totschnig.myexpenses.testutils.withStatus
import java.util.*


class OrientationChangeTest: BaseUiTest() {
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
        onData(allOf(instanceOf(IAccount::class.java), withAccount(accountLabel2))).perform(click())
        onView(withId(R.id.Account)).check(matches(withSpinnerText(containsString(accountLabel2))))
        rotate()
        Espresso.onIdle()
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
        //Thread.sleep(100) //unfortunately needed if test starts in landscape
        closeSoftKeyboard()
        onView(withId(R.id.Method)).perform(scrollTo(), click())
        val string = getString(PaymentMethod.PreDefined.CREDITCARD.resId)
        onData(allOf(instanceOf(org.totschnig.myexpenses.viewmodel.data.PaymentMethod::class.java), withMethod(string))).perform(click())
        onView(withId(R.id.Method)).check(matches(withSpinnerText(containsString(string))))
        rotate()
        Espresso.onIdle()
        onView(withId(R.id.Method)).check(matches(withSpinnerText(containsString(string))))
    }



    @Test
    fun shouldKeepStatusAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1!!.id)
        transaction.amount = Money(currency1, -500L)
        transaction.crStatus = CrStatus.UNRECONCILED
        transaction.save()
        val i = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        mActivityRule.launchActivity(i)
        //Thread.sleep(100) //unfortunately needed if test starts in landscape
        closeSoftKeyboard()
        onView(withId(R.id.Status)).perform(scrollTo(), click())
        onData(allOf(instanceOf(CrStatus::class.java), withStatus(CrStatus.CLEARED))).perform(click())
        //withSpinnerText matches toString of object
        val string = CrStatus.CLEARED.toString()
        onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
        rotate()
        Espresso.onIdle()
        onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
    }

    @Test
    fun shouldHandleNewInstanceAfterOrientationChange() {
        Intent(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext, ExpenseEdit::class.java).apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            mActivityRule.launchActivity(this)
        }
        rotate()
        Espresso.onIdle()
        toolbarTitle().check(doesNotExist())
        checkEffectiveVisible(R.id.OperationType)
    }

    @Test
    fun shouldHandleExistingInstanceAfterOrientationChange() {
        val id = with(Transaction.getNewInstance(account1!!.id)) {
            amount = Money(currency1, -500L)
            crStatus = CrStatus.UNRECONCILED
            save()
            id
        }
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, ExpenseEdit::class.java).apply {
            putExtra(DatabaseConstants.KEY_ROWID, id)
            mActivityRule.launchActivity(this)
        }
        rotate()
        Espresso.onIdle()
        checkEffectiveGone(R.id.OperationType)
        toolbarTitle().check(matches(withText(R.string.menu_edit_transaction)))
    }

    override fun getTestRule() = mActivityRule
}