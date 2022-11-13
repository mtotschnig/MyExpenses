package org.totschnig.requery

import androidx.annotation.Keep
import io.requery.android.database.sqlite.SQLiteDatabase

@Keep
object DatabaseVersionPeekHelper : org.totschnig.myexpenses.provider.DatabaseVersionPeekHelper {
    override fun peekVersion(path: String) =
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use {
            it.version
        }
}