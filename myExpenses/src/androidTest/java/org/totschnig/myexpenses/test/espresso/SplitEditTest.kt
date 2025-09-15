package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
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
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaScrollInteractions
import com.adevinta.android.barista.internal.matcher.HelperMatchers.menuIdMatcher
import com.adevinta.android.barista.internal.viewaction.NestedEnabledScrollToAction.nestedScrollToAction
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.TestShard5
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.isOrchestrated
import org.totschnig.myexpenses.testutils.withAccountGrouped

@TestShard5
class SplitEditTest : BaseExpenseEditTest() {
    private val accountLabel1 = "Test label 1"
    private val accountLabel2 = "Test label 2"

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            // For unidentified reason, tests in this class fail when run with the whole package
            // "am instrument -e package org.totschnig.myexpenses.test.espresso"
            // but work when run on class level
            // "am instrument -e class org.totschnig.myexpenses.test.espresso.SplitEditTest"
            Assume.assumeTrue(isOrchestrated)
        }
    }

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

    private fun launchWithAccountSetup(
        excludeFromTotals: Boolean = false,
        configureIntent: Intent.() -> Unit = {},
    ) {
        account1 = buildAccount(accountLabel1, excludeFromTotals = excludeFromTotals)
        launchForResult(baseIntent.apply(configureIntent))
    }

    private fun launchEdit(excludeFromTotals: Boolean = false) {
        launchWithAccountSetup(excludeFromTotals) { putExtra(KEY_ROWID, prepareSplit(account1.id)) }
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
        }
    }

    /*
    Verify resolution of
    https://github.com/mtotschnig/MyExpenses/issues/987
     */
    @Test
    fun bug987() {
        val account2 = buildAccount("Test Account 2")
        launchWithAccountSetup()
        closeSoftKeyboard()
        onView(withId(R.id.CREATE_PART_COMMAND)).perform(scrollTo(), click())
        setAmount(50)
        setOperationType(Transactions.TYPE_TRANSFER)
        onView(withId(R.id.TransferAccount)).perform(scrollTo(), click())
        onData(
            withAccountGrouped(account2.label)
        ).perform(click())
        clickFab()//save part
        setAccount(account2.label)
        checkAccount(account1.label)
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }

    /*
    Verify https://github.com/mtotschnig/MyExpenses/issues/1633 and
    resolution of https://github.com/mtotschnig/MyExpenses/issues/1664
 */
    @Test
    fun bug1664() {
        buildAccount("Test Account 2") //for transfer
        launchWithAccountSetup()
        setAmount(10)
        onView(withId(R.id.CREATE_PART_COMMAND)).perform(nestedScrollToAction(), click())
        clickMenuItem(R.id.SAVE_AND_NEW_COMMAND)
        setAmount(3)
        clickFab()//save part and new
        checkAmount(7)
        setOperationType(Transactions.TYPE_TRANSFER)
        checkAmount(7)
        clickFab()//saves part and returns to main
        checkPartCount(2)
    }

    @Test
    fun canceledSplitCleanup() {
        launchWithAccountSetup()
        assertUncommittedTransactions(1)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
        assertUncommittedTransactions(0)
    }

    @Test
    fun canceledTemplateSplitCleanup() {
        launchWithAccountSetup { putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true) }
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
        doWithRotation {
            assertUncommittedTransactions(2)
            closeSoftKeyboard()
            pressBackUnconditionally()
            assertCanceled()
            assertUncommittedTransactions(0)
        }
    }

    private fun verifyTypeToggle(initiallyChecked: Boolean) {
        checkType(initiallyChecked)
        toggleType()
        checkType(!initiallyChecked)
    }

    @Test
    fun createPartAndSave() {
        launchWithAccountSetup()
        verifyTypeToggle(false)
        createParts(5)
        verifyTypeToggle(true)
        verifyTypeToggle(false)
        clickFab()
        assertFinishing()
    }

    @Test
    fun withAccountExcludedFromTotalsCreateNewSplit() {
        launchWithAccountSetup(excludeFromTotals = true)
        createParts(1)
        clickFab()
        assertFinishing()
    }

    @Test
    fun withAccountExcludedFromTotalsEditExistingSplit() {
        launchEdit(excludeFromTotals = true)
        checkPartCount(2)
    }

    private fun checkPartCount(count: Int) {
        onView(withId(R.id.list)).check(matches(hasChildCount(count)))
    }

    private fun createParts(
        times: Int,
        amount: Int = 50,
        toggleType: Boolean = false,
        initialChildCount: Int = 0,
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
            clickFab()//save part
            checkPartCount(initialChildCount + it + 1)
        }
    }

    @Test
    fun loadEditSaveSplit() {
        launchEdit()
        checkPartCount(2)
        closeSoftKeyboard()
        BaristaScrollInteractions.scrollTo(R.id.list)
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
        clickFab()//save part
        checkAmount(100) // amount should not be updated (https://github.com/mtotschnig/MyExpenses/issues/1349)
        setAmount(200)
        clickFab()//save parent succeeds
        assertFinishing()
    }

    @Test
    fun create_and_save() {
        launchWithAccountSetup()
        createParts(1)
        clickMenuItem(R.id.SAVE_AND_NEW_COMMAND) //toggle save and new on
        clickFab()
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.save_transaction_and_new_success)))
        waitForSnackbarDismissed()
        createParts(1)
        clickMenuItem(R.id.SAVE_AND_NEW_COMMAND) //toggle save and new off
        closeKeyboardAndSave()
        assertFinishing()
    }

    /**
     * Bug https://github.com/mtotschnig/MyExpenses/issues/1323
     */
    @Test
    fun createPartsWhichFlipSign() {
        launchWithAccountSetup()
        createParts(1, 50)
        createParts(1, 100, true, initialChildCount = 1)
    }

    @Test
    fun createPartsAndDelete() {
        account1 = buildAccount(accountLabel1)
        val account2 = buildAccount(accountLabel2)
        launchForResult(baseIntent.apply {
            putExtra(DatabaseConstants.KEY_ACCOUNTID, account2.id)
        })
        createParts(2, 50)
        checkAccount(accountLabel2)
        onView(withId(R.id.list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<SplitPartRVAdapter.ViewHolder>(
                0,
                click()
            )
        )
        onData(menuIdMatcher(R.id.DELETE_COMMAND)).perform(click())
        checkAccount(accountLabel2)
        createParts(2, 50, initialChildCount = 1)
        checkAccount(accountLabel2)
        clickFab()
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }
}