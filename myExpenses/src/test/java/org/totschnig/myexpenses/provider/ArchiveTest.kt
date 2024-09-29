package org.totschnig.myexpenses.provider

import android.content.ContentUris
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.archive
import org.totschnig.myexpenses.db2.unarchive
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVED
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ArchiveTest : BaseTestWithRepository() {

    private var testAccountId: Long = 0

    @Before
    fun setup() {
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0, "USD")
        testAccountId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                testAccount.contentValues
            )!!
        )
    }

    @Test
    fun createArchiveAndUnpack() {
        insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, -200)
        insertTransaction(testAccountId, 400)
        insertTransaction(testAccountId, 800)
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID, KEY_STATUS, KEY_CR_STATUS, KEY_AMOUNT),
            "$KEY_PARENTID is null", null, null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(0, archiveId)
            hasInt(1, STATUS_ARCHIVE)
            hasString(2, CrStatus.UNRECONCILED.name)
            hasLong(3, 1100)
        }
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS),
            "$KEY_PARENTID = ?", arrayOf(archiveId.toString()), null
        )!!.useAndAssert {
            hasCount(4)
            forEach {
                hasInt(0, STATUS_ARCHIVED)
            }
        }
        repository.unarchive(archiveId)
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, archiveId),
            arrayOf(KEY_STATUS),
            null, null, null
        )!!.useAndAssert {
           hasCount(0)
        }
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS),
            null, null
        )!!.useAndAssert {
            hasCount(4)
            forEach {
                hasInt(0, STATUS_NONE)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun createArchiveWithInconsistentStates() {
        insertTransaction(testAccountId, 100, crStatus = CrStatus.RECONCILED)
        insertTransaction(testAccountId, -200, crStatus = CrStatus.CLEARED)
        repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
    }

    @Test
    fun createArchiveWithVoidTransactions() {
        insertTransaction(testAccountId, 100, crStatus = CrStatus.RECONCILED)
        insertTransaction(testAccountId, -200, crStatus = CrStatus.VOID)
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID, KEY_STATUS, KEY_CR_STATUS, KEY_AMOUNT),
            "$KEY_PARENTID is null", null, null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(0, archiveId)
            hasInt(1, STATUS_ARCHIVE)
            hasString(2, CrStatus.RECONCILED.name)
            hasLong(3, 100)
        }
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS, KEY_AMOUNT),
            "$KEY_PARENTID = ?", arrayOf(archiveId.toString()), null
        )!!.useAndAssert {
            hasCount(2)
            forEach {
                hasInt(0, STATUS_ARCHIVED)
            }
        }
        repository.unarchive(archiveId)
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, archiveId),
            arrayOf(KEY_STATUS),
            null, null, null
        )!!.useAndAssert {
            hasCount(0)
        }
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS),
            null, null
        )!!.useAndAssert {
            hasCount(2)
            forEach {
                hasInt(0, STATUS_NONE)
            }
        }
    }

    @Test
    fun createArchiveWithSplitTransactions() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId)
        insertTransaction(testAccountId, 50, parentId = splitId)
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID, KEY_STATUS, KEY_CR_STATUS, KEY_AMOUNT),
            "$KEY_PARENTID is null", null, null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(0, archiveId)
            hasInt(1, STATUS_ARCHIVE)
            hasString(2, CrStatus.UNRECONCILED.name)
            hasLong(3, 100)
        }
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS, KEY_ROWID),
            "$KEY_PARENTID = ?", arrayOf(archiveId.toString()), null
        )!!.useAndAssert {
            hasCount(1)
            movesToFirst()
            hasInt(0, STATUS_ARCHIVED)
            hasLong(1, splitId)
        }
        val splitIDArg = splitId.toString()
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS, KEY_AMOUNT),
            "$KEY_PARENTID = ?", arrayOf(splitIDArg), null
        )!!.useAndAssert {
            hasCount(2)
            forEach {
                hasInt(0, STATUS_ARCHIVED)
            }
        }
        repository.unarchive(archiveId)
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, archiveId),
            arrayOf(KEY_STATUS),
            null, null, null
        )!!.useAndAssert {
            hasCount(0)
        }
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS, KEY_AMOUNT),
            "$KEY_ROWID = ? OR $KEY_PARENTID = ?", arrayOf(splitIDArg, splitIDArg), null
        )!!.useAndAssert {
            hasCount(3)
            forEach {
                hasInt(0, STATUS_NONE)
            }
        }
    }
}