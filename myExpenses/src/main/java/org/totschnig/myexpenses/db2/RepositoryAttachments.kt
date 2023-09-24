package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.net.Uri
import android.os.Bundle
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI_LIST
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_DELETE_ATTACHMENTS
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import java.io.IOException

fun Repository.addAttachments(transactionId: Long, attachments: List<Uri>) {
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
    })!!.getBoolean(KEY_RESULT))  throw IOException("Deleting attachments failed")
}

fun Repository.loadAttachments(transactionId: Long): ArrayList<Uri> =
    ArrayList<Uri>().apply {
        contentResolver.query(
            TRANSACTIONS_ATTACHMENTS_URI,
            null, "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null
        )?.use { cursor ->
            cursor.asSequence.forEach {
                val uri = Uri.parse(it.getString(0))
                add(uri)
            }
        }
    }