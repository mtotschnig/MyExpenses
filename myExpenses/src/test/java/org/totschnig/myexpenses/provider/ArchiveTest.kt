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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
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
        insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, -200)
        insertTransaction(testAccountId, 400)
        insertTransaction(testAccountId, 800)
    }

    @Test
    fun createArchiveAndUnpack() {
        val archiveId = repository.archive(testAccountId, LocalDate.now() to LocalDate.now())
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            "$KEY_PARENTID is null", null, null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(0, archiveId)
        }
        val archiveUri =
            ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, archiveId)
        contentResolver.query(
            archiveUri,
            arrayOf(KEY_STATUS, KEY_AMOUNT),
            null, null, null
        )!!.useAndAssert {
            movesToFirst()
            hasInt(0, STATUS_ARCHIVE)
            hasLong(1, 1100)
        }
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_STATUS, KEY_AMOUNT),
            "$KEY_PARENTID = ?", arrayOf(archiveId.toString()), null
        )!!.useAndAssert {
            hasCount(4)
            forEach {
                hasInt(0, STATUS_ARCHIVED)
            }
        }
        repository.unarchive(archiveId)
        contentResolver.query(
            archiveUri,
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
}