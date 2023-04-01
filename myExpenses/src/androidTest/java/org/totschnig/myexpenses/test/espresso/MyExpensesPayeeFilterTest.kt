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
import org.totschnig.myexpenses.fragment.PartiesList
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

class MyExpensesPayeeFilterTest: BaseMyExpensesTest() {
    private lateinit var account: org.totschnig.myexpenses.model2.Account
    private var payee1 = "John Doe"
    private var payee2 = "Hinz Finz"

    @Before
    fun fixture() {
        val currency = CurrencyUnit.DebugInstance
        account =  buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, -1200L)
        op.payee = payee1
        op.save()
        op.payee = payee2
        op.date = op.date - 10000
        op.saveAsNew()
        launch(account.id)
    }

    @Test
    fun payeeFilterShouldHideTransaction() {
        assertListSize(2)
        payeeIsDisplayed(payee1, 0)
        payeeIsDisplayed(payee2, 1)
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(withText(R.string.payer_or_payee)).perform(click())
        onView(withId(R.id.list))
            .perform(RecyclerViewActions.actionOnItem<PartiesList.ViewHolder>(
                hasDescendant(withText(payee1)), clickOnViewChild(R.id.checkBox)))
        onView(withId(R.id.CREATE_COMMAND)).perform(click())
        assertListSize(1)
        payeeIsDisplayed(payee1,0)
        //switch off filter
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(withText(payee1)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        assertListSize(2)
        payeeIsDisplayed(payee2, 1)
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