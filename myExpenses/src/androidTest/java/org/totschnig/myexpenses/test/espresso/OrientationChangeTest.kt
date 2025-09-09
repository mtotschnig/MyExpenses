package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.internal.viewaction.NestedEnabledScrollToAction.nestedScrollToAction
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.withMethod
import org.totschnig.myexpenses.testutils.withStatus
import java.util.Currency


@TestShard4
class OrientationChangeTest : BaseExpenseEditTest() {
    private val accountLabel1 = "Test label 1"
    private lateinit var currency1: CurrencyUnit
    private val accountLabel2 = "Test label 2"
    private lateinit var account2: Account
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        val accountTypeCash = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        val accountTypeBank = repository.findAccountType(PREDEFINED_NAME_BANK)!!
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        account1 = Account(
            label = accountLabel1,
            currency = currency1.code,
            type = accountTypeBank,
            ).createIn(repository)
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        account2 = Account(
            label = accountLabel2,
            currency = currency2.code,
            type = accountTypeCash
        ).createIn(repository)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun shouldKeepAccountAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1.id, currency1)
        transaction.amount = Money(currency1, 500L)
        transaction.save(contentResolver)
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        testScenario = ActivityScenario.launch(i)
        setAccount(accountLabel2)
        checkAccount(accountLabel2)
        doWithRotation {
            onIdle()
            checkAccount(accountLabel2)
        }
    }

    @Test
    fun shouldKeepMethodAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1.id, currency1)
        transaction.amount = Money(currency1, -500L)
        transaction.methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.DIRECTDEBIT.name)
        transaction.save(contentResolver)
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        testScenario = ActivityScenario.launch(i)
        //Thread.sleep(100) //unfortunately needed if test starts in landscape
        closeSoftKeyboard()
        onView(withId(R.id.MethodSpinner)).perform(nestedScrollToAction(), click())
        val string = getString(PreDefinedPaymentMethod.CREDITCARD.resId)
        onData(
            allOf(
                instanceOf(org.totschnig.myexpenses.viewmodel.data.PaymentMethod::class.java),
                withMethod(string)
            )
        ).perform(click())
        onView(withId(R.id.MethodSpinner)).check(matches(withSpinnerText(containsString(string))))
        doWithRotation {
            onIdle()
            onView(withId(R.id.MethodSpinner)).check(matches(withSpinnerText(containsString(string))))
        }
    }


    @Test
    fun shouldKeepStatusAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1.id, currency1)
        transaction.amount = Money(currency1, -500L)
        transaction.crStatus = CrStatus.UNRECONCILED
        transaction.save(contentResolver)
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
        testScenario = ActivityScenario.launch(i)
        closeSoftKeyboard()
        //onView(withId(R.id.Status)).perform(nestedScrollToAction(), click()) leads to Status spinner
        //being overlayed by Floating Action Button on phone landscape
        onView(isAssignableFrom(NestedScrollView::class.java)).perform(swipeUp())
        onView(withId(R.id.Status)).perform(click())
        onData(
            allOf(
                instanceOf(CrStatus::class.java),
                withStatus(CrStatus.CLEARED)
            )
        ).perform(click())
        //withSpinnerText matches toString of object
        val string = CrStatus.CLEARED.toString()
        onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
        doWithRotation {
            onIdle()
            onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
        }
    }

    @Test
    fun shouldHandleNewInstanceAfterOrientationChange() {
        launch()
        doWithRotation {
            onIdle()
            toolbarTitle().check(doesNotExist())
            checkEffectiveVisible(R.id.OperationType)
        }
    }

    @Test
    fun shouldHandleExistingInstanceAfterOrientationChange() {
        val id = with(Transaction.getNewInstance(account1.id, currency1)) {
            amount = Money(currency1, -500L)
            crStatus = CrStatus.UNRECONCILED
            save(contentResolver)
            id
        }
        testScenario = ActivityScenario.launch(
            Intent(targetContext, ExpenseEdit::class.java).apply {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            })
        doWithRotation {
            onIdle()
            checkEffectiveGone(R.id.OperationType)
            toolbarTitle().check(matches(withText(R.string.menu_edit_transaction)))
        }
    }
}