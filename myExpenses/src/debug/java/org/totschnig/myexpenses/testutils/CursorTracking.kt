package org.totschnig.myexpenses.testutils

import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteCursorDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteQuery
import timber.log.Timber
import java.util.*

//https://stackoverflow.com/a/6581501/1199911
class TrackingCursor(
    driver: SQLiteCursorDriver,
    editTable: String?, query: SQLiteQuery
) :
    SQLiteCursor(driver, editTable, query) {
    override fun close() {
        openCursors.remove(this)
        Timber.d("closing cursor %s, now open %d", this, openCursors.size)
    }

    companion object {
        private val openCursors: MutableList<Cursor> = LinkedList<Cursor>()
    }

    init {
        openCursors.add(this)
        Timber.d("adding cursor %s, now open %d", this, openCursors.size)
    }
}

class TrackingCursorFactory : CursorFactory {
    override fun newCursor(
        db: SQLiteDatabase, masterQuery: SQLiteCursorDriver,
        editTable: String?, query: SQLiteQuery
    ) = TrackingCursor(masterQuery, editTable, query)
}