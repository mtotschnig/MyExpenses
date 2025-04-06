package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.net.Uri
import android.os.Bundle
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI_LIST
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_DELETE_ATTACHMENTS
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.useAndMapToList
import java.io.IOException
import androidx.core.net.toUri

fun Repository.addAttachments(transactionId: Long, attachments: List<Uri>) {
    if (attachments.isEmpty()) return
    val ops = ArrayList<ContentProviderOperation>()
    attachments.forEach {
        ops.add(
            ContentProviderOperation.newInsert(TRANSACTIONS_ATTACHMENTS_URI)
                .withValue(KEY_TRANSACTIONID, transactionId.toString())
                .withValue(KEY_URI, it.toString())
                .build()
        )
    }

    if (contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).size != ops.size) {
        throw IOException("Saving attachments failed")
    }
}

fun Repository.deleteAttachments(transactionId: Long, attachments: List<Uri>) {
    if (!contentResolver.call(DUAL_URI, METHOD_DELETE_ATTACHMENTS, null, Bundle(2).apply {
            putLong(KEY_TRANSACTIONID, transactionId)
            putStringArray(KEY_URI_LIST, attachments.map(Uri::toString).toTypedArray())
        })!!.getBoolean(KEY_RESULT)) throw IOException("Deleting attachments failed")
}

//noinspection Recycle
fun Repository.loadAttachmentIds(transactionId: Long) = contentResolver.query(
    TRANSACTIONS_ATTACHMENTS_URI,
    arrayOf(KEY_ATTACHMENT_ID), "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null
)?.useAndMapToList { it.getLong(0) } ?: emptyList()

//noinspection Recycle
fun Repository.loadAttachments(transactionId: Long) = contentResolver.query(
    TRANSACTIONS_ATTACHMENTS_URI,
    arrayOf(KEY_URI), "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null
)?.useAndMapToList { it.getString(0).toUri() } ?: emptyList()