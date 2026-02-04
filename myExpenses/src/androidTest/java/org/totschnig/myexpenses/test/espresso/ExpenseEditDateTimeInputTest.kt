package org.totschnig.myexpenses.test.espresso

import android.text.format.DateFormat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.dateButtonHasDate
import org.totschnig.shared_test.TransactionData
import java.time.LocalDate
import java.time.LocalTime

data class TestConfig(
    val accountType: AccountType,
    val withTimePreference: Boolean,
    val withValueDatePreference: Boolean,
    val expectedTimeVisible: Boolean,
    val expectedValueDateVisible: Boolean
) {
    override fun toString() =
        "${accountType.name}: ${if (withTimePreference) "with" else "without"} time, ${if (withValueDatePreference) "with" else "without"} value date"
}

@TestShard2
@RunWith(Parameterized::class)
class ExpenseEditDateTimeInputTest(
    private val config: TestConfig
): BaseExpenseEditTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data2(): List<TestConfig> = listOf(
            TestConfig(
                AccountType.CASH,
                withTimePreference = true,
                withValueDatePreference = true,
                expectedTimeVisible = true,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.CASH,
                withTimePreference = true,
                withValueDatePreference = false,
                expectedTimeVisible = true,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.CASH,
                withTimePreference = false,
                withValueDatePreference = true,
                expectedTimeVisible = false,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.CASH,
                withTimePreference = false,
                withValueDatePreference = false,
                expectedTimeVisible = false,
                expectedValueDateVisible = false
            ),

            TestConfig(
                AccountType.BANK,
                withTimePreference = true,
                withValueDatePreference = true,
                expectedTimeVisible = false,
                expectedValueDateVisible = true
            ),
            TestConfig(
                AccountType.BANK,
                withTimePreference = true,
                withValueDatePreference = false,
                expectedTimeVisible = true,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.BANK,
                withTimePreference = false,
                withValueDatePreference = true,
                expectedTimeVisible = false,
                expectedValueDateVisible = true
            ),
            TestConfig(
                AccountType.BANK,
                withTimePreference = false,
                withValueDatePreference = false,
                expectedTimeVisible = false,
                expectedValueDateVisible = false
            )
        )
    }

    private fun setUpAccount(accountType: AccountType) {
        account1 = buildAccount(ACCOUNT_LABEL_1, type = accountType)
    }

    private fun setPreferences(withTime: Boolean, withValueDate: Boolean) {
        prefHandler.putBoolean(PrefKey.TRANSACTION_WITH_TIME, withTime)
        prefHandler.putBoolean(PrefKey.TRANSACTION_WITH_VALUE_DATE, withValueDate)
    }

    @Test
    fun testDateTimePreferences() = runTest {
        setUpAccount(config.accountType)
        setPreferences(
            withTime = config.withTimePreference,
            withValueDate = config.withValueDatePreference
        )
        launch()
        setAmount(1)
        checkEffectiveVisible(R.id.DateButton)
        val today = LocalDate.now()
        val newDate = if (today.dayOfMonth == 1) today.plusDays(1) else today.minusDays(1)
        val newTime = LocalTime.of(13, 13)
        val newValueDate = today.withDayOfMonth(15)

        onView(withId(R.id.DateButton))
            .check(matches(dateButtonHasDate(today)))
        onView(withId(R.id.DateButton)).perform(click())
        setDate(newDate)
        onView(withId(R.id.DateButton))
            .check(matches(dateButtonHasDate(newDate)))

        if (config.expectedTimeVisible) {
            checkEffectiveVisible(R.id.TimeButton)
            onView(withId(R.id.TimeButton)).perform(click())
            setTime(newTime, DateFormat.is24HourFormat(targetContext))
        } else {
            checkEffectiveGone(R.id.TimeButton)
        }

        if (config.expectedValueDateVisible) {
            checkEffectiveVisible(R.id.Date2Button)
            onView(withId(R.id.Date2Button)).perform(click())

            setDate(newValueDate)
        } else {
            checkEffectiveGone(R.id.Date2Button)
        }
        clickFab() //save
        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = -100,
                date = newDate.atTime(if (config.expectedTimeVisible) newTime else LocalTime.NOON),
                valueDate = if (config.expectedValueDateVisible) newValueDate else null
            )
        )
    }
}