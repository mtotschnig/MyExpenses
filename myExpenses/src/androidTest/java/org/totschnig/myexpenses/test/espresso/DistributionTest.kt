package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.DistributionActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.testutils.BaseUiTest
import java.util.*

class DistributionTest : BaseUiTest<DistributionActivity>() {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var account: Account
    private var categoryId: Long = 0

    private fun baseFixture(additionalFixture: () -> Unit = {}) {
        account = buildAccount("Test account 1")
        additionalFixture()
        testScenario = ActivityScenario.launch(Intent(targetContext, DistributionActivity::class.java).apply {
            putExtra(KEY_ACCOUNTID, account.id)
        })
    }

    private fun fixtureWithMappedTransaction() {
        baseFixture {
            categoryId = writeCategory("TestCategory")
            with(Transaction.getNewInstance(account.id, homeCurrency)) {
                amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
                catId = categoryId
                save()
            }
        }
    }

    @Test
    fun testSelectCommand() {
        launchWithContextCommand(R.string.menu_show_transactions)
        onView(allOf(withText(containsString("TestCategory")), withText(containsString("12"))))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
    }

    @Test
    fun testColorCommand() {
        launchWithContextCommand(R.string.color)
        testScenario.onActivity {
            assertThat(it.supportFragmentManager.findFragmentByTag(ProtectedFragmentActivity.EDIT_COLOR_DIALOG)).isNotNull()
        }
    }

    private fun launchWithContextCommand(@StringRes menuLabel: Int) {
        fixtureWithMappedTransaction()
        composeTestRule
            .onNodeWithText("TestCategory").performTouchInput {
                longClick()
            }
        onContextMenu(menuLabel)
    }

    private fun onContextMenu(@StringRes menuItemId: Int) =
        composeTestRule.onNodeWithText(getString(menuItemId)).performClick()

}