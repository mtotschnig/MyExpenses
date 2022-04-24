package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils.suggestNewCategoryColor
import timber.log.Timber

const val DATABASE_VERSION = 126

private const val RAISE_UPDATE_SEALED_DEBT = "SELECT RAISE (FAIL, 'attempt to update sealed debt');"
private const val RAISE_INCONSISTENT_CATEGORY_HIERARCHY =
    "SELECT RAISE (FAIL, 'attempt to create inconsistent category hierarchy');"

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

const val ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE = """
CREATE TRIGGER account_remap_transfer_transaction_update
AFTER UPDATE on $TABLE_TRANSACTIONS WHEN new.$KEY_ACCOUNTID != old.$KEY_ACCOUNTID
BEGIN
    UPDATE $TABLE_TRANSACTIONS SET $KEY_TRANSFER_ACCOUNT = new.$KEY_ACCOUNTID WHERE _id = new.$KEY_TRANSFER_PEER;
END
"""

private val CATEGORY_HIERARCHY_TRIGGER = """
CREATE TRIGGER category_hierarchy_update
BEFORE UPDATE ON $TABLE_CATEGORIES WHEN new.$KEY_PARENTID IS NOT old.$KEY_PARENTID AND new.$KEY_PARENTID IN (${
    categoryTreeSelect(
        projection = arrayOf(KEY_ROWID),
        rootExpression = "= new.$KEY_ROWID"
    )
})
BEGIN $RAISE_INCONSISTENT_CATEGORY_HIERARCHY END
"""

const val CATEGORY_LABEL_INDEX_CREATE =
    "CREATE UNIQUE INDEX categories_label ON $TABLE_CATEGORIES($KEY_LABEL,coalesce($KEY_PARENTID, 0))"

const val CATEGORY_LABEL_LEGACY_TRIGGER_INSERT = """
CREATE TRIGGER category_label_unique_insert
    BEFORE INSERT
    ON $TABLE_CATEGORIES
    WHEN new.$KEY_PARENTID IS NULL AND exists (SELECT 1 from $TABLE_CATEGORIES WHERE $KEY_LABEL = new.$KEY_LABEL AND $KEY_PARENTID IS NULL)
    BEGIN
    SELECT RAISE (FAIL, 'main category exists');
END
"""

const val CATEGORY_LABEL_LEGACY_TRIGGER_UPDATE = """
CREATE TRIGGER category_label_unique_update
    BEFORE UPDATE
    ON $TABLE_CATEGORIES
    WHEN new.$KEY_PARENTID IS NULL ANd new.$KEY_LABEL != old.$KEY_LABEL AND exists (SELECT 1 from $TABLE_CATEGORIES WHERE $KEY_LABEL = new.$KEY_LABEL AND $KEY_PARENTID IS NULL)
    BEGIN
    SELECT RAISE (FAIL, 'main category exists');
END
"""

abstract class BaseTransactionDatabase(
    context: Context,
    databaseName: String,
    cursorFactory: SQLiteDatabase.CursorFactory?
) :
    SQLiteOpenHelper(context, databaseName, cursorFactory, DATABASE_VERSION) {

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

    fun upgradeTo122(db: SQLiteDatabase) {
        //repair transactions corrupted due to bug https://github.com/mtotschnig/MyExpenses/issues/921
        repairWithSealedAccountsAndDebts(db) {
            db.execSQL(
                "update transactions set transfer_account = (select account_id from transactions peer where _id = transactions.transfer_peer);"
            )
        }
        db.execSQL("DROP TRIGGER IF EXISTS account_remap_transfer_transaction_update")
        db.execSQL(ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE)
    }

    fun upgradeTo124(db: SQLiteDatabase) {
        repairWithSealedAccounts(db) {
            db.query("accounts", arrayOf("_id"), "uuid is null", null, null, null, null)
                .use { cursor ->
                    cursor.asSequence.forEach {
                        db.execSQL(
                            "update accounts set uuid = ? where _id =?",
                            arrayOf(Model.generateUuid(), it.getLong(0))
                        )
                    }
                }
        }
    }

    fun upgradeTo125(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE categories RENAME to categories_old")
        db.execSQL(
            "CREATE TABLE categories (_id integer primary key autoincrement, label text not null, label_normalized text, parent_id integer references categories(_id) ON DELETE CASCADE, usages integer default 0, last_used datetime, color integer, icon string, UNIQUE (label,parent_id));"
        )
        db.execSQL("INSERT INTO categories (_id, label, label_normalized, parent_id, usages, last_used, color, icon) SELECT _id, label, label_normalized, parent_id, usages, last_used, color, icon FROM categories_old")
        db.execSQL("DROP TABLE categories_old")
        createOrRefreshCategoryMainCategoryUniqueLabel(db)
        createOrRefreshCategoryHierarchyTrigger(db)
    }

    fun upgradeTo126(db: SQLiteDatabase) {
        //trigger caused a hanging query, because it did not check if parent_id was updated
        createOrRefreshCategoryHierarchyTrigger(db)
        //subcategories should not have a color
        db.update(
            "categories",
            ContentValues(1).apply {
                putNull("color")
            },
            "parent_id IS NOT NULL", null
        )
        //main categories need a color
        db.query(
            "categories",
            arrayOf("_id"),
            "parent_id is null AND color is null",
            null,
            null,
            null,
            null
        )?.use {
            it.asSequence.forEach {
                db.update(
                    "categories",
                    ContentValues(1).apply {
                        put(KEY_COLOR, suggestNewCategoryColor(db))
                    },
                    "_id = ?",
                    arrayOf(it.getLong(0).toString())
                )
            }
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

    fun repairWithSealedAccounts(db: SQLiteDatabase, run: Runnable) {
        db.execSQL("update accounts set sealed = -1 where sealed = 1")
        run.run()
        db.execSQL("update accounts set sealed = 1 where sealed = -1")
    }

    fun repairWithSealedAccountsAndDebts(db: SQLiteDatabase, run: Runnable) {
        db.execSQL("update accounts set sealed = -1 where sealed = 1")
        db.execSQL("update debts set sealed = -1 where sealed = 1")
        run.run()
        db.execSQL("update accounts set sealed = 1 where sealed = -1")
        db.execSQL("update debts set sealed = 1 where sealed = -1")
    }

    fun createOrRefreshCategoryHierarchyTrigger(db: SQLiteDatabase) {
        with(db) {
            execSQL("DROP TRIGGER IF EXISTS category_hierarchy_update")
            execSQL(CATEGORY_HIERARCHY_TRIGGER)
        }
    }

    fun createOrRefreshCategoryMainCategoryUniqueLabel(db: SQLiteDatabase) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && "robolectric" != Build.FINGERPRINT) {
            db.execSQL("DROP INDEX if exists categories_label")
            db.execSQL(CATEGORY_LABEL_INDEX_CREATE)
        } else {
            with(db) {
                execSQL("DROP TRIGGER IF EXISTS category_label_unique_insert")
                execSQL("DROP TRIGGER IF EXISTS category_label_unique_update")
                execSQL(CATEGORY_LABEL_LEGACY_TRIGGER_INSERT)
                execSQL(CATEGORY_LABEL_LEGACY_TRIGGER_UPDATE)
            }
        }
    }

}