package org.totschnig.myexpenses.testutils

import android.content.Intent
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matchers
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_URI
import org.totschnig.myexpenses.testutils.BaseComposeTest
import org.totschnig.myexpenses.testutils.withIdAndParent
import org.totschnig.myexpenses.testutils.withOperationType

const val TEMPLATE_TITLE = "Espresso template"

abstract class BaseExpenseEditTest : BaseComposeTest<TestExpenseEdit>() {
    lateinit var account1: Account

    val intentForNewTransaction
        get() = intent.apply {
            putExtra(KEY_ACCOUNTID, account1.id)
        }

    val intent get() = Intent(targetContext, TestExpenseEdit::class.java)

    fun setAmount(amount: Int, field: Int = R.id.Amount) {
        onView(
            withIdAndParent(
                R.id.AmountEditText,
                field
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
            .check(matches(if (checked) isChecked() else isNotChecked()))
    }

    fun setStoredPayee(payee: String) {
        typeToAndCloseKeyBoard(R.id.auto_complete_textview, payee.first().toString())
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
        onView(withId(R.id.auto_complete_textview)).check(matches(withText(payee)))
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

    protected fun setTitle() {
        onView(withId(R.id.Title))
            .perform(replaceText(TEMPLATE_TITLE))
        closeSoftKeyboard()
    }

    protected fun assertTemplate(
        expectedAccount: Long,
        expectedAmount: Long,
        templateTitle: String = TEMPLATE_TITLE,
        expectedTags: List<String> = emptyList()
    ) {
        val templateId = contentResolver.query(
            TEMPLATES_URI,
            arrayOf(KEY_ROWID),
            "$KEY_TITLE = ?",
            arrayOf(templateTitle),
            null
        )!!.use {
            it.moveToFirst()
            it.getLong(0)
        }
        val (transaction, tags) = Template.getInstanceFromDbWithTags(contentResolver, templateId)!!
        with(transaction as Template) {
            assertThat(amount.amountMinor).isEqualTo(expectedAmount)
            assertThat(title).isEqualTo(templateTitle)
            assertThat(accountId).isEqualTo(expectedAccount)
        }
        assertThat(tags.map { it.label }).containsExactlyElementsIn(expectedTags)
    }

    protected fun launch(i: Intent = intentForNewTransaction): ActivityScenario<TestExpenseEdit> =
        ActivityScenario.launch<TestExpenseEdit>(i).also {
            testScenario = it
        }

    protected fun launchForResult(i: Intent = intentForNewTransaction): ActivityScenario<TestExpenseEdit> =
        ActivityScenario.launchActivityForResult<TestExpenseEdit>(i).also {
            testScenario = it
        }
}