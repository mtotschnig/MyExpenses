package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.internal.viewaction.NestedEnabledScrollToAction.nestedScrollToAction
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.withAccount
import org.totschnig.myexpenses.testutils.withMethod
import org.totschnig.myexpenses.testutils.withStatus
import java.util.Currency


class OrientationChangeTest : BaseExpenseEditTest() {
    private val accountLabel1 = "Test label 1"
    private lateinit var currency1: CurrencyUnit
    private val accountLabel2 = "Test label 2"
    private lateinit var account2: Account
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        account1 = Account(
            label = accountLabel1,
            currency = currency1.code,
            type = AccountType.BANK,
            ).createIn(repository)
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        account2 = Account(
            label = accountLabel2,
            currency = currency2.code
        ).createIn(repository)
    }

    @Test
    fun shouldKeepAccountAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1.id, currency1)
        transaction.amount = Money(currency1, 500L)
        transaction.save()
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        testScenario = ActivityScenario.launch(i)
        onView(withId(R.id.Account)).perform(click())
        onData(allOf(instanceOf(IdHolder::class.java), withAccount(accountLabel2))).perform(click())
        onView(withId(R.id.Account)).check(matches(withSpinnerText(containsString(accountLabel2))))
        rotate()
        onIdle()
        onView(withId(R.id.Account)).check(matches(withSpinnerText(containsString(accountLabel2))))
        rotate()
    }

    @Test
    fun shouldKeepMethodAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1.id, currency1)
        transaction.amount = Money(currency1, -500L)
        transaction.methodId = PaymentMethod.find(PreDefinedPaymentMethod.DIRECTDEBIT.name)
        transaction.save()
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        testScenario = ActivityScenario.launch(i)
        //Thread.sleep(100) //unfortunately needed if test starts in landscape
        closeSoftKeyboard()
        onView(withId(R.id.Method)).perform(nestedScrollToAction(), click())
        val string = getString(PreDefinedPaymentMethod.CREDITCARD.resId)
        onData(
            allOf(
                instanceOf(org.totschnig.myexpenses.viewmodel.data.PaymentMethod::class.java),
                withMethod(string)
            )
        ).perform(click())
        onView(withId(R.id.Method)).check(matches(withSpinnerText(containsString(string))))
        rotate()
        onIdle()
        onView(withId(R.id.Method)).check(matches(withSpinnerText(containsString(string))))
        rotate()
    }


    @Test
    fun shouldKeepStatusAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1.id, currency1)
        transaction.amount = Money(currency1, -500L)
        transaction.crStatus = CrStatus.UNRECONCILED
        transaction.save()
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        testScenario = ActivityScenario.launch(i)
        //Thread.sleep(100) //unfortunately needed if test starts in landscape
        closeSoftKeyboard()
        onView(withId(R.id.Status)).perform(nestedScrollToAction(), click())
        onData(
            allOf(
                instanceOf(CrStatus::class.java),
                withStatus(CrStatus.CLEARED)
            )
        ).perform(click())
        //withSpinnerText matches toString of object
        val string = CrStatus.CLEARED.toString()
        onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
        rotate()
        onIdle()
        onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
        rotate()
    }

    @Test
    fun shouldHandleNewInstanceAfterOrientationChange() {
        testScenario = ActivityScenario.launch(intentForNewTransaction)
        rotate()
        onIdle()
        toolbarTitle().check(doesNotExist())
        checkEffectiveVisible(R.id.OperationType)
        rotate()
    }

    @Test
    fun shouldHandleExistingInstanceAfterOrientationChange() {
        val id = with(Transaction.getNewInstance(account1.id, currency1)) {
            amount = Money(currency1, -500L)
            crStatus = CrStatus.UNRECONCILED
            save()
            id
        }
        testScenario = ActivityScenario.launch(
            Intent(targetContext, ExpenseEdit::class.java).apply {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            })
        rotate()
        onIdle()
        checkEffectiveGone(R.id.OperationType)
        toolbarTitle().check(matches(withText(R.string.menu_edit_transaction)))
        rotate()
    }
}