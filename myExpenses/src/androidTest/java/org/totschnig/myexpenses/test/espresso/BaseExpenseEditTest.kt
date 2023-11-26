package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.widget.Button
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matchers
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.Espresso.withIdAndParent
import org.totschnig.myexpenses.testutils.withOperationType

abstract class BaseExpenseEditTest: BaseUiTest<TestExpenseEdit>() {
    lateinit var account1: Account

    val intentForNewTransaction
        get() = intent.apply {
            putExtra(KEY_ACCOUNTID, account1.id)
        }

    val intent get() = Intent(targetContext, TestExpenseEdit::class.java)

    fun setAmount(amount: Int) {
        onView(
            withIdAndParent(
                R.id.AmountEditText,
                R.id.Amount
            )
        ).perform(
            scrollTo(),
            click(),
            replaceText(amount.toString())
        )
        closeSoftKeyboard()
    }

    fun checkAmount(amount: Int, parent: Int = R.id.Amount) {
        onView(withIdAndParent(R.id.AmountEditText, parent))
            .check(matches(withText(amount.toString())))
    }

    fun toggleType() {
        onView(withIdAndParent(R.id.TaType, R.id.Amount))
            .perform(click())
    }

    fun checkType(checked: Boolean) {
        onView(withIdAndParent(R.id.TaType, R.id.Amount))
            .check(matches(if(checked) isChecked() else isNotChecked()))
    }

    fun setStoredPayee(payee: String) {
        onView(withId(R.id.Payee)).perform(typeText(payee.first().toString()))
        onView(withText(payee))
            .inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        //Auto Fill Dialog
        onView(
            Matchers.allOf(
                isAssignableFrom(Button::class.java),
                withText(R.string.response_yes)
            )
        ).perform(click())
        onView(withId(R.id.Payee)).check(matches(withText("John")))
    }

    fun setOperationType(@TransactionsContract.Transactions.TransactionType operationType: Int) {
        onView(withId(R.id.OperationType)).perform(click())
        onData(
            allOf(
                instanceOf(TransactionDelegate.OperationType::class.java),
                withOperationType(operationType)
            )
        ).perform(click())
    }
}