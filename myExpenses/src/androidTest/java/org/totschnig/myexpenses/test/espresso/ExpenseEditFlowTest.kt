package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.provider.CalendarContract
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.ACTION_CREATE_FROM_TEMPLATE
import org.totschnig.myexpenses.db2.createPaymentMethod
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteAllTags
import org.totschnig.myexpenses.db2.deleteMethod
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.insertTemplate
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadPrice
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model2.PAYMENT_METHOD_EXPENSE
import org.totschnig.myexpenses.model2.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_2
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TEMPLATE_TITLE
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.uriStartsWith
import org.totschnig.myexpenses.testutils.withAdaptedData
import org.totschnig.myexpenses.testutils.withIdAndParent
import java.time.LocalDate

@TestShard2
class ExpenseEditFlowTest : BaseExpenseEditTest() {
    var templateId: Long = 0
    var methodId: Long = 0

    @Before
    fun fixture() {
        account1 = buildAccount(ACCOUNT_LABEL_1)

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

        templateId = repository.insertTemplate(
            accountId = account1.id,
            title = "Template",
            amount = 500L,
            defaultAction = Template.Action.EDIT
        ).id
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
        val account2 = buildAccount(ACCOUNT_LABEL_2)
        launchForResult(intent.apply {
            putExtra(KEY_ACCOUNTID, account2.id)
        })
        checkAccount(ACCOUNT_LABEL_2)
        onView(withId(R.id.Category)).perform(click())
        pressBack()
        checkAccount(ACCOUNT_LABEL_2)
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun templateMenuShouldLoadTemplateForNewTransaction() {
        launchForResult()
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        onView(withText("Template")).perform(click())
        toolbarTitle().check(matches(withText(R.string.menu_create_transaction)))
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

    @Test
    fun saveAsPlanCreatesPlanInstanceLink() {
        launchForResult()
        clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
        setTitle()
        selectRecurrenceFromSpinner(Plan.Recurrence.DAILY)
        setAmount(5)
        clickFab()
        val template =
            assertTemplate(
                expectedAccount = account1.id,
                expectedAmount = -500,
                expectedPlanRecurrence = Plan.Recurrence.DAILY,
                checkPlanInstance = true
            )
        Plan.delete(contentResolver, template.plan!!.id)
    }

    @Test
    fun saveAsPlanCustomRecurrenceOpensCalendar() {
        Intents.init()
        launchForResult()
        clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
        setTitle()
        selectRecurrenceFromSpinner(Plan.Recurrence.CUSTOM)
        setAmount(5)
        clickFab()
        val template =
            assertTemplate(
                expectedAccount = account1.id,
                expectedAmount = -500,
                expectedPlanRecurrence = Plan.Recurrence.CUSTOM,
                checkPlanInstance = true
            )
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(uriStartsWith(CalendarContract.Events.CONTENT_URI.toString()))
            )
        )
        Plan.delete(contentResolver, template.plan!!.id)
    }

    @Test
    fun templateWithCustomRecurrenceOpensCalendar() {
        Intents.init()
        launchNewTemplate()
        setTitle()
        selectRecurrenceFromSpinner(Plan.Recurrence.CUSTOM)
        setAmount(5)
        clickFab()
        val template =
            assertTemplate(
                expectedAccount = account1.id,
                expectedAmount = -500,
                expectedPlanRecurrence = Plan.Recurrence.CUSTOM
            )
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(uriStartsWith(CalendarContract.Events.CONTENT_URI.toString()))
            )
        )
        Plan.delete(contentResolver, template.plan!!.id)
    }

    private fun linkWithTag() {
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withId(R.id.tag_edit)).perform(replaceText("Tag"), pressImeActionButton())
        clickFab()
    }

    private fun selectRecurrenceFromSpinner(recurrence: Plan.Recurrence) {
        onView(withId(R.id.Recurrence)).perform(click())

        onData(
            allOf(
                instanceOf(Plan.Recurrence::class.java),
                `is`(recurrence)
            )
        ).perform(click())

        onView(withId(R.id.Recurrence)).check(matches(withAdaptedData(`is`(recurrence))))
    }

    //Bug https://github.com/mtotschnig/MyExpenses/issues/1426
    @Test
    fun instantiateTemplateForEditDoesNotAffectTemplate() {
        launchForResult(getIntentForNewTransaction().apply {
            action = ACTION_CREATE_FROM_TEMPLATE
            putExtra(KEY_TEMPLATEID, templateId)
        })
        setAmount(5)
        linkWithTag()
        clickFab()
        //asserts that template is still without tags
        assertTemplate(account1.id, 500, templateTitle = "Template")
        cleanup {
            repository.deleteAllTags()
        }
    }

    //https://github.com/mtotschnig/MyExpenses/issues/1793
    @Test
    fun calculatePriceCorrectlyWhenFractionDigitsAreDifferent() {
        val account2 = buildAccount(
            ACCOUNT_LABEL_2,
            currency = "PYG",
            dynamicExchangeRates = true
        )
        launchForResult(getIntentForNewTransaction(account2.id))
        setAmount(10000)
        setAmount(1, R.id.EquivalentAmount)
        clickFab()
        val price = repository.loadPrice(homeCurrency, currencyContext["PYG"],
            LocalDate.now(),
            ExchangeRateSource.User
        )
        assertThat(price).isEqualToIgnoringScale("0.0001")
    }

    //https://github.com/mtotschnig/MyExpenses/issues/1793
    @Test
    fun doNotStorePriceWhenTransactionIsEditedButAmountIsUntouched() {
        Assume.assumeFalse(homeCurrency.code == "DKK")
        val account2 = buildAccount(
            ACCOUNT_LABEL_2,
            currency = "DKK",
            dynamicExchangeRates = true
        )
        val transaction = repository.insertTransaction(
            accountId = account2.id,
            amount = 100,
            equivalentAmount = 13
        )
        launch(getIntentForEditTransaction(transaction.id))
        onView(withId(R.id.Comment))
            .perform(
                scrollTo(),
                replaceText("New comment"))
        closeSoftKeyboard()
        clickFab()
        val price = repository.loadPrice(homeCurrency, currencyContext["DKK"],
            LocalDate.now(),
            ExchangeRateSource.User
        )
        assertThat(price).isNull()
    }
}