package org.totschnig.myexpenses.db2

import android.content.ContentResolver
import android.content.ContentUris
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider

fun Repository.deleteTemplate(id: Long) {
    contentResolver.delete(ContentUris.withAppendedId(TransactionProvider.TEMPLATES_URI, id), null, null)
}

fun Repository.getPayeeForTemplate(id: Long) = contentResolver.findBySelection(
    "$KEY_ROWID = ?",
    arrayOf(id.toString()),
    KEY_PAYEEID
)

private fun ContentResolver.findBySelection(
    selection: String,
    selectionArgs: Array<String>,
    column: String
) =
    query(
        org.totschnig.myexpenses.model.Template.CONTENT_URI,
        arrayOf(column),
        selection,
        selectionArgs,
        null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    } ?: -1