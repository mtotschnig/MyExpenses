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
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.internal.viewaction.NestedEnabledScrollToAction.nestedScrollToAction
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.ACTION_CREATE_FROM_TEMPLATE
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.ACTION_CREATE_TEMPLATE_FROM_TRANSACTION
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.db2.entities.Recurrence
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod

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


    fun getBaseIntent(type: Int = TYPE_SPLIT): Intent =
        getIntentForNewTransaction().apply {
            putExtra(Transactions.OPERATION_TYPE, type)
        }

    fun getIntentForNewTransaction(accountId: Long = account1.id) = intent.apply {
        putExtra(KEY_ACCOUNTID, accountId)
    }

    fun getIntentForEditTransaction(rowId: Long) = intent.apply {
        putExtra(KEY_ROWID, rowId)
    }

    fun getIntentForEditTemplate(rowId: Long) = intent.apply {
        putExtra(KEY_TEMPLATEID, rowId)
    }

    fun getIntentForTransactionFromTemplate(rowId: Long) = getIntentForEditTemplate(rowId).apply {
        action = ACTION_CREATE_FROM_TEMPLATE
    }

    fun getIntentForTemplateFromTransaction(rowId: Long) =
        getIntentForEditTransaction(rowId).apply {
            action = ACTION_CREATE_TEMPLATE_FROM_TRANSACTION
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

    protected fun selectRecurrenceFromSpinner(recurrence: Recurrence) {
        onView(withId(R.id.Recurrence)).perform(scrollTo(), click())

        onData(
            allOf(
                instanceOf(Recurrence::class.java),
                `is`(recurrence)
            )
        ).perform(click())

        onView(withId(R.id.Recurrence)).check(matches(withAdaptedData(`is`(recurrence))))
    }

    protected fun assertTransfer(
        id: Long,
        expectedAccount: Long,
        expectedAmount: Long,
        expectedTransferAccount: Long,
        expectedTransferAmount: Long,
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

    fun launchNewTemplate(type: Int = TYPE_TRANSACTION) {
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

    fun setMethod(method: PreDefinedPaymentMethod) {
        onView(withId(R.id.MethodSpinner)).perform(nestedScrollToAction(), click())
        onData(
            allOf(
                instanceOf(PaymentMethod::class.java),
                withMethod(getString(method.resId))
            )
        ).perform(click())
    }

    fun checkMethod(method: PreDefinedPaymentMethod) {
        onView(withId(R.id.MethodSpinner)).check(
            matches(
                withSpinnerText(
                    containsString(
                        getString(
                            method.resId
                        )
                    )
                )
            )
        )
    }

    fun checkToolbarTitle(expected: Int) {
        toolbarTitle().check(matches(withText(expected)))
    }

    fun checkToolbarTitleForTemplate(
        edit: Boolean = false,
        @Transactions.TransactionType transactionType: Int = TYPE_TRANSACTION,
    ) {
        toolbarTitle().check(
            matches(
                withText(
                    if (edit) getString(
                        R.string.menu_edit_template
                    ) + " (" + getString(
                        when (transactionType) {
                            TYPE_TRANSACTION -> R.string.transaction
                            TYPE_SPLIT -> R.string.split_transaction
                            TYPE_TRANSFER -> R.string.transfer
                            else -> throw IllegalArgumentException()
                        }
                    ) + ")" else getString(
                        when (transactionType) {
                            TYPE_TRANSACTION -> R.string.menu_create_template_for_transaction
                            TYPE_SPLIT -> R.string.menu_create_template_for_split
                            TYPE_TRANSFER -> R.string.menu_create_template_for_transfer
                            else -> throw IllegalArgumentException()
                        }
                    )
                )
            )
        )
    }
}