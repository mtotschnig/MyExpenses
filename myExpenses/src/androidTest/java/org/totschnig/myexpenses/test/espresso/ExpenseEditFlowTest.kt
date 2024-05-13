package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.ACTION_CREATE_FROM_TEMPLATE
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.createPaymentMethod
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model2.PAYMENT_METHOD_EXPENSE
import org.totschnig.myexpenses.model2.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_URI
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.withIdAndParent

class ExpenseEditFlowTest : BaseExpenseEditTest() {
    lateinit var template: Template
    @Before
    fun fixture() {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        repository.createPaymentMethod(
            targetContext,
            PaymentMethod(
                0,
                "TEST",
                null,
                PAYMENT_METHOD_EXPENSE,
                true,
                null,
                listOf(AccountType.CASH)
            )
        )
        template = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_TRANSACTION,
            account1.id,
            homeCurrency,
            false,
            null
        )!!.apply {
            amount = Money(homeCurrency, 500L)
            title = "Template"
            defaultAction = Template.Action.EDIT
            save(contentResolver)
        }
    }

    /**
     * If user toggles from expense (where we have at least one payment method) to income (where there is none)
     * and then selects category, or opens calculator, and comes back, saving failed. We test here
     * the fix for this bug.
     */
    @Test
    fun testScenarioForBug5b11072e6007d59fcd92c40b() {
        launchForResult(intentForNewTransaction)
        setAmount(10)
        toggleType()
        closeSoftKeyboard()
        onView(withId(R.id.Category)).perform(click())
        androidx.test.espresso.Espresso.pressBack()
        clickFab()
        assertFinishing()
    }

    @Test
    fun calculatorMaintainsType() {
        launchForResult(intentForNewTransaction)
        setAmount(123)
        closeSoftKeyboard()
        onView(
            withIdAndParent(
                R.id.Calculator,
                R.id.Amount
            )
        ).perform(click())
        onView(withId(R.id.bOK))
            .perform(click())
        checkType(false)
    }

    @Test
    fun templateMenuShouldLoadTemplateForNewTransaction() {
        launchForResult(intentForNewTransaction)
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        onView(withText("Template")).perform(click())
        toolbarTitle().check(ViewAssertions.matches(withText(R.string.menu_create_transaction)))
        checkAmount(5)
    }

    //Bug https://github.com/mtotschnig/MyExpenses/issues/1408
    @Test
    fun saveAsTemplateCreatesTemplateWithTags() {
        launchForResult(intentForNewTransaction)
        clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
        setTitle()
        linkWithTag()
        setAmount(5)
        clickFab()
        val templateId = contentResolver.query(
            TEMPLATES_URI,
            arrayOf(KEY_ROWID),
            "$KEY_TITLE = ?",
            arrayOf(TEMPLATE_TITLE),
            null
        )!!.use {
            it.moveToFirst()
            it.getLong(0)
        }
        assertTemplate(templateId, account1.id, -500, expectedTags = listOf("Tag"))
    }

    private fun linkWithTag() {
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withId(R.id.tag_edit)).perform(replaceText("Tag"), pressImeActionButton())
        clickFab()
    }

    //Bug https://github.com/mtotschnig/MyExpenses/issues/1426
    @Test
    fun instantiateTemplateForEditDoesNotAffectTemplate() {
        launchForResult(intentForNewTransaction.apply {
            action = ACTION_CREATE_FROM_TEMPLATE
            putExtra(KEY_TEMPLATEID, template.id)
        })
        linkWithTag()
        setAmount(5)
        clickFab()
        //asserts that template is still without tags
        assertTemplate(template.id, account1.id, 500, templateTitle = "Template")
    }
}