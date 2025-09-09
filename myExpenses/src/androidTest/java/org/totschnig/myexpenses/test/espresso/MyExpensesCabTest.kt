package org.totschnig.myexpenses.test.espresso

import android.net.Uri
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_CONTEXT_MENU
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.TEST_TAG_SELECT_DIALOG
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.isOrchestrated

@TestShard3
class MyExpensesCabTest : BaseMyExpensesTest() {
    private val origListSize = 6
    private lateinit var account: Account
    private var op0Id: Long = 0

    private fun doLaunch(excludeFromTotals: Boolean = false, initialOpCount: Int = 6) {
        account = buildAccount("Test account 1", excludeFromTotals = excludeFromTotals)
        val op0 = Transaction.getNewInstance(account.id, homeCurrency)
        op0.amount = Money(homeCurrency, -100L)
        op0.save(contentResolver)
        op0Id = op0.id
        for (i in 2..initialOpCount) {
            repository.addAttachments(
                op0.id,
                listOf(Uri.parse("file:///android_asset/screenshot.jpg"))
            )
            op0.amount = Money(homeCurrency, -100L * i)
            op0.date -= 10000
            op0.saveAsNew(contentResolver)
        }
        launch(account.id)
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            // For unidentified reason, tests in this class fail when run with the whole package
            // "am instrument -e package org.totschnig.myexpenses.test.espresso"
            // but work when run on class level
            // "am instrument -e class org.totschnig.myexpenses.test.espresso.MyExpensesCabTest"
            Assume.assumeTrue(isOrchestrated)
        }
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
        }
    }

    @Test
    fun cloneCommandIncreasesListSize() {
        doLaunch()
        assertListSize(origListSize)
        clickContextItem(R.string.menu_clone_transaction)
        closeKeyboardAndSave()
        assertListSize(origListSize + 1)
    }

    @Test
    fun editCommandKeepsListSize() {
        doLaunch()
        assertListSize(origListSize)
        clickContextItem(R.string.menu_edit)
        closeKeyboardAndSave()
        assertListSize(origListSize)
    }

    @Test
    fun createTemplateCommandCreatesTemplate() {
        doLaunch()
        val templateTitle = "Espresso Template Test"
        assertListSize(origListSize)
        clickContextItem(R.string.menu_create_template_from_transaction)
        onView(withId(R.id.Title)).perform(
            closeSoftKeyboard(),
            typeText(templateTitle),
            closeSoftKeyboard()
        )
        closeKeyboardAndSave()
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        onView(withText(Matchers.`is`(templateTitle)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun deleteCommandDecreasesListSize() {
        doLaunch()
        doDelete(useCab = false, cancel = false)
    }

    @Test
    fun deleteCommandDecreasesListSizeCab() {
        doLaunch()
        doDelete(useCab = true, cancel = false)
    }

    @Test
    fun deleteCommandCancelKeepsListSize() {
        doLaunch()
        doDelete(useCab = false, cancel = true)
    }

    @Test
    fun deleteCommandCancelKeepsListSizeCab() {
        doLaunch()
        doDelete(useCab = true, cancel = true)
    }

    private fun doDelete(useCab: Boolean, cancel: Boolean) {
        assertListSize(origListSize)
        triggerDelete(useCab)
        onView(withText(if (cancel) android.R.string.cancel else R.string.menu_delete))
            .inRoot(isDialog())
            .perform(click())
        assertListSize(if (cancel) origListSize else origListSize - 1)
    }

    @Test
    fun deleteCommandWithVoidOptionCab() {
        doLaunch()
        doDeleteCommandWithVoidOption(true)
    }

    @Test
    fun deleteCommandWithVoidOption() {
        doLaunch()
        doDeleteCommandWithVoidOption(false)
    }

    private fun triggerDelete(useCab: Boolean) {
        if (useCab) {
            openCab(R.id.DELETE_COMMAND)
        } else {
            clickContextItem(R.string.menu_delete)
        }
    }

    private fun doDeleteCommandWithVoidOption(useCab: Boolean) {
        assertListSize(origListSize)
        triggerDelete(useCab)
        onView(withId(R.id.checkBox)).inRoot(isDialog()).perform(click())
        onView(withText(R.string.menu_delete)).perform(click())
        val voidStatus = getString(R.string.status_void)
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren().onFirst()
            .assertContentDescriptionEquals(voidStatus)
        assertListSize(origListSize)
        clickContextItem(R.string.menu_undelete_transaction)
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren().onFirst()
            .assert(hasContentDescription(voidStatus).not())
        assertListSize(origListSize)
    }

    @Test
    fun splitCommandCreatesSplitTransaction() {
        doLaunch()
        doSplitCommandTest()
    }

    @Test
    fun withAccountExcludedFromTotalsSplitCommandCreatesSplitTransaction() {
        doLaunch(true)
        doSplitCommandTest()
    }

    private fun doSplitCommandTest() {
        openCab(R.id.SPLIT_TRANSACTION_COMMAND)
        handleContribDialog(ContribFeature.SPLIT_TRANSACTION)
        onView(withText(R.string.menu_split_transaction))
            .perform(click())
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren().onFirst()
            .assertTextContains(getString(R.string.split_transaction))
    }

    @Test
    fun cabIsRestoredAfterOrientationChange() {
        doLaunch()
        openCab(null)
        doWithRotation {
            onView(withId(androidx.appcompat.R.id.action_mode_bar)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun contextForSealedAccount() {
        doLaunch()
        testScenario.onActivity {
            it.viewModel.setSealed(account.id, true)
        }
        openCab(null)
        onView(withId(androidx.appcompat.R.id.action_mode_bar)).check(doesNotExist())
        composeTestRule.onNodeWithTag(TEST_TAG_CONTEXT_MENU).onChildAt(0).assert(hasText(getString(R.string.details)))
        composeTestRule.onNodeWithTag(TEST_TAG_CONTEXT_MENU).onChildAt(1).assert(hasText(getString(R.string.filter)))
    }

    @Test
    fun transformToTransfer() {
        doLaunch(initialOpCount = 1)
        val transferAccount = buildAccount("Test account 2")
        clickContextItem(R.string.menu_transform_to_transfer)
        composeTestRule.onNode(hasAnyAncestor(hasTestTag(TEST_TAG_SELECT_DIALOG)) and hasText("Test account 2"))
            .performClick()
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(android.R.id.button1)).perform(click())
        val op = Transaction.getInstanceFromDb(contentResolver, op0Id, homeCurrency)
        assertThat(op.isTransfer).isTrue()
        assertThat(op.transferAccountId).isEqualTo(transferAccount.id)
        cleanup {
            repository.deleteAccount(transferAccount.id)
        }
    }

    @Test
    fun unlinkTransfer() {
        account = buildAccount("Test account 1")
        val transferAccount = buildAccount("Test account 2")
        val op0 = Transfer.getNewInstance(account.id, homeCurrency, transferAccount.id)
        op0.amount = Money(homeCurrency, -100L)
        op0.save(contentResolver)
        op0Id = op0.id
        val transferPeer = op0.transferPeer!!
        launch(account.id)
        clickContextItem(R.string.menu_unlink_transfer)
        onView(withId(android.R.id.button1)).perform(click())
        assertThat(
            Transaction.getInstanceFromDb(
                contentResolver,
                op0Id,
                homeCurrency
            ).isTransfer
        ).isFalse()
        assertThat(
            Transaction.getInstanceFromDb(
                contentResolver,
                transferPeer,
                homeCurrency
            ).isTransfer
        ).isFalse()
        cleanup {
            repository.deleteAccount(transferAccount.id)
        }
    }

    @Test
    fun linkTransfer() {
        account = buildAccount("Test account 1")
        val transferAccount = buildAccount("Test account 2")
        val op0 = Transaction.getNewInstance(account.id, homeCurrency)
        op0.amount = Money(homeCurrency, -100L)
        op0.save(contentResolver)
        op0Id = op0.id
        val peer = Transaction.getNewInstance(transferAccount.id, homeCurrency)
        peer.amount = Money(homeCurrency, 100L)
        peer.save(contentResolver)
        val currencyId = contentResolver.query(
            TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(homeCurrency.code).build(),
            null, null, null, null
        )!!.use {
            it.moveToFirst()
            it.getLong(DatabaseConstants.KEY_ROWID)
        }
        launch(-currencyId)
        assertListSize(2)
        openCab(null)
        listNode.onChildren()[1].performClick()
        clickMenuItem(R.id.LINK_TRANSFER_COMMAND, true)
        onView(withId(android.R.id.button1)).perform(click())
        val op = Transaction.getInstanceFromDb(contentResolver, op0Id, homeCurrency) as Transfer
        assertThat(op.isTransfer).isTrue()
        assertThat(op.transferAccountId).isEqualTo(transferAccount.id)
        assertThat(op.transferPeer).isEqualTo(peer.id)
        cleanup {
            repository.deleteAccount(transferAccount.id)
        }
    }
}