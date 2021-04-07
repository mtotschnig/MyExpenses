package org.totschnig.myexpenses.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Category
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import java.util.*


@RunWith(AndroidJUnit4::class)
class DistributionTest {
    private lateinit var scenario: ActivityScenario<Distribution>
    val currency = CurrencyUnit(Currency.getInstance("EUR"))
    private lateinit var account: Account
    private var categoryId: Long = 0

    private fun baseFixture(additionalFixture: () -> Unit = {}) {
        account = Account("Test account 1", currency, 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        additionalFixture()
        scenario = ActivityScenario.launch(Intent(InstrumentationRegistry.getInstrumentation().context, Distribution::class.java).apply {
            putExtra(KEY_ACCOUNTID, account.id)
        })
    }

    private fun fixtureWithMappedTransaction() {
        baseFixture {
            categoryId = Category.write(0, "TestCategory", null)
            with(Transaction.getNewInstance(account.id)) {
                amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
                catId = categoryId
                save()
            }
        }
    }

    @Test
    fun testEmpty() {
        baseFixture()
        onView(withId(R.id.empty)).check(matches(isDisplayed()))
    }

    @Test
    fun testSelectCommand() {
        launchWithContextCommand(R.id.SELECT_COMMAND)
        onView(allOf(withText(containsString("TestCategory")), withText(containsString("12"))))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
    }

    @Test
    fun testColorCommand() {
        launchWithContextCommand(R.id.COLOR_COMMAND)
        scenario.onActivity {
            assertThat(it.supportFragmentManager.findFragmentByTag(ProtectedFragmentActivity.EDIT_COLOR_DIALOG)).isNotNull()
        }
    }

    private fun launchWithContextCommand(menuItemId: Int) {
        fixtureWithMappedTransaction()
        onView(withId(R.id.empty)).check(matches(not(isDisplayed())))
        onData(Matchers.`is`(Matchers.instanceOf(org.totschnig.myexpenses.viewmodel.data.Category::class.java)))
                .atPosition(0)
                .perform(ViewActions.longClick())
        onContextMenu(menuItemId).perform(ViewActions.click())
    }

    private fun onContextMenu(menuItemId: Int): DataInteraction =
            onData(org.totschnig.myexpenses.testutils.Matchers.menuIdMatcher(menuItemId))
                    .inRoot(isPlatformPopup())
}