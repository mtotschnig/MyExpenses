package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_SELECT_DIALOG
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup
import java.time.ZonedDateTime

@TestShard4
class RemapTest : BaseMyExpensesTest() {

    private fun createMoney() = Money(homeCurrency, 2000)

    @Test
    fun remapAccountShouldUpdateTransferPeer() {
        val account1 = buildAccount("K1")
        val account2 = buildAccount("K2")
        val account3 = buildAccount("K3")
        Transaction(account1.id, createMoney()).also {
            it.setDate(ZonedDateTime.now().minusDays(4))
            it.save(contentResolver)
        }
        val transfer = Transfer(account1.id, createMoney(), account2.id).also {
            it.save(contentResolver)
        }
        doRemapAccount(account1.id, "K3")
        val self = getTransactionFromDb(transfer.id)
        assertThat(self.accountId).isEqualTo(account3.id)
        assertThat(self.transferAccountId).isEqualTo(account2.id)
        val peer = getTransactionFromDb(transfer.transferPeer!!)
        assertThat(peer.accountId).isEqualTo(account2.id)
        assertThat(peer.transferAccountId).isEqualTo(account3.id)
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
            repository.deleteAccount(account3.id)
        }
    }

    @Test
    fun remapAndCloneAccountForSplit() {
        val account1 = buildAccount("K1")
        val account2 = buildAccount("K2")
        prepareSplit(account1.id)
        doRemapAccount(account1.id, "K2", true)
        contentResolver.query(
            TRANSACTIONS_URI,
            arrayOf(
                KEY_ACCOUNTID,
                "(SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS parent WHERE $KEY_ROWID = $VIEW_COMMITTED.$KEY_PARENTID)"
            ),
            "$KEY_PARENTID IS NOT NULL",
            null, null
        )!!.use { cursor ->
            cursor.asSequence.forEach {
                assertThat(it.getLong(1)).isEqualTo(it.getLong(0))
            }
        }
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)

        }
    }

    @Test
    fun remapCategoryShouldUpdateTransferPeer() {
        doRemapCategory(false)
    }

    @Test
    fun remapAndCloneCategoryShouldUpdateTransferPeer() {
        doRemapCategory(true)
    }

    private fun doRemapCategory(doClone: Boolean) {
        val account1 = buildAccount("K1")
        val account2 = buildAccount("K2")
        Transfer(account1.id, createMoney(), account2.id).also {
            it.save(contentResolver)
        }
        val catLabel = "Food"
        val catId = writeCategory(catLabel)
        launch(account1.id)
        openCab(R.id.REMAP_PARENT)
        onView(withText(R.string.category)).perform(click())
        composeTestRule.onNodeWithText(catLabel).performClick()
        confirmRemap(doClone)
        verifyCatIdsForTransfers()
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
            repository.deleteCategory(catId)
        }
    }

    private fun verifyCatIdsForTransfers() {
        contentResolver.query(
            TRANSACTIONS_URI,
            arrayOf(
                KEY_CATID,
                "(SELECT $KEY_CATID FROM $TABLE_TRANSACTIONS peer WHERE $KEY_ROWID = $VIEW_COMMITTED.$KEY_TRANSFER_PEER)"
            ),
            null, null, null
        )!!.use { cursor ->
            cursor.asSequence.forEach {
                assertThat(it.getLong(1)).isEqualTo(it.getLong(0))
            }
        }
    }


    private fun doRemapAccount(accountId: Long, target: String, doClone: Boolean = false) {
        launch(accountId)
        openCab(R.id.REMAP_PARENT)
        onView(withText(R.string.account)).perform(click())
        composeTestRule.onAllNodesWithText(target)
            .filterToOne(hasAnyAncestor(hasTestTag(TEST_TAG_SELECT_DIALOG)))
            .performClick()
        //Espresso recorder

        onView(
            allOf(
                withId(android.R.id.button1),
                withText(android.R.string.ok)
            )
        ).perform(ViewActions.scrollTo(), click())
        confirmRemap(doClone)
    }

    private fun confirmRemap(doClone: Boolean) {
        if (doClone) {
            onView(withId(R.id.checkBox)).inRoot(isDialog()).perform(click())
        }
        onView(
            allOf(
                withId(android.R.id.button1),
                withText(if (doClone) R.string.button_label_clone_and_remap else R.string.menu_remap),
            )
        ).inRoot(isDialog()).perform(ViewActions.scrollTo(), click())
    }

}