package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.PayeeInfo
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.provider.update
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.viewmodel.data.Debt

class TransactionDebtTest: BaseDbTest() {
    private var testAccountId: Long = 0
    private var payeeId1: Long = 0
    private var payeeId2: Long = 0
    private var debt1: Long = 0
    private var debt2: Long = 0
    private var closedDebt: Long = 0
    private var closedTransaction: Long = 0
    val currency = CurrencyUnit.DebugInstance

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        testAccountId = setupTestAccount()

        payeeId1 = mDb.insert(
            TABLE_PAYEES,
            PayeeInfo("A.A.").contentValues
        )
        payeeId2 = mDb.insert(
            TABLE_PAYEES,
            PayeeInfo("B.B.").contentValues
        )
        debt1 = mDb.insert(
            TABLE_DEBTS,
            Debt(0, "Debt 1", "", payeeId1, 100000, currency, System.currentTimeMillis() / 1000).toContentValues()
        )
        debt2 = mDb.insert(
            TABLE_DEBTS,
            Debt(0, "Debt 2", "", payeeId2, 100000, currency, System.currentTimeMillis() / 1000).toContentValues()
        )
        closedDebt = mDb.insert(
            TABLE_DEBTS,
            Debt(0, "Closed debt", "", payeeId1, 100000, currency, System.currentTimeMillis() / 1000).toContentValues()
        )
        closedTransaction = mDb.insert(
            DatabaseConstants.TABLE_TRANSACTIONS,
            TransactionInfo(
                accountId = testAccountId,
                amount = 0,
                comment = "Transaction closed",
                payeeId = payeeId1,
                debtId = closedDebt
            ).contentValues
        )
        mDb.update(TABLE_DEBTS, ContentValues(1).apply { put(KEY_SEALED, 1) },
            "$KEY_ROWID = ?", arrayOf(closedDebt.toString()))
    }

    fun testUpdateTransactionForSealedDebtShouldFail() {
        try {
            mDb.update(
                DatabaseConstants.TABLE_TRANSACTIONS,
                ContentValues(1).apply { put(KEY_AMOUNT, 5000) },
                "$KEY_ROWID = ?", arrayOf(closedTransaction.toString())
            )
            kotlin.test.fail("Update of closed debt did not raise SQLiteConstraintException")
        } catch (_: SQLiteConstraintException) {
            //Expected
        }
    }

    fun testDeleteTransactionForSealedDebtShouldFail() {
        try {
            mDb.delete(
                DatabaseConstants.TABLE_TRANSACTIONS,
                "$KEY_ROWID = ?", arrayOf(closedTransaction.toString())
            )
            kotlin.test.fail("Delete of transaction for closed debt did not raise SQLiteConstraintException")
        } catch (_: SQLiteConstraintException) {
            //Expected
        }
    }

    fun testInsertIntoSealedDebtShouldFail() {
        val testTransaction = TransactionInfo(
            accountId = testAccountId,
            amount = 0,
            comment = "Transaction 0",
            payeeId = payeeId1,
            debtId = closedDebt
        )

        try {
            mDb.insert(
                DatabaseConstants.TABLE_TRANSACTIONS,
                testTransaction.contentValues
            )
            kotlin.test.fail("Insert into closed debt dit no raise SQLiteConstraintException")
        } catch (_: SQLiteConstraintException) {
            //Expected
        }
    }

}