package org.totschnig.myexpenses.test.espresso

import android.content.Context
import android.content.Intent
import android.content.OperationApplicationException
import android.os.Bundle
import android.os.RemoteException
import android.widget.ListView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.intercepting.SingleActivityFactory
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.anything
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.Espresso.withIdAndParent
import java.util.*

@RunWith(AndroidJUnit4::class)
class SplitEditTest : BaseUiTest() {
    var splitPartListUpdateCalled = 0
    var activityIsRecreated = false
    private var activityFactory: SingleActivityFactory<ExpenseEdit> = object : SingleActivityFactory<ExpenseEdit>(ExpenseEdit::class.java) {
        override fun create(intent: Intent): ExpenseEdit {
            return object : ExpenseEdit() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    if (savedInstanceState != null) {
                        activityIsRecreated = true
                    }
                }

                override fun updateSplitPartList(account: org.totschnig.myexpenses.viewmodel.data.Account, rowId: Long) {
                    super.updateSplitPartList(account, rowId)
                    if (activityIsRecreated) {
                        activityIsRecreated = false
                    } else {
                        splitPartListUpdateCalled++
                    }
                }
            }
        }
    }

    @get:Rule
    var mActivityRule = ActivityTestRule(activityFactory, false, false)
    private val accountLabel1 = "Test label 1"
    lateinit var account1: Account
    private var currency1: CurrencyUnit? = null

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val baseIntent: Intent
        get() = Intent(targetContext, ExpenseEdit::class.java).apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_SPLIT)
        }

    @Before
    fun fixture() {
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        account1 = Account(accountLabel1, currency1, 0, "", AccountType.CASH, Account.DEFAULT_COLOR).apply { save() }
    }

    @After
    @Throws(RemoteException::class, OperationApplicationException::class)
    fun tearDown() {
        Account.delete(account1.id)
    }

    override fun getTestRule() = mActivityRule

    @Test
    fun canceledSplitCleanup() {
        mActivityRule.launchActivity(baseIntent)
        val uncommittedUri = TransactionProvider.UNCOMMITTED_URI
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(1)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(0)
        assertThat(mActivityRule.activity.isFinishing).isTrue
    }

    @Test
    fun createPartAndSave() {
        mActivityRule.launchActivity(baseIntent)
        assertThat(splitPartListUpdateCalled).isEqualTo(1)
        createParts(5)
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save parent fails with unsplit amount
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.unsplit_amount_greater_than_zero)))
        enterAmountSave("250")
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save parent succeeds
        assertThat(mActivityRule.activity.isFinishing).isTrue
    }

    private fun createParts(times: Int) {
        assertThat(splitPartListUpdateCalled).isEqualTo(1)
        repeat(times) {
            closeSoftKeyboard()
            onView(withId(R.id.CREATE_PART_COMMAND)).perform(scrollTo(), click())
            onView(withId(R.id.MANAGE_TEMPLATES_COMMAND)).check(doesNotExist())
            onView(withId(R.id.CREATE_TEMPLATE_COMMAND)).check(doesNotExist())
            enterAmountSave("50")
            onView(withId(R.id.CREATE_COMMAND)).perform(click())//save part
            assertThat(splitPartListUpdateCalled).isEqualTo(1)
        }
    }

    @Test
    fun loadEditSaveSplit() {
        mActivityRule.launchActivity(baseIntent.apply { putExtra(KEY_ROWID, prepareSplit()) })
        assertThat(waitForAdapter().count).isEqualTo(2)
        closeSoftKeyboard()
        onView(withId(R.id.list)).perform(scrollTo())
        onData(anything()).inAdapterView(ViewMatchers.isAssignableFrom(ListView::class.java)).atPosition(0).perform(click())
        onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).perform(replaceText("150"))
        onView(withId(R.id.MANAGE_TEMPLATES_COMMAND)).check(doesNotExist())
        onView(withId(R.id.CREATE_TEMPLATE_COMMAND)).check(doesNotExist())
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save part
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save parent fails with unsplit amount
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.unsplit_amount_greater_than_zero)))
        onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).perform(replaceText("200"))
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save parent succeeds
        assertThat(mActivityRule.activity.isFinishing).isTrue
    }

    private fun prepareSplit() = with(SplitTransaction.getNewInstance(account1.id)) {
        amount = Money(CurrencyUnit(Currency.getInstance("EUR")), 10000)
        status = DatabaseConstants.STATUS_NONE
        save(true)
        val part = Transaction.getNewInstance(account1.id, id)
        part.amount = Money(CurrencyUnit(Currency.getInstance("EUR")), 5000)
        part.save()
        part.amount = Money(CurrencyUnit(Currency.getInstance("EUR")), 5000)
        part.saveAsNew()
        id
    }

    @Test
    fun canceledTemplateSplitCleanup() {
        mActivityRule.launchActivity(baseIntent.apply { putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true) })
        val uncommittedUri = TransactionProvider.TEMPLATES_UNCOMMITTED_URI
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(1)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(0)
    }

    @Test
    fun create_and_save() {
        mActivityRule.launchActivity(baseIntent)
        createParts(1)
        enterAmountSave("50")
        clickMenuItem(R.id.SAVE_AND_NEW_COMMAND, false) //toggle save and new on
        onView(withId(R.id.CREATE_COMMAND)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.save_transaction_and_new_success)))
        waitForSnackbarDismissed()
        createParts(1)
        enterAmountSave("50")
        clickMenuItem(R.id.SAVE_AND_NEW_COMMAND, false) //toggle save and new off
        closeKeyboardAndSave()
        assertThat(mActivityRule.activity.isFinishing).isTrue
    }

    private fun enterAmountSave(amount: String) {
        onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).perform(typeText(amount))
        closeSoftKeyboard()
    }
}