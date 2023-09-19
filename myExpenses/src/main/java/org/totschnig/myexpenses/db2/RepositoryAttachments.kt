package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.util.PictureDirHelper
import timber.log.Timber
import java.io.IOException

fun Repository.saveAttachments(transactionId: Long, attachments: List<Uri>) {
    val ops = ArrayList<ContentProviderOperation>()
    ops.add(
        ContentProviderOperation.newDelete(ATTACHMENTS_URI)
            .withSelection("$KEY_TRANSACTIONID= ?", arrayOf(transactionId.toString()))
            .build())
    attachments.forEach {
        ops.add(
            ContentProviderOperation.newInsert(ATTACHMENTS_URI)
                .withValue(KEY_URI, it.toString())
                .withValue(KEY_TRANSACTIONID, transactionId)
                .build())
    }

    if (contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).size != ops.size) {
        throw IOException("Saving tags failed")
    }
}

fun Repository.loadAttachments(transactionId: Long): ArrayList<Uri> =
    ArrayList<Uri>().apply {
        contentResolver.query(ATTACHMENTS_URI, null, "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null)?.use { cursor ->
            cursor.asSequence.forEach {
                val uri = Uri.parse(it.getString(KEY_URI))
                add(uri)
            }
        }
    }

fun Repository.registerAsStale(uri: Uri) {
    contentResolver.insert(TransactionProvider.STALE_IMAGES_URI, ContentValues(1).apply {
        put(KEY_PICTURE_URI, uri.toString())
    })
}

fun Repository.onAttachmentDelete(uri: Uri) {
    if (uri.toString().startsWith(PictureDirHelper.getPictureUriBase(false,
            context.applicationContext as MyApplication
        ))) {
        Timber.d("found internally stored uri ($uri), need to set stale")
        registerAsStale(uri)
    } else {
        Timber.d("found externally linked uri ($uri), need to release permission")
        contentResolver.releasePersistableUriPermission(uri,  Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}