package org.totschnig.myexpenses.test.espresso

import android.net.Uri
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.internal.matcher.HelperMatchers.menuIdMatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_2
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.addDebugAttachment
import org.totschnig.shared_test.TransactionData

@TestShard2
class ExpenseEditAttachmentTest : BaseExpenseEditTest() {

    private val resultUri = Uri.parse("file:///android_asset/screenshot.jpg")

    @Test
    fun shouldSaveAttachments() {
        runTest {
            account1 = buildAccount(ACCOUNT_LABEL_1)
            launch()
            setAmount(101)
            onView(withId(R.id.newAttachment)).perform(click())
            onData(menuIdMatcher(R.id.PHOTO_COMMAND)).perform(click())
            clickFab() // save transaction
            assertTransaction(
                id = repository.loadTransactions(account1.id).first().id,
                TransactionData(
                    accountId = account1.id,
                    amount = -10100,
                    attachments = listOf(resultUri)
                )
            )
        }
    }

    @Test
    fun shouldSaveAttachmentsForTransfer() {
        runTest {
            account1 = buildAccount(ACCOUNT_LABEL_1)
            val account2 = buildAccount(ACCOUNT_LABEL_2)
            launchForResult()
            setOperationType(Transactions.TYPE_TRANSFER)
            setAmount(101)
            onView(withId(R.id.newAttachment)).perform(click())
            onData(menuIdMatcher(R.id.PHOTO_COMMAND)).perform(click())
            clickFab() // save transaction
            assertFinishing()
            assertTransfer(
                id = repository.loadTransactions(account1.id).first().id,
                expectedAccount = account1.id,
                expectedAmount = -10100,
                expectedTransferAccount = account2.id,
                expectedPeer = repository.loadTransactions(account2.id).first().id,
                expectedAttachments = listOf(resultUri)
            )
        }
    }

    @Test
    fun shouldLoadAttachments() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        val transaction = repository.insertTransaction(
            accountId = account1.id,
            amount = 100,
            equivalentAmount = 13
        )
        repository.addDebugAttachment(transaction.id)
        launch(getIntentForEditTransaction(transaction.id))
        onView(
            allOf(
                isDescendantOfA(withId(R.id.AttachmentGroup)),
                withContentDescription("screenshot.jpg")
            )
        ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }
}
