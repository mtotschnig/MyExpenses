package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.STALE_IMAGES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.testutils.CursorSubject.Companion.assertThat
import java.util.Date

class AttachmentTest : BaseDbTest() {

    private var transactionId: Long = 0

    private val testUri: Uri
        get() = TRANSACTIONS_ATTACHMENTS_URI

    private val internalAttachmentUri =
        "content://org.totschnig.myexpenses.debug.fileprovider/external-files/Pictures/dummy.pdf"
    private val externalAttachmentUri = "content://some.other.app/external-files/Pictures/dummy.pdf"


    private fun insertFixture() {
        val account = AccountInfo("Test account", AccountType.CASH, 0, "USD")
        val accountId = mDb.insert(TABLE_ACCOUNTS, account.contentValues)
        val transaction = TransactionInfo("Transaction 0", Date(), 0, accountId)
        transactionId = mDb.insert(TABLE_TRANSACTIONS, transaction.contentValues)
    }

    private fun expectStaleUris(expected: Int) {
        contentResolver.query(STALE_IMAGES_URI, null, null, null, null)!!.use {
            assertThat(it).hasCount(expected)
        }
    }

    private fun expectLinkedAttachment(expected: String?) {
        contentResolver.query(
            testUri, null, "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null
        )!!.use {
            with(assertThat(it)) {
                if (expected == null) {
                    hasCount(0)
                } else {
                    hasCount(1)
                    movesToFirst()
                    hasString(0, expected)
                }
            }
        }
    }

    private fun callDelete(uri: String) {
        assertThat(contentResolver.call(
            TransactionProvider.DUAL_URI,
            TransactionProvider.METHOD_DELETE_ATTACHMENTS, null, Bundle(2).apply {
                putLong(KEY_TRANSACTIONID, transactionId)
                putStringArray(DatabaseConstants.KEY_URI_LIST, arrayOf(uri))
            })!!.getBoolean(KEY_RESULT)
        ).isTrue()
    }

    private fun expectNoAttachments() {
        contentResolver.query(ATTACHMENTS_URI, null, null, null, null)!!.use {
            assertThat(it).hasCount(0)
        }
    }

    fun testInsertQueryDeleteInternal() {
        insertFixture()
        expectStaleUris(0)
        expectLinkedAttachment(null)
        contentResolver.insert(testUri, ContentValues(1).apply {
            put(KEY_TRANSACTIONID, transactionId)
            put(KEY_URI, internalAttachmentUri)
        })
        expectStaleUris(0)
        assertThat(persistedPermissions).isEmpty()
        expectLinkedAttachment(internalAttachmentUri)
        callDelete(internalAttachmentUri)
        expectLinkedAttachment(null)
        //uri should not be deleted but reported as stale
        expectStaleUris(1)
        expectNoAttachments()
    }

    fun testInsertQueryDeleteExternal() {
        insertFixture()
        expectStaleUris(0)
        expectLinkedAttachment(null)
        contentResolver.insert(testUri, ContentValues(1).apply {
            put(KEY_TRANSACTIONID, transactionId)
            put(KEY_URI, externalAttachmentUri)
        })
        expectStaleUris(0)
        assertThat(persistedPermissions).containsExactly(Uri.parse(externalAttachmentUri))
        expectLinkedAttachment(externalAttachmentUri)
        callDelete(externalAttachmentUri)
        expectLinkedAttachment(null)
        assertThat(persistedPermissions).isEmpty()
        //uri should now be deleted
        expectStaleUris(0)
        expectNoAttachments()
    }
}