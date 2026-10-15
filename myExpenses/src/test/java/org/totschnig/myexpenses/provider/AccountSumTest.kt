package org.totschnig.myexpenses.provider

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class AccountSumTest: BaseTestWithRepository() {
    val openingBalance: Long = 100L
    val expense1: Long = 11L
    val expense2: Long = 22L
    val income1: Long = 33L
    val income2: Long = 44L
    val transferP: Long = 55L
    val transferN: Long = 66L

    @Test
    fun testDatabaseCalculatedSums() {
        val testAccountId1 = insertAccount("Test account 1", openingBalance = openingBalance)
        val testAccountId2 = insertAccount("Test account 2", openingBalance = openingBalance)
        repository.insertTransaction(testAccountId1, -expense1)
        repository.insertTransaction(testAccountId1, -expense2)
        repository.insertTransaction(testAccountId1, income1)
        repository.insertTransaction(testAccountId1, income2)
        repository.insertTransfer(testAccountId1, testAccountId2, transferP)
        repository.insertTransfer(testAccountId2, testAccountId1, transferN)

        contentResolver.query(
            TransactionProvider.ACCOUNTS_FULL_URI,
            null,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(2)
        }

        contentResolver.query(
            TransactionProvider.ACCOUNTS_FULL_URI,
            null,
            "$KEY_ROWID= ?",
            arrayOf(testAccountId1.toString()),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_SUM_INCOME, income1 + income2)
            hasLong(KEY_SUM_EXPENSES, -expense1 - expense2)
            hasLong(KEY_SUM_TRANSFERS, transferP - transferN)
            hasLong(KEY_CURRENT_BALANCE, openingBalance + income1 + income2 - expense1 - expense2 + transferP - transferN)
        }

        contentResolver.query(
            TransactionProvider.ACCOUNTS_FULL_URI,
            null,  // get all the columns
            "$KEY_ROWID= ?",
            arrayOf(testAccountId2.toString()),
            null // use default the sort order
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_SUM_INCOME, 0L)
            hasLong(KEY_SUM_EXPENSES, 0L)
            hasLong(KEY_SUM_TRANSFERS, transferN - transferP)
            hasLong(KEY_CURRENT_BALANCE, openingBalance + transferN - transferP)
        }
    }
}