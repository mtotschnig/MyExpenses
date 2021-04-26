package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.preference.PrefKey
import timber.log.Timber

const val DATABASE_VERSION = 117
abstract class BaseTransactionDatabase(context: Context, databaseName: String): SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    fun upgradeTo117(db: SQLiteDatabase) {
        migrateCurrency(db, "VEB", CurrencyEnum.VES)
        migrateCurrency(db, "MRO", CurrencyEnum.MRU)
        migrateCurrency(db, "STD", CurrencyEnum.STN)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        PrefKey.FIRST_INSTALL_DB_SCHEMA_VERSION.putInt(DATABASE_VERSION)
    }

    private fun migrateCurrency(db: SQLiteDatabase, oldCurrency: String, newCurrency: CurrencyEnum) {
        if (db.query("accounts", arrayOf("count(*)"), "currency = ?", arrayOf(oldCurrency), null, null, null).use {
            it.moveToFirst()
            it.getInt(0)
        } > 0) {
            Timber.w("Currency %s is in use", oldCurrency)
        } else if (db.delete("currency", "code = ?", arrayOf(oldCurrency)) == 1) {
            Timber.d("Currency %s deleted", oldCurrency)
        }
        //if new currency is already defined, error is logged
        if (db.insert("currency", null, ContentValues().apply {
                    put("code", newCurrency.name)
                }) != -1L) {
            Timber.d("Currency %s inserted", newCurrency.name)
        }
    }
}