package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.adevinta.android.barista.internal.viewaction.NestedEnabledScrollToAction.nestedScrollToAction
import org.hamcrest.CoreMatchers.*
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.*
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import java.util.*


class OrientationChangeTest : BaseMyExpensesTest() {
    private val accountLabel1 = "Test label 1"
    private lateinit var account1: Account
    private lateinit var currency1: CurrencyUnit
    private val accountLabel2 = "Test label 2"
    private lateinit var account2: Account
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        account1 = Account(
            accountLabel1,
            currency1,
            0,
            "",
            AccountType.BANK,
            Account.DEFAULT_COLOR
        ).apply { save() }
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        account2 = Account(
            accountLabel2,
            currency2,
            0,
            "",
            AccountType.CASH,
            Account.DEFAULT_COLOR
        ).apply { save() }
    }

    @Test
    fun shouldKeepAccountAfterOrientationChange() {
        val transaction = Transaction.getNewInstance(account1)
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
        val transaction = Transaction.getNewInstance(account1)
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
        val transaction = Transaction.getNewInstance(account1)
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
        testScenario = ActivityScenario.launch(
            Intent(targetContext, ExpenseEdit::class.java).apply {
                putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            })
        rotate()
        onIdle()
        toolbarTitle().check(doesNotExist())
        checkEffectiveVisible(R.id.OperationType)
        rotate()
    }

    @Test
    fun shouldHandleExistingInstanceAfterOrientationChange() {
        val id = with(Transaction.getNewInstance(account1)) {
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