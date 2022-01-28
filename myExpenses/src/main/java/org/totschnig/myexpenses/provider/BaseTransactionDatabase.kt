package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import timber.log.Timber

const val DATABASE_VERSION = 121
const val RAISE_UPDATE_SEALED_DEBT = "SELECT RAISE (FAIL, 'attempt to update sealed debt');"

private const val DEBTS_SEALED_TRIGGER_CREATE = """
CREATE TRIGGER sealed_debt_update
BEFORE UPDATE OF $KEY_DATE,$KEY_LABEL,$KEY_AMOUNT,$KEY_CURRENCY,$KEY_DESCRIPTION ON $TABLE_DEBTS WHEN old.$KEY_SEALED = 1
BEGIN $RAISE_UPDATE_SEALED_DEBT END
"""

private const val TRANSACTIONS_SEALED_DEBT_INSERT_TRIGGER_CREATE = """
CREATE TRIGGER sealed_debt_transaction_insert
BEFORE INSERT ON $TABLE_TRANSACTIONS WHEN (SELECT $KEY_SEALED FROM $TABLE_DEBTS WHERE $KEY_ROWID = new.$KEY_DEBT_ID) = 1
BEGIN $RAISE_UPDATE_SEALED_DEBT END
"""

private const val TRANSACTIONS_SEALED_DEBT_UPDATE_TRIGGER_CREATE = """
CREATE TRIGGER sealed_debt_transaction_update
BEFORE UPDATE ON $TABLE_TRANSACTIONS WHEN (SELECT max($KEY_SEALED) FROM $TABLE_DEBTS WHERE $KEY_ROWID IN (new.$KEY_DEBT_ID,old.$KEY_DEBT_ID)) = 1
BEGIN $RAISE_UPDATE_SEALED_DEBT END
"""

private const val TRANSACTIONS_SEALED_DEBT_DELETE_TRIGGER_CREATE = """
CREATE TRIGGER sealed_debt_transaction_delete
BEFORE DELETE ON $TABLE_TRANSACTIONS WHEN (SELECT $KEY_SEALED FROM $TABLE_DEBTS WHERE $KEY_ROWID = old.$KEY_DEBT_ID) = 1
BEGIN $RAISE_UPDATE_SEALED_DEBT END
"""

abstract class BaseTransactionDatabase(context: Context, databaseName: String) :
    SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    fun upgradeTo117(db: SQLiteDatabase) {
        migrateCurrency(db, "VEB", CurrencyEnum.VES)
        migrateCurrency(db, "MRO", CurrencyEnum.MRU)
        migrateCurrency(db, "STD", CurrencyEnum.STN)
    }

    fun upgradeTo118(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE planinstance_transaction RENAME to planinstance_transaction_old")
        //make sure we have only one instance per template
        db.execSQL(
            "CREATE TABLE planinstance_transaction " +
                    "(template_id integer references templates(_id) ON DELETE CASCADE, " +
                    "instance_id integer, " +
                    "transaction_id integer unique references transactions(_id) ON DELETE CASCADE," +
                    "primary key (template_id, instance_id));"
        )
        db.execSQL(
            ("INSERT OR IGNORE INTO planinstance_transaction " +
                    "(template_id,instance_id,transaction_id)" +
                    "SELECT " +
                    "template_id,instance_id,transaction_id FROM planinstance_transaction_old")
        )
        db.execSQL("DROP TABLE planinstance_transaction_old")
    }

    fun upgradeTo119(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions add column debt_id integer references debts (_id) ON DELETE SET NULL")
        db.execSQL(
            "CREATE TABLE debts (_id integer primary key autoincrement, payee_id integer references payee(_id) ON DELETE CASCADE, date datetime not null, label text not null, amount integer, currency text not null, description text, sealed boolean default 0);"
        )
        createOrRefreshTransactionDebtTriggers(db)
    }

    fun upgradeTo120(db: SQLiteDatabase) {
        with(db) {
            execSQL("DROP TRIGGER IF EXISTS transaction_debt_insert")
            execSQL("DROP TRIGGER IF EXISTS transaction_debt_update")
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        PrefKey.FIRST_INSTALL_DB_SCHEMA_VERSION.putInt(DATABASE_VERSION)
    }

    private fun migrateCurrency(
        db: SQLiteDatabase,
        oldCurrency: String,
        newCurrency: CurrencyEnum
    ) {
        if (db.query(
                "accounts",
                arrayOf("count(*)"),
                "currency = ?",
                arrayOf(oldCurrency),
                null,
                null,
                null
            ).use {
                it.moveToFirst()
                it.getInt(0)
            } > 0
        ) {
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

    fun createOrRefreshTransactionDebtTriggers(db: SQLiteDatabase) {
        with(db) {
            execSQL("DROP TRIGGER IF EXISTS sealed_debt_update")
            execSQL("DROP TRIGGER IF EXISTS sealed_debt_transaction_insert")
            execSQL("DROP TRIGGER IF EXISTS sealed_debt_transaction_update")
            execSQL("DROP TRIGGER IF EXISTS sealed_debt_transaction_delete")
            execSQL(DEBTS_SEALED_TRIGGER_CREATE)
            execSQL(TRANSACTIONS_SEALED_DEBT_INSERT_TRIGGER_CREATE)
            execSQL(TRANSACTIONS_SEALED_DEBT_UPDATE_TRIGGER_CREATE)
            execSQL(TRANSACTIONS_SEALED_DEBT_DELETE_TRIGGER_CREATE)
        }
    }
}