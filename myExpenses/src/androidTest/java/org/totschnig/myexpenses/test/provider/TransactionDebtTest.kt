package org.totschnig.myexpenses.test.provider

import android.database.sqlite.SQLiteConstraintException
import com.google.common.truth.Truth
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.lang.Exception
import java.util.*

class TransactionDebtTest: BaseDbTest() {
    private var testAccountId: Long = 0
    private var payeeId1: Long = 0
    private var payeeId2: Long = 0
    private var debt1: Long = 0
    private var debt2: Long = 0


    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0)
        testAccountId =
            mDb.insertOrThrow(DatabaseConstants.TABLE_ACCOUNTS, null, testAccount.contentValues)
        payeeId1 = mDb.insertOrThrow(
            DatabaseConstants.TABLE_PAYEES,
            null,
            PayeeInfo("A.A.").contentValues
        )
        payeeId2 = mDb.insertOrThrow(
            DatabaseConstants.TABLE_PAYEES,
            null,
            PayeeInfo("B.B.").contentValues
        )
        debt1 = mDb.insertOrThrow(
            DatabaseConstants.TABLE_DEBTS,
            null,
            Debt(0, "Debt 1", "", payeeId1, 100000, "EUR", System.currentTimeMillis() / 1000).toContentValues()
        )
        debt2 = mDb.insertOrThrow(
            DatabaseConstants.TABLE_DEBTS,
            null,
            Debt(0, "Debt 1", "", payeeId2, 100000, "EUR", System.currentTimeMillis() / 1000).toContentValues()
        )
    }

    fun testInsertInconsistentDebtShouldFail() {
        val testTransaction = TransactionInfo("Transaction 0", Date(), 0, testAccountId, payeeId1, debt2)

        try {
            mDb.insertOrThrow(
                DatabaseConstants.TABLE_TRANSACTIONS,
                null,
                testTransaction.contentValues
            )
            kotlin.test.fail("Inconsistent debt insert did not raise SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            //Expected
        }
    }
}