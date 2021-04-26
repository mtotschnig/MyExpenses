package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.totschnig.myexpenses.model.CurrencyEnum
import timber.log.Timber

const val DATABASE_VERSION = 117
abstract class BaseTransactionDatabase(context: Context, databaseName: String): SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    fun upgradeTo117(db: SQLiteDatabase) {
        migrateCurrency(db, "VEB", CurrencyEnum.VES)
        migrateCurrency(db, "MRO", CurrencyEnum.MRU)
        migrateCurrency(db, "STD", CurrencyEnum.STN)
    }

    private fun migrateCurrency(db: SQLiteDatabase, oldCurrency: String, newCurrency: CurrencyEnum) {
        if (db.query("accounts", arrayOf("count(*)"), "currency = ?", arrayOf(oldCurrency), null, null, null).use {
            it.moveToFirst()
            it.getInt(0)
        } > 0) {
            Timber.w("Currency is in use")
        } else {
            db.delete("currency", "code = ?", arrayOf(oldCurrency))
        }
        //if new currency is already defined, error is logged
        if (db.insert("currency", null, ContentValues().apply {
            put("code", newCurrency.name)
        }) != -1L) {
            Timber.d("Currency %s inserted", newCurrency.name)
        }
    }
}