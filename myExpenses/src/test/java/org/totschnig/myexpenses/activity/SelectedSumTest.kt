package org.totschnig.myexpenses.activity

import android.content.Intent
import android.database.Cursor
import android.view.View
import android.widget.AdapterView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import java.util.*

@RunWith(AndroidJUnit4::class)
class SelectedSumTest {
    private lateinit var scenario: ActivityScenario<Distribution>

    @Before
    fun fixture() {
        val account = Account("Test account 1", CurrencyUnit(Currency.getInstance("EUR")), 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        val op0 = Transaction.getNewInstance(account.id)
        op0.amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
        op0.save()
        val times = 5
        for (i in 0 until times) {
            op0.saveAsNew()
        }
        scenario = ActivityScenario.launch(
                Intent(InstrumentationRegistry.getInstrumentation().context, MyExpenses::class.java).apply {
                    putExtra(DatabaseConstants.KEY_ROWID, account.id)
                })
    }

    @Test
    fun testSelectedSum() {
        openCab()
        var sum = 12
        testTitle(sum)
        for (i in 2 until 6) {
            select(i)
            sum+=12
            testTitle(sum)
        }
    }

    private fun testTitle(sum: Int) {
        Espresso.onView(withId(R.id.action_bar_title))
            .check(matches(withText(containsString(String.format("%.2f", sum.toFloat())))))
    }

    private val wrappedList: Matcher<View>
        get() = Matchers.allOf(
                isAssignableFrom(AdapterView::class.java),
                isDescendantOfA(withId(R.id.list)),
                isDisplayed()
        )

    private fun openCab() {
        onData(CoreMatchers.`is`(instanceOf(Cursor::class.java)))
                .inAdapterView(wrappedList)
                .atPosition(1)
                .perform(longClick())
    }
    private fun select(position: Int) {
        onData(CoreMatchers.`is`(instanceOf(Cursor::class.java)))
            .inAdapterView(wrappedList)
            .atPosition(position)
            .perform(click())
    }
}