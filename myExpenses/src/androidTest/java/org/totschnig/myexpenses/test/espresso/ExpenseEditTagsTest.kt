package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteAllTags
import org.totschnig.myexpenses.db2.insertTemplate
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.db2.saveTagsForTemplate
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.db2.writeTag
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_2
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TAG_LABEL
import org.totschnig.myexpenses.testutils.TEMPLATE_TITLE
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.shared_test.TransactionData

private const val TAG_LABEL_2 = "Unwichtig"

class ExpenseEditTagsTest : BaseExpenseEditTest() {

    var tagId1: Long = 0
    var tagId2: Long = 0

    @Before
    fun baseFixture() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        tagId1 = repository.writeTag(TAG_LABEL)
        tagId2 = repository.writeTag(TAG_LABEL_2)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAllTags()
        }
    }

    @Test
    fun shouldSaveTags() = runTest {
        launch()
        setAmount(101)
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withText(TAG_LABEL)).perform(click())
        clickFab() // confirm tag selection
        clickFab() // save transaction
        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = -10100,
                tags = listOf(tagId1)
            )
        )
    }

    @Test
    fun shouldSaveTagsForBothSidesOfTransfer() = runTest {
        val account2 = buildAccount(
            ACCOUNT_LABEL_2
        )
        launch()
        setOperationType(Transactions.TYPE_TRANSFER)
        setAmount(101)
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withText(TAG_LABEL)).perform(click())
        clickFab() // confirm tag selection
        clickFab() // save transaction
        val transfer = repository.loadTransactions(account1.id).first().id
        val peer = repository.loadTransactions(account2.id).first().id
        assertTransaction(
            id = transfer,
            TransactionData(
                accountId = account1.id,
                amount = -10100,
                tags = listOf(tagId1),
                category = transferCategoryId,
                transferAccount = account2.id,
                transferPeer = peer
            )
        )
        assertTransaction(
            id = peer,
            TransactionData(
                accountId = account2.id,
                amount = 10100,
                tags = listOf(tagId1),
                category = transferCategoryId,
                transferAccount = account1.id,
                transferPeer = transfer
            )
        )
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun shouldSaveTagsForTemplate() = runTest {
        launchNewTemplate()
        setAmount(101)
        setTitle()
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withText(TAG_LABEL)).perform(click())
        clickFab() // confirm tag selection
        clickFab() // save transaction
        assertTemplate(
            expectedAccount = account1.id,
            expectedAmount = -10100,
            expectedTags = listOf(TAG_LABEL)
        )
    }

    @Test
    fun shouldLoadAndUpdateTags() = runTest {
        val transaction = repository.insertTransaction(
            accountId = account1.id,
            amount = 100
        )
        repository.saveTagsForTransaction(longArrayOf(tagId1), transaction.id)
        launch(getIntentForEditTransaction(transaction.id))
        onView(
            allOf(
                isDescendantOfA(withId(R.id.TagGroup)),
                withText(TAG_LABEL)
            )
        ).check(matches(isDisplayed()))
        closeSoftKeyboard()
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withText(TAG_LABEL)).perform(click()) //unselect
        onView(withText(TAG_LABEL_2)).perform(click()) //select
        clickFab() // confirm tag selection
        clickFab() // save transaction
        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = 100,
                tags = listOf(tagId2)
            )
        )
    }

    @Test
    fun shouldLoadAndUpdateTagsForTemplate() = runTest {
        val template = repository.insertTemplate(
            title = TEMPLATE_TITLE,
            accountId = account1.id,
            amount = 100,
        )
        repository.saveTagsForTemplate(listOf(tagId1), template.id)
        launch(getIntentForEditTemplate(template.id))
        onView(
            allOf(
                isDescendantOfA(withId(R.id.TagGroup)),
                withText(TAG_LABEL)
            )
        ).check(matches(isDisplayed()))
        closeSoftKeyboard()
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withText(TAG_LABEL)).perform(click()) //unselect
        onView(withText(TAG_LABEL_2)).perform(click()) //select
        clickFab() // confirm tag selection
        clickFab() // save transaction
        assertTemplate(
            expectedAccount = account1.id,
            expectedAmount = 100,
            expectedTags = listOf(TAG_LABEL_2)
        )
    }

    @Test
    fun shouldLoadAndUpdateTagsForTransactionFromTemplate() = runTest {
        val template = repository.insertTemplate(
            title = TEMPLATE_TITLE,
            accountId = account1.id,
            amount = 100,
        )
        repository.saveTagsForTemplate(listOf(tagId1), template.id)
        launch(intent.apply {
            action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
            putExtra(KEY_TEMPLATEID, template.id)
        })
        onView(
            allOf(
                isDescendantOfA(withId(R.id.TagGroup)),
                withText(TAG_LABEL)
            )
        ).check(matches(isDisplayed()))
        closeSoftKeyboard()
        onView(withId(R.id.TagSelection)).perform(click())
        onView(withText(TAG_LABEL)).perform(click()) //unselect
        onView(withText(TAG_LABEL_2)).perform(click()) //select
        clickFab() // confirm tag selection
        clickFab() // save transaction
        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = 100,
                tags = listOf(tagId2)
            )
        )
    }
}