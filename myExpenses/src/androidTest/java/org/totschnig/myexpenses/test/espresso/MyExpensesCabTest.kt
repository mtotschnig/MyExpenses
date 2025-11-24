package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_CONTEXT_MENU
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.TEST_TAG_SELECT_DIALOG
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.db2.writeTag
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.CATEGORY_ICON
import org.totschnig.myexpenses.testutils.CATEGORY_LABEL
import org.totschnig.myexpenses.testutils.PARTY_NAME
import org.totschnig.myexpenses.testutils.TAG_LABEL
import org.totschnig.myexpenses.testutils.TEMPLATE_TITLE
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.addDebugAttachment
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.isOrchestrated
import org.totschnig.shared_test.TransactionData
import java.time.LocalDateTime

@TestShard3
class MyExpensesCabTest : BaseMyExpensesTest() {
    private val origListSize = 6
    private lateinit var account: Account
    private var opId: Long = 0
    private var partyId: Long = 0
    private var tagId: Long = 0
    private var categoryId: Long = 0


    private fun doLaunch(excludeFromTotals: Boolean = false, initialOpCount: Int = 6) {
        account = buildAccount("Test account 1", excludeFromTotals = excludeFromTotals)
        partyId = repository.createParty(Party.create(name = PARTY_NAME)!!)!!.id
        categoryId = writeCategory(CATEGORY_LABEL, icon = CATEGORY_ICON, type = FLAG_INCOME)
        tagId = repository.writeTag(TAG_LABEL)

        opId = (1..initialOpCount).map { i ->
            val id = repository.insertTransaction(
                accountId = account.id,
                amount = -100L * i,
                date = LocalDateTime.now().minusMinutes(i.toLong()),
                payeeId = partyId,
                categoryId = categoryId,
            ).id
            repository.saveTagsForTransaction(listOf(tagId), id)
            repository.addDebugAttachment(id)
            id
        }.first()
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
        assertListSize(origListSize)
        clickContextItem(R.string.menu_create_template_from_transaction)
        onView(withId(R.id.Title)).perform(
            closeSoftKeyboard(),
            typeText(TEMPLATE_TITLE),
            closeSoftKeyboard()
        )
        closeKeyboardAndSave()
        assertTemplate(
            expectedAccount = account.id,
            expectedAmount = -100L,
            expectedCategory = categoryId,
            expectedParty = partyId,
            expectedTags = listOf(TAG_LABEL),
        )
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
        runTest {
            openCab(R.id.SELECT_ALL_COMMAND)
            clickMenuItem(R.id.SPLIT_TRANSACTION_COMMAND, true)
            handleContribDialog(ContribFeature.SPLIT_TRANSACTION)
            onView(withText(R.string.menu_split_transaction))
                .perform(click())
            assertTransaction(
                repository.loadTransactions(account.id).first().id,
                TransactionData(
                    accountId = account.id,
                    amount = -2100L,
                    party = partyId,
                    splitParts = buildList {
                        repeat(6) {
                            add(
                                TransactionData(
                                    accountId = account.id,
                                    amount = -100L * (it + 1),
                                    category = categoryId,
                                    tags = listOf(tagId)
                                )
                            )
                        }
                    }
                )
            )
        }
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
        composeTestRule.onNodeWithTag(TEST_TAG_CONTEXT_MENU).onChildAt(0)
            .assert(hasText(getString(R.string.details)))
        composeTestRule.onNodeWithTag(TEST_TAG_CONTEXT_MENU).onChildAt(1)
            .assert(hasText(getString(R.string.filter)))
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
        val op = repository.loadTransaction(opId)
        assertThat(op.isTransfer).isTrue()
        assertThat(op.data.transferAccountId).isEqualTo(transferAccount.id)
        cleanup {
            repository.deleteAccount(transferAccount.id)
        }
    }

    @Test
    fun unlinkTransfer() {
        account = buildAccount("Test account 1")
        val transferAccount = buildAccount("Test account 2")
        val (transfer, peer) = repository.insertTransfer(
            accountId = account.id,
            transferAccountId = transferAccount.id,
            amount = -100L
        )
        launch(account.id)
        clickContextItem(R.string.menu_unlink_transfer)
        onView(withId(android.R.id.button1)).perform(click())
        assertThat(
            repository.loadTransaction(transfer.id).isTransfer
        ).isFalse()
        assertThat(
            repository.loadTransaction(peer!!.id).isTransfer
        ).isFalse()
        cleanup {
            repository.deleteAccount(transferAccount.id)
        }
    }

    @Test
    fun linkTransfer() {
        account = buildAccount("Test account 1")
        val transferAccount = buildAccount("Test account 2")
        val op0Id = repository.insertTransaction(
            accountId = account.id,
            amount = -100L
        ).id
        val peerId = repository.insertTransaction(
            accountId = transferAccount.id,
            amount = 100L
        ).id
        val currencyId = contentResolver.query(
            TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(homeCurrency.code).build(),
            null, null, null, null
        )!!.use {
            it.moveToFirst()
            it.getLong(KEY_ROWID)
        }
        launch(-currencyId)
        assertListSize(2)
        openCab(null)
        listNode.onChildren()[1].performClick()
        clickMenuItem(R.id.LINK_TRANSFER_COMMAND, true)
        onView(withId(android.R.id.button1)).perform(click())
        val op = repository.loadTransaction(op0Id)
        assertThat(op.isTransfer).isTrue()
        assertThat(op.data.transferAccountId).isEqualTo(transferAccount.id)
        assertThat(op.data.transferPeerId).isEqualTo(peerId)
        cleanup {
            repository.deleteAccount(transferAccount.id)
        }
    }
}