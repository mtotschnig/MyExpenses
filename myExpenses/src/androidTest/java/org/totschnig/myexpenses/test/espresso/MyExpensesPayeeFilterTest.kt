package org.totschnig.myexpenses.test.espresso

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.setParentId
import org.totschnig.myexpenses.fragment.PartiesList
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

class MyExpensesPayeeFilterTest: BaseMyExpensesTest() {
    private lateinit var account: Account
    private var payee1 = "John Doe"
    private var payee2 = "Hinz Finz"
    private var duplicate = "Finz Hinz"

    @Before
    fun fixture() {
        val p2 = repository.createParty(Party(name = payee2))
        val currency = CurrencyUnit.DebugInstance
        account =  buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, -1200L)
        op.payee = payee1
        op.save(contentResolver)
        op.payee = payee2
        op.date = op.date - 10000
        op.saveAsNew(contentResolver)
        val d = repository.createParty(Party(name = duplicate))
        repository.setParentId(d.id, p2.id)
        op.payee = duplicate
        op.date = op.date - 10000
        op.saveAsNew(contentResolver)
        launch(account.id)
    }

    @Test
    fun payeeFilterShouldHideTransaction() {
        checkOriginalState()
        filterOn(payee1)
        assertListSize(1)
        payeeIsDisplayed(payee1,0)
        filterOff(payee1)
        checkOriginalState()
    }

    @Test
    fun payeeFilterShouldShowDuplicate() {
        checkOriginalState()
        filterOn(payee2)
        assertListSize(2)
        payeeIsDisplayed(payee2,0)
        payeeIsDisplayed(duplicate, 1)
        filterOff(payee2)
        checkOriginalState()
    }


    private fun checkOriginalState() {
        assertListSize(3)
        payeeIsDisplayed(payee1, 0)
        payeeIsDisplayed(payee2, 1)
        payeeIsDisplayed(duplicate, 2)
    }

    private fun filterOn(payee: String) {
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(withText(R.string.payer_or_payee)).perform(click())
        onView(withId(R.id.list))
            .perform(RecyclerViewActions.actionOnItem<PartiesList.ViewHolder>(
                hasDescendant(withText(payee)), clickOnViewChild(R.id.checkBox)))
        onView(withId(R.id.CREATE_COMMAND)).perform(click())
    }

    private fun filterOff(payee: String) {
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(withText(payee)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
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