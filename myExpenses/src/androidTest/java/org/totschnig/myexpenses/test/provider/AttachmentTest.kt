package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTACHMENTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTACHMENTS
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider.ATTACHMENTS_FOR_TRANSACTION_URI
import org.totschnig.myexpenses.provider.TransactionProvider.ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.STALE_IMAGES_URI
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.testutils.CursorSubject.Companion.assertThat
import java.util.Date

class AttachmentTest : BaseDbTest() {

    private var transactionId: Long = 0

    private val testUri: Uri
        get() = ATTACHMENTS_FOR_TRANSACTION_URI(transactionId)

    private val internalAttachmentUri = "content://org.totschnig.myexpenses.debug.fileprovider/external-files/Pictures/dummy.pdf"
    private val externalAttachmentUri = "content://some.other.app/external-files/Pictures/dummy.pdf"


    private fun insertFixture() {
        val account = AccountInfo("Test account", AccountType.CASH, 0, "USD")
        val accountId = mDb.insert(TABLE_ACCOUNTS, account.contentValues)
        val transaction = TransactionInfo("Transaction 0", Date(), 0, accountId)
        transactionId = mDb.insert(TABLE_TRANSACTIONS, transaction.contentValues)
    }

    private fun insertAttachments() {
        val attachmentId = mDb.insert(TABLE_ATTACHMENTS, ContentValues(2).apply {
            put(KEY_URI, internalAttachmentUri)
            put(KEY_UUID, "uuid")
        })
        mDb.insert(TABLE_TRANSACTION_ATTACHMENTS, ContentValues(2).apply {
            put(KEY_TRANSACTIONID, transactionId)
            put(KEY_ATTACHMENT_ID, attachmentId)
        })
    }

    private fun expectStaleUris(expected: Int) {
        contentResolver.query(STALE_IMAGES_URI, null, null, null, null)!!.use {
            assertThat(it).hasCount(expected)
        }
    }

    fun testInsertQueryDeleteInternal() {
        insertFixture()
        expectStaleUris(0)
        contentResolver.query(STALE_IMAGES_URI, null, null, null, null)!!.use {
            assertThat(it).hasCount(0)
        }
        contentResolver.query(
            testUri
            , null, null, null, null
        )!!.use {
            assertThat(it).hasCount(0)
        }
        contentResolver.insert(testUri, ContentValues(1).apply {
            put(KEY_URI, internalAttachmentUri)
        })
        expectStaleUris(0)
        assertThat(persistedPermissions).isEmpty()
        contentResolver.query(
            testUri
            , null, null, null, null
        )!!.use {
            with(assertThat(it)) {
                hasCount(1)
                movesToFirst()
                hasString(0, internalAttachmentUri)
            }
        }
        contentResolver.delete(testUri, "$KEY_URI = ?", arrayOf(internalAttachmentUri))
        contentResolver.query(testUri, null, null, null, null)!!.use {
            assertThat(it).hasCount(0)
        }
        //uri should not be deleted but reported as stale
        expectStaleUris(1)
        contentResolver.query(ATTACHMENTS_URI, null, null, null, null)!!.use {
            assertThat(it).hasCount(1)
        }
    }

    fun testInsertQueryDeleteExternal() {
        insertFixture()
        expectStaleUris(0)
        contentResolver.query(STALE_IMAGES_URI, null, null, null, null)!!.use {
            assertThat(it).hasCount(0)
        }
        contentResolver.query(
            testUri
            , null, null, null, null
        )!!.use {
            assertThat(it).hasCount(0)
        }
        contentResolver.insert(testUri, ContentValues(1).apply {
            put(KEY_URI, externalAttachmentUri)
        })
        expectStaleUris(0)
        assertThat(persistedPermissions).containsExactly(Uri.parse(externalAttachmentUri))
        contentResolver.query(
            testUri
            , null, null, null, null
        )!!.use {
            with(assertThat(it)) {
                hasCount(1)
                movesToFirst()
                hasString(0, externalAttachmentUri)
            }
        }
        contentResolver.delete(testUri, "$KEY_URI = ?", arrayOf(externalAttachmentUri))
        contentResolver.query(testUri, null, null, null, null)!!.use {
            assertThat(it).hasCount(0)
        }
        assertThat(persistedPermissions).isEmpty()
        //uri should now be deleted
        expectStaleUris(0)
        contentResolver.query(ATTACHMENTS_URI, null, null, null, null)!!.use {
            assertThat(it).hasCount(0)
        }
    }
}