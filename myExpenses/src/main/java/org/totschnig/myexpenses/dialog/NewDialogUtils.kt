package org.totschnig.myexpenses.dialog

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Spinner
import eltos.simpledialogfragment.color.SimpleColorDialog
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.enumValueOrDefault

fun Spinner.configureDateFormat(
    context: Context,
    prefHandler: PrefHandler,
    prefName: String
) {
    adapter = ArrayAdapter(
        context, android.R.layout.simple_spinner_item, QifDateFormat.entries.toTypedArray()
    ).apply {
        setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }
    setSelection(prefHandler.enumValueOrDefault(prefName, QifDateFormat.default).ordinal)
}

fun buildColorDialog(color: Int): SimpleColorDialog = SimpleColorDialog.build()
    .allowCustom(true)
    .cancelable(false)
    .neut()
    .colorPreset(color)

fun ContentResolver.getDisplayName(
    uri: Uri
): String {

    if (!"file".equals(uri.scheme, ignoreCase = true)) {
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        try {
            query(uri, null, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    // Note it's called "Display Name".  This is
                    // provider-specific, and might not necessarily be the file name.
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        val displayName = it.getString(columnIndex)
                        if (displayName != null) {
                            return displayName
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            //this can happen if the user has restored a backup and
            //we do not have a persistable permission
            null
        }
    }
    return uri.lastPathSegment ?: "UNKNOWN"
}
