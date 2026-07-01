package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCurrencies
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.storeCustomFractionDigits
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseComposeTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup

@TestShard3
class ManageCurrenciesTest : BaseComposeTest<ManageCurrencies>() {

    @get:Rule
    var scenarioRule = ActivityScenarioRule(ManageCurrencies::class.java)

    lateinit var account: Account

    @Before
    fun setup() {
        testScenario = scenarioRule.scenario
    }

    @After
    fun clearDb() {
        cleanup {
            if (::account.isInitialized) {
                repository.deleteAccount(account.id)
            }
            runBlocking {
                repository.storeCustomFractionDigits(CURRENCY_CODE, null)
            }
        }
    }

    @Test
    fun changeOfFractionDigitsWithUpdateShouldKeepTransactionSum() {
        testHelper(true)
    }

    @Test
    fun changeOfFractionDigitsWithoutUpdateShouldChangeTransactionSum() {
        testHelper(false)
    }

    private fun getTotalAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance + repository.getTransactionSum(account)

    private fun testHelper(withUpdate: Boolean) {
        account = repository.createAccount(
            Account(
                label = "TEST ACCOUNT",
                openingBalance = 5000L,
                currency = CURRENCY_CODE,
                type = repository.findAccountType(PREDEFINED_NAME_CASH)!!
            )
        )
        repository.insertTransaction(
            accountId = account.id,
            amount = -1200L
        )
        val before = getTotalAccountBalance(account)
        assertThat(before).isEqualTo(3800)

        val expectedBefore = "${getString(R.string.number_of_fraction_digits)}: 2"
        val expectedAfter = "${getString(R.string.number_of_fraction_digits)}: 3"

        listNode.performScrollToNode(
            hasText(CURRENCY_CODE) and hasText(expectedBefore)
        )
        composeTestRule.onNode(hasText(CURRENCY_CODE) and hasText(expectedBefore))
            .performClick()

        composeTestRule.onNodeWithText(getString(R.string.menu_edit)).performClick()

        composeTestRule.onNodeWithText("2").performTextReplacement("3")

        if (withUpdate) {
            composeTestRule.onNodeWithText(getString(R.string.warning_change_fraction_digits_checkbox_label))
                .performClick()
        }

        composeTestRule.onNodeWithText(getString(android.R.string.ok)).performClick()

        listNode.performScrollToNode(
            hasText(CURRENCY_CODE) and hasText(expectedAfter)
        )

        composeTestRule.onNode(hasText(CURRENCY_CODE) and hasText(expectedAfter))
            .assertIsDisplayed()

        val after = getTotalAccountBalance(account)
        if (withUpdate) {
            assertThat(after).isEqualTo(before * 10)
        } else {
            assertThat(after).isEqualTo(before)
        }
    }

    companion object {
        private const val CURRENCY_CODE = "EUR"
    }
}