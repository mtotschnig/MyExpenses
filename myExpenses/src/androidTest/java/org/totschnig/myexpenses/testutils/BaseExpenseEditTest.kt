package org.totschnig.myexpenses.testutils

import android.content.Intent
import android.widget.Button
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matchers
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID

const val TEMPLATE_TITLE = "Espresso template"
const val ACCOUNT_LABEL_1 = "Test label 1"
const val ACCOUNT_LABEL_2 = "Test label 2"
const val PARTY_NAME = "John"
const val DEBT_LABEL = "Schuld"
const val CATEGORY_LABEL = "Grocery"
const val CATEGORY_ICON = "apple-whole"
const val TAG_LABEL = "Wichtig"

abstract class BaseExpenseEditTest : BaseComposeTest<TestExpenseEdit>() {
    lateinit var account1: Account

    val transferCategoryId
        get() = prefHandler.defaultTransferCategory

    suspend fun load() = repository.loadTransactions(account1.id)


    fun getBaseIntent(type: Int = Transactions.TYPE_SPLIT): Intent =
        getIntentForNewTransaction().apply {
            putExtra(Transactions.OPERATION_TYPE, type)
        }

    fun getIntentForNewTransaction(accountId: Long = account1.id) = intent.apply {
        putExtra(KEY_ACCOUNTID, accountId)
    }

    fun getIntentForEditTransaction(rowId: Long) = intent.apply {
        putExtra(KEY_ROWID, rowId)
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

    fun setOperationType(@Transactions.TransactionType operationType: Int) {
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

    protected fun assertTransfer(
        id: Long,
        expectedAccount: Long,
        expectedAmount: Long,
        expectedTransferAccount: Long,
        expectedTransferAmount: Long
    ) {
        val transaction = repository.loadTransaction(id)
        with(transaction.data) {
            assertThat(amount).isEqualTo(expectedAmount)
            assertThat(accountId).isEqualTo(expectedAccount)
        }
        with(transaction.transferPeer!!) {
            assertThat(amount).isEqualTo(expectedTransferAmount)
            assertThat(accountId).isEqualTo(expectedTransferAccount)
        }
    }

    protected fun launch(i: Intent = getIntentForNewTransaction()): ActivityScenario<TestExpenseEdit> =
        ActivityScenario.launch<TestExpenseEdit>(i).also {
            testScenario = it
        }

    protected fun launchForResult(i: Intent = getIntentForNewTransaction()): ActivityScenario<TestExpenseEdit> =
        ActivityScenario.launchActivityForResult<TestExpenseEdit>(i).also {
            testScenario = it
        }

    fun launchNewTemplate(type: Int = Transactions.TYPE_TRANSACTION) {
        launchForResult(getBaseIntent(type).apply {
            putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true)
        })
    }

    fun setDebt() {
        onView(withId(R.id.DebtCheckBox)).perform(click())
        onView(withText(PARTY_NAME)).perform(click())
        onView(withSubstring(DEBT_LABEL)).perform(click())
    }

    fun setCategory() {
        onView(withId(R.id.Category)).perform(click())
        composeTestRule.onNodeWithText(CATEGORY_LABEL).performClick()
    }
}