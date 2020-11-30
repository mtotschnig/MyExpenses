package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

fun safeUpdateWithSealedAccounts(db: SQLiteDatabase, runnable: Runnable) {
    db.beginTransaction()
    try {
        ContentValues(1).apply {
            put(DatabaseConstants.KEY_SEALED, -1)
            db.update(DatabaseConstants.TABLE_ACCOUNTS, this, DatabaseConstants.KEY_SEALED + "= ?", arrayOf("1"))
        }
        runnable.run()
        ContentValues(1).apply {
            put(DatabaseConstants.KEY_SEALED, 1)
            db.update(DatabaseConstants.TABLE_ACCOUNTS, this, DatabaseConstants.KEY_SEALED + "= ?", arrayOf("-1"))
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}