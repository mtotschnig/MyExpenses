package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.util.*

class TransactionDebtTest: BaseDbTest() {
    private var testAccountId: Long = 0
    private var payeeId1: Long = 0
    private var payeeId2: Long = 0
    private var debt1: Long = 0
    private var debt2: Long = 0
    private var closedDebt: Long = 0
    private var closedTransaction: Long = 0
    val currency = CurrencyUnit.DebugInstance

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0)
        testAccountId =
            mDb.insertOrThrow(TABLE_ACCOUNTS, null, testAccount.contentValues)
        payeeId1 = mDb.insertOrThrow(
            TABLE_PAYEES,
            null,
            PayeeInfo("A.A.").contentValues
        )
        payeeId2 = mDb.insertOrThrow(
            TABLE_PAYEES,
            null,
            PayeeInfo("B.B.").contentValues
        )
        debt1 = mDb.insertOrThrow(
            TABLE_DEBTS,
            null,
            Debt(0, "Debt 1", "", payeeId1, 100000, currency, System.currentTimeMillis() / 1000).toContentValues()
        )
        debt2 = mDb.insertOrThrow(
            TABLE_DEBTS,
            null,
            Debt(0, "Debt 2", "", payeeId2, 100000, currency, System.currentTimeMillis() / 1000).toContentValues()
        )
        closedDebt = mDb.insertOrThrow(
            TABLE_DEBTS,
            null,
            Debt(0, "Closed debt", "", payeeId1, 100000, currency, System.currentTimeMillis() / 1000).toContentValues()
        )
        closedTransaction = mDb.insertOrThrow(
            DatabaseConstants.TABLE_TRANSACTIONS,
            null,
            TransactionInfo("Transaction closed", Date(), 0, testAccountId, payeeId1, closedDebt).contentValues
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
        } catch (e: SQLiteConstraintException) {
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
        } catch (e: SQLiteConstraintException) {
            //Expected
        }
    }

    fun testInsertIntoSealedDebtShouldFail() {
        val testTransaction = TransactionInfo("Transaction 0", Date(), 0, testAccountId, payeeId1, closedDebt)

        try {
            mDb.insertOrThrow(
                DatabaseConstants.TABLE_TRANSACTIONS,
                null,
                testTransaction.contentValues
            )
            kotlin.test.fail("Insert into closed debt dit no raise SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            //Expected
        }
    }

}