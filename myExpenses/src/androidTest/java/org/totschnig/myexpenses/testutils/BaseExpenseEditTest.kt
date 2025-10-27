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
import com.google.common.truth.Truth.assertWithMessage
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matchers
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.RepositoryTemplate
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.loadTagsForTemplate
import org.totschnig.myexpenses.db2.loadTagsForTransaction
import org.totschnig.myexpenses.db2.loadTemplate
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_URI
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalDate

const val TEMPLATE_TITLE = "Espresso template"
const val ACCOUNT_LABEL_1 = "Test label 1"
const val ACCOUNT_LABEL_2 = "Test label 2"


abstract class BaseExpenseEditTest : BaseComposeTest<TestExpenseEdit>() {
    lateinit var account1: Account

    fun getBaseIntent(type: Int = Transactions.TYPE_SPLIT): Intent = intentForNewTransaction.apply {
        putExtra(Transactions.OPERATION_TYPE, type)
    }

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

    protected fun assertTemplate(
        expectedAccount: Long,
        expectedAmount: Long,
        templateTitle: String = TEMPLATE_TITLE,
        expectedTags: List<String> = emptyList(),
        expectedSplitParts: List<Long>? = null,
        expectedPlanRecurrence: Plan.Recurrence = Plan.Recurrence.NONE
    ): RepositoryTemplate {
        val templateId = contentResolver.query(
            TEMPLATES_URI,
            arrayOf(KEY_ROWID),
            "$KEY_TITLE = ?",
            arrayOf(templateTitle),
            null
        )!!.use {
            assertWithMessage("No template with title $templateTitle").that(it.moveToFirst())
                .isTrue()
            it.getLong(0)
        }
        val template = repository.loadTemplate(templateId)!!
        val tags = repository.loadTagsForTemplate(templateId)
        with(template.data) {
            assertThat(amount).isEqualTo(expectedAmount)
            assertThat(title).isEqualTo(templateTitle)
            assertThat(accountId).isEqualTo(expectedAccount)
        }
        assertThat(tags.map { it.label }).containsExactlyElementsIn(expectedTags)

        if (expectedSplitParts == null) {
            assertThat(template.splitParts).isNull()
        } else {
            assertThat(template.splitParts!!.map { it.data.amount })
                .containsExactlyElementsIn(expectedSplitParts)
        }

        if (expectedPlanRecurrence != Plan.Recurrence.NONE) {
            assertThat(template.plan!!.id).isGreaterThan(0)
            val today = LocalDate.now()
            assertThat(template.plan.rRule).isEqualTo(expectedPlanRecurrence.toRule(today))
            contentResolver.query(
                TransactionProvider.PLAN_INSTANCE_SINGLE_URI(
                    template.id,
                    CalendarProviderProxy.calculateId(template.plan.dtStart)
                ),
                null, null, null, null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(KEY_TRANSACTIONID) { isGreaterThan(0) }
            }
        } else {
            assertThat(template.plan).isNull()
        }
        return template
    }

    protected fun assertTransaction(
        id: Long,
        expectedAccount: Long,
        expectedAmount: Long,
        expectedTags: List<String> = emptyList(),
        expectedSplitParts: List<Long>? = null
    ) {

        val transaction = repository.loadTransaction(id)
        val tags = repository.loadTagsForTransaction(id)
        with(transaction.data) {
            assertThat(amount).isEqualTo(expectedAmount)
            assertThat(accountId).isEqualTo(expectedAccount)
        }
        assertThat(tags.map { it.label }).containsExactlyElementsIn(expectedTags)
        if (expectedSplitParts == null) {
            assertThat(transaction.splitParts).isNull()
        } else {
            assertThat(transaction.splitParts!!.map { it.data.amount })
                .containsExactlyElementsIn(expectedSplitParts)
        }
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

    protected fun launch(i: Intent = intentForNewTransaction): ActivityScenario<TestExpenseEdit> =
        ActivityScenario.launch<TestExpenseEdit>(i).also {
            testScenario = it
        }

    protected fun launchForResult(i: Intent = intentForNewTransaction): ActivityScenario<TestExpenseEdit> =
        ActivityScenario.launchActivityForResult<TestExpenseEdit>(i).also {
            testScenario = it
        }

    fun launchWithAccountSetup(
        excludeFromTotals: Boolean = false,
        type: Int = Transactions.TYPE_SPLIT,
        configureIntent: Intent.() -> Unit = {},
    ) {
        account1 = buildAccount(ACCOUNT_LABEL_1, excludeFromTotals = excludeFromTotals)
        launchForResult(getBaseIntent(type).apply(configureIntent))
    }

    fun launchNewTemplate(type: Int) {
        launchWithAccountSetup(type = type) { putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true) }
    }
}