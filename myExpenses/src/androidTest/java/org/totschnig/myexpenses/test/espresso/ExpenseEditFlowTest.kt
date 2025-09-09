package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.ACTION_CREATE_FROM_TEMPLATE
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.createPaymentMethod
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteAllTags
import org.totschnig.myexpenses.db2.deleteMethod
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model2.PAYMENT_METHOD_EXPENSE
import org.totschnig.myexpenses.model2.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.withIdAndParent

@TestShard2
class ExpenseEditFlowTest : BaseExpenseEditTest() {
    var templateId: Long = 0
    var methodId: Long = 0
    @Before
    fun fixture() {
        account1 = buildAccount("Test label 1")

        val accountType = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        methodId = repository.createPaymentMethod(
            targetContext,
            PaymentMethod(
                0,
                "TEST",
                null,
                PAYMENT_METHOD_EXPENSE,
                true,
                null,
                listOf(accountType.id)
            )
        ).id

        templateId = Template.getTypedNewInstance(
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
        }.id
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteMethod(methodId)
        }
    }

    /**
     * If user toggles from expense (where we have at least one payment method) to income (where there is none)
     * and then selects category, or opens calculator, and comes back, saving failed. We test here
     * the fix for this bug.
     */
    @Test
    fun testScenarioForBug5b11072e6007d59fcd92c40b() {
        launchForResult()
        setAmount(10)
        toggleType()
        closeSoftKeyboard()
        onView(withId(R.id.Category)).perform(click())
        pressBack()
        clickFab()
        assertFinishing()
    }

    @Test
    fun calculatorMaintainsType() {
        launchForResult()
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
    fun categorySelectionMaintainsAccount() {
        val accountLabel2 = "Test label 2"
        val account2 = buildAccount(accountLabel2)
        launchForResult(intent.apply {
            putExtra(KEY_ACCOUNTID, account2.id)
        })
        checkAccount(accountLabel2)
        onView(withId(R.id.Category)).perform(click())
        pressBack()
        checkAccount(accountLabel2)
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun templateMenuShouldLoadTemplateForNewTransaction() {
        launchForResult()
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        onView(withText("Template")).perform(click())
        toolbarTitle().check(ViewAssertions.matches(withText(R.string.menu_create_transaction)))
        checkAmount(5)
    }

    //Bug https://github.com/mtotschnig/MyExpenses/issues/1408
    @Test
    fun saveAsTemplateCreatesTemplateWithTags() {
        launchForResult()
        clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
        setTitle()
        linkWithTag()
        setAmount(5)
        clickFab()
        assertTemplate(account1.id, -500, expectedTags = listOf("Tag"))
        cleanup {
            repository.deleteAllTags()
        }
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
            putExtra(KEY_TEMPLATEID, templateId)
        })
        linkWithTag()
        setAmount(5)
        clickFab()
        //asserts that template is still without tags
        assertTemplate(account1.id, 500, templateTitle = "Template")
        cleanup {
            repository.deleteAllTags()
        }
    }
}