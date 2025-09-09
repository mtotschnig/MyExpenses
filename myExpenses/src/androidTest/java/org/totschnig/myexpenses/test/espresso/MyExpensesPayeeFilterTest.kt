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
import org.totschnig.myexpenses.db2.setParentId
import org.totschnig.myexpenses.fragment.PartiesList
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.ui.DisplayParty

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
        val currency = CurrencyUnit.DebugInstance
        account =  buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, -1200L)
        op.party = DisplayParty(p1, payee1)
        op.save(contentResolver)
        op.party = DisplayParty(p2, payee2)
        op.date = op.date - 10000
        op.saveAsNew(contentResolver)
        d = repository.createParty(Party(name = duplicate))!!.id
        repository.setParentId(d, p2)
        op.party = DisplayParty(d, duplicate)
        op.date = op.date - 10000
        op.saveAsNew(contentResolver)
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