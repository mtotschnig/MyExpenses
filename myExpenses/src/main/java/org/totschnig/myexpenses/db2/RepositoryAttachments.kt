package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ATTACHMENTS_FOR_TRANSACTION_URI
import org.totschnig.myexpenses.provider.TransactionProvider.ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.io.IOException

fun Repository.addAttachments(transactionId: Long, attachments: List<Uri>) {
    val ops = ArrayList<ContentProviderOperation>()
    attachments.forEach {
        ops.add(
            ContentProviderOperation.newInsert(ATTACHMENTS_FOR_TRANSACTION_URI(transactionId))
                .withValue(KEY_URI, it.toString())
                .build()
        )
    }

    if (contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).size != ops.size) {
        throw IOException("Saving attachments failed")
    }
}

fun Repository.deleteAttachments(transactionId: Long, attachments: List<Uri>) {
    if (contentResolver.delete(
            ATTACHMENTS_FOR_TRANSACTION_URI(transactionId),
            "$KEY_URI ${WhereFilter.Operation.IN.getOp(attachments.size)}",
            attachments.map { it.toString() }.toTypedArray()
        ) != attachments.size
    ) {
        throw IOException("Deleting attachments failed")
    }
}

fun Repository.loadAttachments(transactionId: Long): ArrayList<Uri> =
    ArrayList<Uri>().apply {
        contentResolver.query(
            ATTACHMENTS_FOR_TRANSACTION_URI(transactionId),
            null, null, null, null
        )?.use { cursor ->
            cursor.asSequence.forEach {
                val uri = Uri.parse(it.getString(0))
                add(uri)
            }
        }
    }