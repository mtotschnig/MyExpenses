package org.totschnig.myexpenses.test.provider

import android.content.ContentUris
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.testutils.CursorSubject
import java.util.Date

class AutoFillTest : BaseDbTest() {
    private val testTransactions = arrayOfNulls<TransactionInfo>(3)
    private var testAccountId: Long = 0
    private var payeeId: Long = 0
    private var payeeId1: Long = 0
    private var duplicateId: Long = 0
    private var payeeId2 : Long = 0
    private var catId: Long = 0
    private var catId1: Long = 0
    private var methodChequeId: Long = 0
    private var methodCreditCardId: Long = 0

    private fun insertData() {
        val now = Date()
        testTransactions[0] = TransactionInfo(
            "Transaction 0",
            now,
            0,
            testAccountId,
            payeeId,
            null,
            catId1,
            methodCreditCardId
        )
        testTransactions[1] = TransactionInfo(
            "Transaction 1",
            now,
            200,
            testAccountId,
            payeeId,
            null,
            catId,
            methodChequeId
        )
        testTransactions[2] = TransactionInfo(
            "Transaction 2",
            now,
            -100,
            testAccountId,
            payeeId1,
            null,
            catId,
            methodCreditCardId
        )

        for (testTransaction in testTransactions) {
            mDb.insert(DatabaseConstants.TABLE_TRANSACTIONS, testTransaction!!.contentValues)
        }
    }

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0, "USD")
        testAccountId = mDb.insert(TABLE_ACCOUNTS, testAccount.contentValues)
        payeeId = mDb.insert(TABLE_PAYEES, PayeeInfo("N.N").contentValues)
        catId =
            mDb.insert(TABLE_CATEGORIES, CategoryInfo("Main").contentValues)
        methodChequeId = repository.findPaymentMethod(PreDefinedPaymentMethod.CHEQUE.name)!!
        methodCreditCardId = repository.findPaymentMethod(PreDefinedPaymentMethod.CREDITCARD.name)!!
        payeeId1 = mDb.insert(TABLE_PAYEES, PayeeInfo("y.y").contentValues)
        duplicateId = mDb.insert(TABLE_PAYEES, PayeeInfo("yy", payeeId1).contentValues)
        payeeId2 = mDb.insert(TABLE_PAYEES, PayeeInfo("OhneGeld").contentValues)
        catId1 = mDb.insert(
            TABLE_CATEGORIES,
            CategoryInfo("Main 1").contentValues
        )
        insertData()
    }

    fun testAutoLoadData() {
        testAutoLoadForPayee(payeeId, testTransactions[1]!!)
    }

    fun testAutoLoadDataForDuplicate() {
        testAutoLoadForPayee(duplicateId, testTransactions[2]!!)
    }

    fun testAutoLoadDataForPayeeWithoutTransaction() {
        testAutoLoadForPayee(payeeId2, null)
    }

    private fun testAutoLoadForPayee(payeeId: Long, transaction: TransactionInfo?) {
        val projection = arrayOf(
            DatabaseConstants.KEY_CURRENCY,
            DatabaseConstants.KEY_AMOUNT,
            DatabaseConstants.KEY_CATID,
            DatabaseConstants.CAT_AS_LABEL,
            DatabaseConstants.KEY_COMMENT,
            DatabaseConstants.KEY_ACCOUNTID,
            DatabaseConstants.KEY_METHODID
        )
        mockContentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, payeeId),
            projection,
            null,
            null,
            null
        )!!.use {
            with(CursorSubject.assertThat(it)) {
                if (transaction != null) {
                    reportsTransaction(transaction)
                } else {
                    hasCount(0)
                }
            }
        }
    }

    private fun CursorSubject.reportsTransaction(transaction: TransactionInfo) {
        hasCount(1)
        movesToFirst()
        hasLong(1, transaction.amount)
        hasLong(2, transaction.catId!!)
        hasString(3, "Main")
        hasString(4, transaction.comment)
        hasLong(5, transaction.accountId)
        hasLong(6, transaction.methodId!!)
    }
}
