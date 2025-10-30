package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.graphics.Color
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
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaScrollInteractions
import com.adevinta.android.barista.internal.matcher.HelperMatchers.menuIdMatcher
import com.adevinta.android.barista.internal.viewaction.NestedEnabledScrollToAction.nestedScrollToAction
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_2
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.CATEGORY_LABEL
import org.totschnig.myexpenses.testutils.DEBT_LABEL
import org.totschnig.myexpenses.testutils.PARTY_NAME
import org.totschnig.myexpenses.testutils.TestShard5
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.isOrchestrated
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.withAccountGrouped
import org.totschnig.myexpenses.viewmodel.data.Debt
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.withViewCount

@TestShard5
class SplitEditTest : BaseExpenseEditTest() {

    private var partyId: Long = 0

    private var debtId: Long = 0

    private var categoryId: Long = 0

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

    private fun launchEdit(excludeFromTotals: Boolean = false): Long {
        var id: Long = 0
        launchWithAccountSetup(excludeFromTotals) {
            id = prepareSplit(account1.id)
            putExtra(KEY_ROWID, id)
        }
        return id
    }

    private fun launchWithAccountSetup(
        excludeFromTotals: Boolean = false,
        type: Int = Transactions.TYPE_SPLIT,
        configureIntent: Intent.() -> Unit = {},
    ) {
        account1 = buildAccount(ACCOUNT_LABEL_1, excludeFromTotals = excludeFromTotals)
        launchForResult(getBaseIntent(type).apply(configureIntent))
    }

    private fun launchEditTemplate(): Long {
        var id: Long = 0
        launchWithAccountSetup {
            id = prepareSplitTemplate(account1.id)
            putExtra(KEY_TEMPLATEID, id)
        }
        return id
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
        onView(withId(R.id.Account)).check(matches(not(isEnabled())))
        setAmount(50)
        setOperationType(Transactions.TYPE_TRANSFER)
        onView(withId(R.id.Account)).check(matches(not(isEnabled())))
        onView(withId(R.id.TransferAccount)).perform(scrollTo(), click())
        onData(
            withAccountGrouped(account2.label)
        ).perform(click())
        clickFab()//save part
        checkPartCount(1)
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
    fun bug1783() {
        val account2 = buildAccount("Test Account 2", color = Color.RED)
        launchEdit()
        assertThat(repository.count(
            TRANSACTIONS_URI,
            "$KEY_ACCOUNTID = ?",
            arrayOf(account1.id.toString())
        )).isEqualTo(3)
        setAccount("Test Account 2")
        clickFab()
        assertThat(repository.count(
            TRANSACTIONS_URI,
            "$KEY_ACCOUNTID = ?",
            arrayOf(account2.id.toString())
        )).isEqualTo(3)
    }

    @Test
    fun canceledSplitCleanup() {
        launchWithAccountSetup()
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
    }

    @Test
    fun canceledTemplateSplitCleanup() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        launchNewTemplate(Transactions.TYPE_SPLIT)
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
    }

