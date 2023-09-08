package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_SELECT_DIALOG
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.childAtPosition
import java.time.ZonedDateTime

class RemapAccountTest : BaseMyExpensesTest() {

    private lateinit var account1: Account
    private lateinit var account2: Account
    private lateinit var account3: Account
    private lateinit var transfer: Transfer

    private fun createMoney() = Money(homeCurrency, 2000)
    @Before
    fun fixture() {
        account1 = buildAccount("K1")
        account2 = buildAccount("K2")
        account3 = buildAccount("K3")
        Transaction(account1.id, createMoney()).also {
            it.setDate(ZonedDateTime.now().minusDays(4))
            it.save(contentResolver)
        }
        transfer = Transfer(account1.id, createMoney(), account2.id).also {
            it.save(contentResolver)
        }
        launch(account1.id)
    }

    @Test
    fun remapAccountShouldUpdateTransferPeer() {
        openCab(R.id.REMAP_PARENT)
        onView(allOf(withText(R.string.account))).perform(click())
        composeTestRule.onAllNodesWithText("K3")
            .filterToOne(hasAnyAncestor(hasTestTag(TEST_TAG_SELECT_DIALOG)))
            .performClick()
        //Espresso recorder

        onView(
            Matchers.allOf(
                ViewMatchers.withId(android.R.id.button1), withText(android.R.string.ok),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        ).perform(ViewActions.scrollTo(), click())
        onView(
            Matchers.allOf(
                ViewMatchers.withId(android.R.id.button1), withText(R.string.menu_remap),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        ).perform(ViewActions.scrollTo(), click())
        val self = getTransactionFromDb(transfer.id)
        Truth.assertThat(self.accountId).isEqualTo(account3.id)
        Truth.assertThat(self.transferAccountId).isEqualTo(account2.id)
        val peer = getTransactionFromDb(transfer.transferPeer!!)
        Truth.assertThat(peer.accountId).isEqualTo(account2.id)
        Truth.assertThat(peer.transferAccountId).isEqualTo(account3.id)
    }
}