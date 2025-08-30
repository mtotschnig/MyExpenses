package org.totschnig.myexpenses.provider

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.compose.FutureCriterion
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class AccountFutureCriterionTest: BaseTestWithRepository() {
    private var testAccountId: Long = 0

    fun runQueryTest(futureCriterion: FutureCriterion) {
        testAccountId = insertAccount("Test account")
        val prefKey = stringPreferencesKey(PrefKey.CRITERION_FUTURE.getKey()!!)
        runBlocking {
            dataStore.edit {
                it[prefKey] = futureCriterion.name
            }
        }
        val incomeNow = 200L
        val expenseLater = -100L
        val currentBalance = incomeNow + when(futureCriterion) {
            FutureCriterion.Current -> 0
            FutureCriterion.EndOfDay -> expenseLater
        }

        insertTransaction(testAccountId, incomeNow)
        insertTransaction(testAccountId, expenseLater, date = LocalDateTime.now().plusSeconds(2))

        contentResolver.query(
            TransactionProvider.ACCOUNTS_FULL_URI,
            null,
            "$KEY_ROWID = ?",
            arrayOf(testAccountId.toString()),
            null
        )!!.useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_SUM_EXPENSES, expenseLater)
            hasLong(KEY_SUM_INCOME, incomeNow)
            hasLong(KEY_CURRENT_BALANCE, currentBalance)
        }
    }

    @Test
    fun testQueryWithSumEndOfDay() {
        runQueryTest(FutureCriterion.EndOfDay)
    }

    @Test
    fun testQueryWithSumCurrent() {
        runQueryTest(FutureCriterion.Current)
    }
}