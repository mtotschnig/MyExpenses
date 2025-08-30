package org.totschnig.myexpenses.provider

import android.content.ContentUris
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.archive
import org.totschnig.myexpenses.db2.unarchive
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
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

    fun setup(accountType: String = PREDEFINED_NAME_CASH) {
        testAccountId = insertAccount("Test account", accountType = accountType)
    }

    @Test
    fun createArchiveAndUnpack() {
        setup()
        insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, -200)
        insertTransaction(testAccountId, 400)
        insertTransaction(testAccountId, 800)
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        verifyArchive(archiveId, 1100)
        verifyArchivedChildren(archiveId, 4)
        unarchiveAndVerify(archiveId, 4)
    }

    @Test
    fun createArchiveWithInconsistentStatesForCashAccount() {
        setup()
        insertTransaction(testAccountId, 100, crStatus = CrStatus.RECONCILED)
        insertTransaction(testAccountId, -200, crStatus = CrStatus.CLEARED)
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        verifyArchive(archiveId, -100)
        verifyArchivedChildren(archiveId, 2)
    }

    @Test(expected = IllegalStateException::class)
    fun createArchiveWithInconsistentStatesForBankAccount() {
        setup(PREDEFINED_NAME_BANK)
        insertTransaction(testAccountId, 100, crStatus = CrStatus.RECONCILED)
        insertTransaction(testAccountId, -200, crStatus = CrStatus.CLEARED)
        repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
    }

    @Test
    fun createArchiveWithVoidTransactions() {
        setup()
        insertTransaction(testAccountId, 100, crStatus = CrStatus.RECONCILED)
        insertTransaction(testAccountId, -200, crStatus = CrStatus.VOID)
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        verifyArchive(archiveId, 100)
        verifyArchivedChildren(archiveId, 2)
        unarchiveAndVerify(archiveId, 2)
    }

    @Test
    fun createArchiveWithSplitTransactions() {
        setup()
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId)
        insertTransaction(testAccountId, 50, parentId = splitId)
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        verifyArchive(archiveId, 100)
        verifyArchivedChildren(archiveId, 1, splitId)
        verifyArchivedChildren(splitId, 2)
        val splitIDArg = splitId.toString()
        unarchiveAndVerify(archiveId, 3, "$KEY_ROWID = ? OR $KEY_PARENTID = ?", arrayOf(splitIDArg, splitIDArg))
    }

    private fun verifyArchive(
        archiveId: Long,
        expectedAmount: Long,
        expectedStatus: CrStatus = CrStatus.UNRECONCILED
    ) {
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID, KEY_STATUS, KEY_CR_STATUS, KEY_AMOUNT),
            "$KEY_PARENTID is null", null, null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(0, archiveId)
            hasInt(1, STATUS_ARCHIVE)
            hasString(2, expectedStatus.name)
            hasLong(3, expectedAmount)
        }
    }

    private fun verifyArchivedChildren(
        parentId: Long,
        expectedCount: Int,
        expectedRowId: Long? = null,
    ) {
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS, KEY_ROWID),
            "$KEY_PARENTID = ?", arrayOf(parentId.toString()), null
        )!!.useAndAssert {
            hasCount(expectedCount)
            forEach {
                hasInt(0, STATUS_ARCHIVED)
                expectedRowId?.let {
                    hasLong(1, expectedRowId)
                }
            }
        }
    }

    private fun unarchiveAndVerify(
        archiveId: Long,
        expectedCount: Int,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ) {
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
            selection, selectionArgs, null
        )!!.useAndAssert {
            hasCount(expectedCount)
            forEach {
                hasInt(0, STATUS_NONE)
            }
        }

    }
}