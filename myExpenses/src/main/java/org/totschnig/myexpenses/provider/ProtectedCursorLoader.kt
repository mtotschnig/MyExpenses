package org.totschnig.myexpenses.provider

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.loader.content.CursorLoader
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

class ProtectedCursorLoader(
    context: Context,
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?
) : CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder) {
    override fun loadInBackground(): Cursor? {
        return try {
            super.loadInBackground()
        } catch (e: Exception) {
            CrashHandler.report(e)
            null
        }
    }
}