    @Test
    fun newTemplate() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        launchNewTemplate(Transactions.TYPE_SPLIT)
        setOperationType(Transactions.TYPE_SPLIT)
        newTemplateHelper()
    }

    @Test
    fun newTemplateWithTypeSpinner() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        launchNewTemplate(Transactions.TYPE_TRANSACTION)
        setOperationType(Transactions.TYPE_SPLIT)
        newTemplateHelper()
    }

    @Test
    fun newTransactionWithTypeSpinner() {
        unlock()
        launchWithAccountSetup(type = Transactions.TYPE_TRANSACTION)
        setOperationType(Transactions.TYPE_SPLIT)
        createParts(1)
        clickFab()
        assertFinishing()
    }

    private fun newTemplateHelper() {
        createParts(2)
        setTitle()
        clickFab()
        assertFinishing()
        assertTemplate(account1.id, -10000, expectedSplitParts = listOf(-5000,-5000))
    }

    /*
    This also verifies #1316
     */
    @Test
    fun loadCancelCleanup() {
        launchEdit()
        closeSoftKeyboard()
        pressBackUnconditionally()
        assertCanceled()
    }

    @Test
    fun loadCancelRotateCleanup() {
        launchEdit()
        doWithRotation {
            closeSoftKeyboard()
            pressBackUnconditionally()
            assertCanceled()
        }
    }

    private fun verifyTypeToggle(initiallyChecked: Boolean) {
        checkType(initiallyChecked)
        toggleType()
        checkType(!initiallyChecked)
    }

    @Test
    fun createPartAndSave() {
        runTest {
            val partCount = 10
            val partAmount = 50
            launchWithAccountSetup()
            verifyTypeToggle(false)
            setupData()
            createParts(partCount, amount = partAmount, withDebtAndCategory = true)
            verifyTypeToggle(true)
            verifyTypeToggle(false)
            clickFab()
            assertFinishing()
            assertTransaction(
                id = repository.loadTransactions(account1.id).first().id,
                expectedAccount = account1.id,
                expectedAmount = partCount * partAmount * 100L,
                expectedSplitParts = buildList { repeat(partCount) { add(partAmount * 100L)} }
            )
        }
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

    private fun setupData() {
        partyId = repository.createParty(Party.create(name = PARTY_NAME)!!)!!.id
        debtId = repository.saveDebt(
            Debt(
                payeeId = partyId,
                amount = 100,
                id = 0L,
                label = DEBT_LABEL,
                description = "",
                currency = homeCurrency,
                date = System.currentTimeMillis() / 1000,
            )
        )
        categoryId = writeCategory(CATEGORY_LABEL, type = FLAG_INCOME)
    }

    private fun createParts(
        times: Int,
        amount: Int = 50,
        toggleType: Boolean = false,
        initialChildCount: Int = 0,
        withDebtAndCategory: Boolean = false
    ) {
        repeat(times) {
            closeSoftKeyboard()
            onView(withId(R.id.CREATE_PART_COMMAND)).perform(nestedScrollToAction(), click())
            onView(withId(R.id.MANAGE_TEMPLATES_COMMAND)).check(doesNotExist())
            onView(withId(R.id.CREATE_TEMPLATE_COMMAND)).check(doesNotExist())
            checkEffectiveGone(R.id.PayeeRow)
            if (toggleType) {
                toggleType()
            }
            setAmount(amount)
            if(withDebtAndCategory) {
                setDebt()
                setCategory()
            }
            clickFab()//save part
            checkPartCount(initialChildCount + it + 1)
            if (withDebtAndCategory) {
                onView(isRoot()).check(
                    withViewCount(
                        allOf(
                            withSubstring(DEBT_LABEL),
                            withSubstring(CATEGORY_LABEL)

                        ), it + 1)
                )
            }
        }
    }

    @Test
    fun loadEditSaveSplit() {
        val id = launchEdit()
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
        toolbarTitle().check(matches(withText(R.string.menu_edit_split_part_category)))
        clickFab()//save part
        checkAmount(100) // amount should not be updated (https://github.com/mtotschnig/MyExpenses/issues/1349)
        setAmount(200)
        clickFab()//save parent succeeds
        assertFinishing()
        assertTransaction(
            id = id,
            expectedAccount = account1.id,
            expectedAmount = 20000,
            expectedSplitParts = listOf(15000, 5000)
        )
    }

    //delete one item, add another one
    @Test
    fun loadEditSaveSplit2() {
        val id = launchEdit()
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
        onView(withText(R.string.menu_delete)).perform(click())
        createParts(1, 150, initialChildCount = 1)
        checkAmount(100) // amount should not be updated (https://github.com/mtotschnig/MyExpenses/issues/1349)
        setAmount(200)
        clickFab()//save parent succeeds
        assertFinishing()
        assertTransaction(
            id = id,
            expectedAccount = account1.id,
            expectedAmount = 20000,
            expectedSplitParts = listOf(15000, 5000)
        )
    }

    @Test
    fun loadEditSaveSplitTemplate() {
        launchEditTemplate()
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
        toolbarTitle().check(matches(withText(getString(R.string.menu_edit_template) + " (" + getString(R.string.transaction) + ")")))
        clickFab()//save part
        checkAmount(100) // amount should not be updated (https://github.com/mtotschnig/MyExpenses/issues/1349)
        setAmount(200)
        clickFab()//save parent succeeds
        assertFinishing()
        assertTemplate(
            expectedAccount = account1.id,
            expectedAmount = 20000,
            expectedSplitParts = listOf(15000, 5000)
        )
    }

    //delete one item, add another one
    @Test
    fun loadEditSaveSplitTemplate2() {
        launchEditTemplate()
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
        onView(withText(R.string.menu_delete)).perform(click())
        createParts(1, 150, initialChildCount = 1)
        checkAmount(100) // amount should not be updated (https://github.com/mtotschnig/MyExpenses/issues/1349)
        setAmount(200)
        clickFab()//save parent succeeds
        assertFinishing()
        assertTemplate(
            expectedAccount = account1.id,
            expectedAmount = 20000,
            expectedSplitParts = listOf(15000, 5000)
        )
    }

    @Test
    fun createSaveAndNew() {
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
        account1 = buildAccount(ACCOUNT_LABEL_1)
        val account2 = buildAccount(ACCOUNT_LABEL_2)
        launchForResult(getBaseIntent().apply {
            putExtra(KEY_ACCOUNTID, account2.id)
        })
        createParts(2, 50)
        checkAccount(ACCOUNT_LABEL_2)
        onView(withId(R.id.list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<SplitPartRVAdapter.ViewHolder>(
                0,
                click()
            )
        )
        onData(menuIdMatcher(R.id.DELETE_COMMAND)).perform(click())
        checkAccount(ACCOUNT_LABEL_2)
        createParts(2, 50, initialChildCount = 1)
        checkAccount(ACCOUNT_LABEL_2)
        clickFab()
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }
}