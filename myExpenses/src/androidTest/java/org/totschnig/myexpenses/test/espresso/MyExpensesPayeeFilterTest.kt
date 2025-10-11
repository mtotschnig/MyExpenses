package org.totschnig.myexpenses.test.espresso

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteParty
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.fragment.PartiesList
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import java.time.LocalDateTime

@TestShard3
class MyExpensesPayeeFilterTest: BaseMyExpensesTest() {
    private lateinit var account: Account
    private var payee1 = "John Doe"
    private var payee2 = "Hinz Finz"
    private var duplicate = "Finz Hinz"
    private var p1: Long = 0
    private var p2: Long = 0
    private var d: Long = 0

    @Before
    fun fixture() {
        p1 = repository.createParty(Party(name = payee1))!!.id
        p2 = repository.createParty(Party(name = payee2))!!.id
        account =  buildAccount("Test account 1")
        repository.insertTransaction(
            accountId = account.id,
            amount = -1200L,
            payeeId = p1
        )
        repository.insertTransaction(
            accountId = account.id,
            amount = -1200L,
            payeeId = p2,
            date = LocalDateTime.now().minusMinutes(1)
        )
        d = repository.createParty(Party(name = duplicate, parentId = p2))!!.id
        repository.insertTransaction(
            accountId = account.id,
            amount = -1200L,
            payeeId = d,
            date = LocalDateTime.now().minusMinutes(2)
        )
        launch(account.id)

        launch(account.id)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
            repository.deleteParty(p1)
            repository.deleteParty(p2)
            repository.deleteParty(d)

        }
    }

    @Test
    fun payeeFilterShouldHideTransaction() {
        checkOriginalState()
        filterOn(payee1)
        assertListSize(1)
        payeeIsDisplayed(payee1,0)
        clearFilters()
        checkOriginalState()
    }

    @Test
    fun payeeFilterShouldShowDuplicate() {
        checkOriginalState()
        filterOn(payee2)
        assertListSize(2)
        payeeIsDisplayed(payee2,0)
        payeeIsDisplayed(duplicate, 1)
        clearFilters()
        checkOriginalState()
    }


    private fun checkOriginalState() {
        assertListSize(3)
        payeeIsDisplayed(payee1, 0)
        payeeIsDisplayed(payee2, 1)
        payeeIsDisplayed(duplicate, 2)
    }

    private fun filterOn(payee: String) {
        selectFilter(R.string.payer_or_payee) {
            onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItem<PartiesList.ViewHolder>(
                    hasDescendant(withText(payee)), clickOnViewChild(R.id.checkBox)))
            clickFab()
        }
    }

    private fun clickOnViewChild(viewId: Int) = object : ViewAction {
        override fun getConstraints() = null

        override fun getDescription() = "Click on a child view with specified id."

        override fun perform(uiController: UiController, view: View) = click().perform(uiController, view.findViewById(viewId))
    }

    private fun payeeIsDisplayed(payee: String, position: Int) {
        assertTextAtPosition(payee, position)
    }

}