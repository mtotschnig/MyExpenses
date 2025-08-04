package org.totschnig.myexpenses.test.provider

import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.provider.AccountInfo
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
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

class AttachmentTest : BaseDbTest() {

    private var transactionId: Long = 0
    private var attachmentId: Long = 0

    private val testUri: Uri
        get() = TRANSACTIONS_ATTACHMENTS_URI

    private val internalAttachmentUri =
        "content://org.totschnig.myexpenses.debug.fileprovider/external-files/Pictures/dummy.pdf"
    private val externalAttachmentUri = "content://some.other.app/external-files/Pictures/dummy.pdf"


    private fun insertFixture() {
        //Assume account types are populated
        val account = AccountInfo("Test account", cashAccount.id, 0, "USD")
        val accountId = mDb.insert(TABLE_ACCOUNTS, account.contentValues)
        val transaction = TransactionInfo(accountId, 0, comment = "Transaction 0")
        //We insert two transactions, in order to have transactionId and attachmentId with different values,
        //so that a bug where they were mixed up, would surface
        mDb.insert(TABLE_TRANSACTIONS, transaction.contentValues)
        transactionId = mDb.insert(TABLE_TRANSACTIONS, transaction.contentValues)
    }

    private fun expectStaleUris(expected: Int) {
        contentResolver.query(STALE_IMAGES_URI, null, null, null, null).useAndAssert {
            hasCount(expected)
        }
    }

    private fun expectLinkedAttachment(expected: String?) {
        contentResolver.query(
            testUri, null, "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null
        ).useAndAssert {
            if (expected == null) {
                hasCount(0)
            } else {
                hasCount(1)
                movesToFirst()
                hasString(0, expected)
            }
        }
    }

    private fun callDelete(uri: String, callDeleteMethod: Boolean) {
        if (callDeleteMethod) {
            assertThat(contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_DELETE_ATTACHMENTS, null, Bundle(2).apply {
                    putLong(KEY_TRANSACTIONID, transactionId)
                    putStringArray(DatabaseConstants.KEY_URI_LIST, arrayOf(uri))
                })!!.getBoolean(KEY_RESULT)
            ).isTrue()
        } else {
            assertThat(contentResolver.delete(
                TransactionProvider.TRANSACTION_ATTACHMENT_SINGLE_URI(
                    transactionId, attachmentId
                ),  null, null
            )).isEqualTo(1)
        }
    }

    private fun expectNoAttachments() {
        contentResolver.query(ATTACHMENTS_URI, null, null, null, null).useAndAssert {
            hasCount(0)
        }
    }

    fun testInsertQueryDeleteInternalCall() {
        doTheTest(withExternalUri = false, withCall = true)
    }

    fun testInsertQueryDeleteExternalCall() {
        doTheTest(withExternalUri = true, withCall = true)
    }

    fun testInsertQueryDeleteInternalDelete() {
        doTheTest(withExternalUri = false, withCall = false)
    }

    fun testInsertQueryDeleteExternalDelete() {
        doTheTest(withExternalUri = true, withCall = false)
    }

    private fun doTheTest(withExternalUri: Boolean, withCall: Boolean) {
        val uri = if(withExternalUri) externalAttachmentUri else internalAttachmentUri
        insertFixture()
        expectStaleUris(0)
        expectLinkedAttachment(null)
        attachmentId = ContentUris.parseId(contentResolver.insert(testUri, ContentValues(1).apply {
            put(KEY_TRANSACTIONID, transactionId)
            put(KEY_URI, uri)
        })!!)
        expectStaleUris(0)
        if (withExternalUri) {
            assertThat(persistedPermissions).containsExactly(Uri.parse(externalAttachmentUri))
        } else {
            assertThat(persistedPermissions).isEmpty()
        }
        expectLinkedAttachment(uri)
        callDelete(uri, withCall)
        expectLinkedAttachment(null)
        assertThat(persistedPermissions).isEmpty()
        //uri should now be deleted
        expectStaleUris(if (withExternalUri) 0 else 1)
        expectNoAttachments()
    }
}