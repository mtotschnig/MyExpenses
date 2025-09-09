package org.totschnig.myexpenses.test.espresso

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.TestShard2
import kotlin.test.Test

data class TestConfig(
    val accountType: AccountType,
    val withTimePreference: Boolean,
    val withValuDatePreference: Boolean,
    val expectedTimeVisible: Boolean,
    val expectedValueDateVisible: Boolean
) {
    override fun toString() =
        "${accountType.name}: ${if (withTimePreference) "with" else "without"} time, ${if (withValuDatePreference) "with" else "without"} value date"
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
                withValuDatePreference = true,
                expectedTimeVisible = true,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.CASH,
                withTimePreference = true,
                withValuDatePreference = false,
                expectedTimeVisible = true,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.CASH,
                withTimePreference = false,
                withValuDatePreference = true,
                expectedTimeVisible = false,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.CASH,
                withTimePreference = false,
                withValuDatePreference = false,
                expectedTimeVisible = false,
                expectedValueDateVisible = false
            ),

            TestConfig(
                AccountType.BANK,
                withTimePreference = true,
                withValuDatePreference = true,
                expectedTimeVisible = false,
                expectedValueDateVisible = true
            ),
            TestConfig(
                AccountType.BANK,
                withTimePreference = true,
                withValuDatePreference = false,
                expectedTimeVisible = true,
                expectedValueDateVisible = false
            ),
            TestConfig(
                AccountType.BANK,
                withTimePreference = false,
                withValuDatePreference = true,
                expectedTimeVisible = false,
                expectedValueDateVisible = true
            ),
            TestConfig(
                AccountType.BANK,
                withTimePreference = false,
                withValuDatePreference = false,
                expectedTimeVisible = false,
                expectedValueDateVisible = false
            )
        )
    }

    private fun setUpAccount(accountType: AccountType) {
        account1 = buildAccount("Test label 1", type = accountType)
    }

    private fun setPreferences(withTime: Boolean, withValueDate: Boolean) {
        prefHandler.putBoolean(PrefKey.TRANSACTION_WITH_TIME, withTime)
        prefHandler.putBoolean(PrefKey.TRANSACTION_WITH_VALUE_DATE, withValueDate)
    }

    @Test
    fun testDateTimePreferences() {
        setUpAccount(config.accountType)
        setPreferences(
            withTime = config.withTimePreference,
            withValueDate = config.withValuDatePreference
        )
        launch()
        checkEffectiveVisible(R.id.DateButton)
        if (config.expectedTimeVisible) {
            checkEffectiveVisible(R.id.TimeButton)
        } else {
            checkEffectiveGone(R.id.TimeButton)
        }
        if (config.expectedValueDateVisible) {
            checkEffectiveVisible(R.id.Date2Button)
        } else {
            checkEffectiveGone(R.id.Date2Button)
        }
    }
}