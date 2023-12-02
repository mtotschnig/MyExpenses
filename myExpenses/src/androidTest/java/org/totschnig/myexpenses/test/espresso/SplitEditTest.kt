package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import com.adevinta.android.barista.internal.viewaction.NestedEnabledScrollToAction.nestedScrollToAction
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.withAccount

class SplitEditTest : BaseExpenseEditTest() {
    private val accountLabel1 = "Test label 1"

    private val baseIntent: Intent
        get() = intentForNewTransaction.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_SPLIT)
        }

    private fun assertUncommittedCount(uri: Uri, count: Int) {
        assertThat(repository.count(uri, "$KEY_STATUS= $STATUS_UNCOMMITTED"))
            .isEqualTo(count)
    }

    private fun assertUncommittedTransactions(count: Int) {
        assertUncommittedCount(TransactionProvider.UNCOMMITTED_URI, count)
    }

    private fun assertUncommittedTemplates(count: Int) {
        assertUncommittedCount(TransactionProvider.TEMPLATES_UNCOMMITTED_URI, count)
    }

    private fun launch(
        excludeFromTotals: Boolean = false,
        configureIntent: Intent.() -> Unit = {}
    ) {
        account1 = buildAccount(accountLabel1, excludeFromTotals = excludeFromTotals)
        testScenario = ActivityScenario.launchActivityForResult(
            baseIntent.apply(configureIntent)
        )
    }

    private fun launchEdit(excludeFromTotals: Boolean = false) {
        launch(excludeFromTotals) { putExtra(KEY_ROWID, prepareSplit(account1.id)) }
    }

    /*
    Verify resolution of
    https://github.com/mtotschnig/MyExpenses/issues/987
     */
    @Test
    fun bug987() {
        val account2 = buildAccount("Test Account 2")
        launch()
        closeSoftKeyboard()
        onView(withId(R.id.CREATE_PART_COMMAND)).perform(scrollTo(), click())
        setAmount(50)
        setOperationType(Transactions.TYPE_TRANSFER)
        onView(withId(R.id.TransferAccount)).perform(scrollTo(), click())
        onData(
            allOf(
                instanceOf(IdHolder::class.java),
                withAccount(account2.label)
            )
        ).perform(click())
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save part
        onView(withId(R.id.Account)).perform(scrollTo(), click())
        onData(
            allOf(
                instanceOf(IdHolder::class.java),
                withAccount(account2.label)
            )
        ).perform(click())
        onView(withId(R.id.Account)).check(
            matches(
                withSpinnerText(
                    CoreMatchers.containsString(
                        account1.label
                    )
                )
            )
        )
    }

    @Test
    fun canceledSplitCleanup() {
        launch()
        assertUncommittedTransactions(1)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
        assertUncommittedTransactions(0)
    }

    @Test
    fun canceledTemplateSplitCleanup() {
        launch { putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true) }
        assertUncommittedTemplates(1)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
        assertUncommittedTemplates(0)
    }

    /*
    This also verifies #1316
     */
    @Test
    fun loadCancelCleanup() {
        launchEdit()
        assertUncommittedTransactions(2)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
        assertUncommittedTransactions(0)
    }

    @Test
    fun loadCancelRotateCleanup() {
        launchEdit()
        assertUncommittedTransactions(2)
        rotate()
        assertUncommittedTransactions(2)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
        assertUncommittedTransactions(0)
    }

    private fun verifyTypeToggle(initiallyChecked: Boolean) {
        checkType(initiallyChecked)
        toggleType()
        checkType(!initiallyChecked)
    }

    @Test
    fun createPartAndSave() {
        launch()
        verifyTypeToggle(false)
        createParts(5)
        verifyTypeToggle(true)
        verifyTypeToggle(false)
        onView(withId(R.id.CREATE_COMMAND)).perform(click())
        assertFinishing()
    }

    @Test
    fun withAccountExcludedFromTotalsCreateNewSplit() {
        launch(excludeFromTotals = true)
        createParts(1)
        onView(withId(R.id.CREATE_COMMAND)).perform(click())
        assertFinishing()
    }

    @Test
    fun withAccountExcludedFromTotalsEditExistingSplit() {
        launchEdit(excludeFromTotals = true)
        onView(withId(R.id.list)).check(matches(hasChildCount(2)))
    }

    private fun createParts(
        times: Int,
        amount: Int = 50,
        toggleType: Boolean = false,
        initialChildCount: Int = 0
    ) {
        repeat(times) {
            closeSoftKeyboard()
            onView(withId(R.id.CREATE_PART_COMMAND)).perform(nestedScrollToAction(), click())
            onView(withId(R.id.MANAGE_TEMPLATES_COMMAND)).check(doesNotExist())
            onView(withId(R.id.CREATE_TEMPLATE_COMMAND)).check(doesNotExist())
            if (toggleType) {
                toggleType()
            }
            setAmount(amount)
            onView(withId(R.id.CREATE_COMMAND)).perform(click())//save part
            onView(withId(R.id.list)).check(matches(hasChildCount(initialChildCount + it + 1)))
        }
    }

    @Test
    fun loadEditSaveSplit() {
        launchEdit()
        onView(withId(R.id.list)).check(matches(hasChildCount(2)))
        closeSoftKeyboard()
        scrollTo(R.id.list)
        onView(withId(R.id.list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    click()
                )
            )
        onView(withText(R.string.menu_edit)).perform(click())
        setAmount(150)
        onView(withId(R.id.MANAGE_TEMPLATES_COMMAND)).check(doesNotExist())
        onView(withId(R.id.CREATE_TEMPLATE_COMMAND)).check(doesNotExist())
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save part
        onView(withId(R.id.CREATE_COMMAND)).perform(click())//save parent succeeds
        assertFinishing()
    }

    @Test
    fun create_and_save() {
        launch()
        createParts(1)
        clickMenuItem(R.id.SAVE_AND_NEW_COMMAND, false) //toggle save and new on
        onView(withId(R.id.CREATE_COMMAND)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.save_transaction_and_new_success)))
        waitForSnackbarDismissed()
        createParts(1)
        clickMenuItem(R.id.SAVE_AND_NEW_COMMAND, false) //toggle save and new off
        closeKeyboardAndSave()
        assertFinishing()
    }

    /**
     * Bug https://github.com/mtotschnig/MyExpenses/issues/1323
     */
    @Test
    fun createPartsWhichFlipSign() {
        launch()
        createParts(1, 50, false)
        createParts(1, 100, true, initialChildCount = 1)
    }
}