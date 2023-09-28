package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.totschnig.myexpenses.db2.Attribute
import org.totschnig.myexpenses.db2.BankingAttribute
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CONTEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IBAN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTACHMENTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BANKS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTACHMENTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_ALL
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_CHANGES_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_UNCOMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_WITH_ACCOUNT
import timber.log.Timber
import java.util.Locale

const val DATABASE_VERSION = 149

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
BEFORE UPDATE ON $TABLE_CATEGORIES WHEN new.$KEY_PARENTID IS NOT old.$KEY_PARENTID AND new.$KEY_PARENTID IN ($categoryTreeSelectForTrigger)
BEGIN $RAISE_INCONSISTENT_CATEGORY_HIERARCHY END
"""

const val PARTY_HIERARCHY_TRIGGER = """
CREATE TRIGGER party_hierarchy_update
AFTER UPDATE OF $KEY_PARENTID ON $TABLE_PAYEES WHEN new.$KEY_PARENTID IS NOT NULL
BEGIN
UPDATE $TABLE_PAYEES SET $KEY_PARENTID = new.$KEY_PARENTID WHERE $KEY_PARENTID = new.$KEY_ROWID;
END
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

const val BANK_CREATE = """
CREATE TABLE $TABLE_BANKS ($KEY_ROWID integer primary key autoincrement, $KEY_BLZ text not null, $KEY_BIC text not null, $KEY_BANK_NAME text not null, $KEY_USER_ID text not null, unique($KEY_BLZ, $KEY_USER_ID))
"""

const val PAYEE_CREATE = """
CREATE TABLE $TABLE_PAYEES (
    $KEY_ROWID integer primary key autoincrement,
    $KEY_PAYEE_NAME text not null,
    $KEY_SHORT_NAME text,
    $KEY_IBAN text,
    $KEY_BIC text,
    $KEY_PAYEE_NAME_NORMALIZED text,
    $KEY_PARENTID integer references $TABLE_PAYEES($KEY_ROWID) ON DELETE CASCADE,
    unique($KEY_PAYEE_NAME, $KEY_IBAN));
"""

//the unique index on ($KEY_PAYEE_NAME, $KEY_IBAN) does not prevent duplicate names when iban is null
const val PAYEE_UNIQUE_INDEX = """
CREATE UNIQUE INDEX payee_name ON $TABLE_PAYEES($KEY_PAYEE_NAME) WHERE $KEY_IBAN IS NULL;
"""

const val ATTRIBUTES_CREATE = """
CREATE TABLE $TABLE_ATTRIBUTES (
    $KEY_ROWID integer primary key autoincrement,
    $KEY_ATTRIBUTE_NAME text not null,
    $KEY_CONTEXT text not null,
    unique ($KEY_ATTRIBUTE_NAME, $KEY_CONTEXT)
);
"""

const val TRANSACTION_ATTRIBUTES_CREATE = """
CREATE TABLE $TABLE_TRANSACTION_ATTRIBUTES (
    $KEY_TRANSACTIONID integer references $TABLE_TRANSACTIONS($KEY_ROWID) ON DELETE CASCADE,
    $KEY_ATTRIBUTE_ID integer references $TABLE_ATTRIBUTES($KEY_ROWID) ON DELETE CASCADE,
    $KEY_VALUE text not null,
    primary key ($KEY_TRANSACTIONID, $KEY_ATTRIBUTE_ID)
);
"""

const val ACCOUNT_ATTRIBUTES_CREATE = """
CREATE TABLE $TABLE_ACCOUNT_ATTRIBUTES (
    $KEY_ACCOUNTID integer references $TABLE_ACCOUNTS($KEY_ROWID) ON DELETE CASCADE,
    $KEY_ATTRIBUTE_ID integer references $TABLE_ATTRIBUTES($KEY_ROWID) ON DELETE CASCADE,
    $KEY_VALUE text not null,
    primary key ($KEY_ACCOUNTID, $KEY_ATTRIBUTE_ID)
);
"""

const val ATTACHMENTS_CREATE = """
CREATE TABLE $TABLE_ATTACHMENTS (
    $KEY_ROWID integer primary key autoincrement,
    $KEY_URI text not null unique,
    $KEY_UUID text not null unique
);
"""

const val TRANSACTIONS_ATTACHMENTS_CREATE = """
CREATE TABLE $TABLE_TRANSACTION_ATTACHMENTS (
    $KEY_TRANSACTIONID integer references $TABLE_TRANSACTIONS($KEY_ROWID) ON DELETE CASCADE,
    $KEY_ATTACHMENT_ID integer references $TABLE_ATTACHMENTS($KEY_ROWID),
    primary key ($KEY_TRANSACTIONID, $KEY_ATTACHMENT_ID)
);
"""

private const val INCREASE_CATEGORY_USAGE_ACTION =
    " BEGIN UPDATE $TABLE_CATEGORIES SET $KEY_USAGES = $KEY_USAGES + 1, $KEY_LAST_USED = strftime('%s', 'now')  WHERE $KEY_ROWID IN (new.$KEY_CATID , (SELECT $KEY_PARENTID FROM $TABLE_CATEGORIES WHERE $KEY_ROWID = new.$KEY_CATID)); END;"

private val INCREASE_CATEGORY_USAGE_INSERT_TRIGGER =
    "CREATE TRIGGER insert_increase_category_usage AFTER INSERT ON $TABLE_TRANSACTIONS WHEN new.$KEY_CATID IS NOT NULL AND new.$KEY_CATID != ${DatabaseConstants.SPLIT_CATID}$INCREASE_CATEGORY_USAGE_ACTION"

private const val INCREASE_CATEGORY_USAGE_UPDATE_TRIGGER =
    "CREATE TRIGGER update_increase_category_usage AFTER UPDATE ON $TABLE_TRANSACTIONS WHEN new.$KEY_CATID IS NOT NULL AND (old.$KEY_CATID IS NULL OR new.$KEY_CATID != old.$KEY_CATID)$INCREASE_CATEGORY_USAGE_ACTION"

private const val INCREASE_ACCOUNT_USAGE_ACTION =
    " BEGIN UPDATE $TABLE_ACCOUNTS SET $KEY_USAGES = $KEY_USAGES + 1, $KEY_LAST_USED = strftime('%s', 'now')  WHERE $KEY_ROWID = new.$KEY_ACCOUNTID; END;"

private const val INCREASE_ACCOUNT_USAGE_INSERT_TRIGGER =
    "CREATE TRIGGER insert_increase_account_usage AFTER INSERT ON $TABLE_TRANSACTIONS WHEN new.$KEY_PARENTID IS NULL$INCREASE_ACCOUNT_USAGE_ACTION"

private const val INCREASE_ACCOUNT_USAGE_UPDATE_TRIGGER =
    "CREATE TRIGGER update_increase_account_usage AFTER UPDATE ON $TABLE_TRANSACTIONS WHEN new.$KEY_PARENTID IS NULL AND new.$KEY_ACCOUNTID != old.$KEY_ACCOUNTID AND (old.$KEY_TRANSFER_ACCOUNT IS NULL OR new.$KEY_ACCOUNTID != old.$KEY_TRANSFER_ACCOUNT)$INCREASE_ACCOUNT_USAGE_ACTION"

const val TRANSACTIONS_UUID_INDEX_CREATE =
    "CREATE UNIQUE INDEX transactions_account_uuid_index ON $TABLE_TRANSACTIONS($KEY_ACCOUNTID,$KEY_UUID,$KEY_STATUS)"

const val TRANSACTIONS_CAT_ID_INDEX =
    "CREATE INDEX transactions_cat_id_index on $TABLE_TRANSACTIONS($KEY_CATID)"

const val TRANSACTIONS_PAYEE_ID_INDEX =
    "CREATE INDEX transactions_payee_id_index on $TABLE_TRANSACTIONS($KEY_PAYEEID)"

private const val RAISE_UPDATE_SEALED_ACCOUNT =
    "SELECT RAISE (FAIL, 'attempt to update sealed account');"

const val ACCOUNTS_SEALED_TRIGGER_CREATE =
    """CREATE TRIGGER sealed_account_update
 BEFORE UPDATE OF $KEY_LABEL,$KEY_OPENING_BALANCE,$KEY_DESCRIPTION,$KEY_CURRENCY,$KEY_TYPE,$KEY_UUID,$KEY_CRITERION ON $TABLE_ACCOUNTS
 WHEN old.$KEY_SEALED = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""

const val TRANSACTIONS_SEALED_INSERT_TRIGGER_CREATE =
    """CREATE TRIGGER sealed_account_transaction_insert
 BEFORE INSERT ON $TABLE_TRANSACTIONS
 WHEN (SELECT $KEY_SEALED FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = new.$KEY_ACCOUNTID) = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""

//we allow update of status
const val TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE =
    """CREATE TRIGGER sealed_account_transaction_update
 BEFORE UPDATE OF $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, $KEY_AMOUNT, $KEY_CATID, $KEY_ACCOUNTID, $KEY_PAYEEID, $KEY_TRANSFER_PEER, $KEY_TRANSFER_ACCOUNT, $KEY_METHODID, $KEY_PARENTID, $KEY_REFERENCE_NUMBER, $KEY_UUID, $KEY_ORIGINAL_AMOUNT, $KEY_ORIGINAL_CURRENCY, $KEY_EQUIVALENT_AMOUNT, $KEY_DEBT_ID, $KEY_CR_STATUS
 ON $TABLE_TRANSACTIONS
 WHEN (SELECT max($KEY_SEALED) FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID IN (new.$KEY_ACCOUNTID,old.$KEY_ACCOUNTID)) = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""

//we allow update of cr_status and status
const val TRANSFER_SEALED_UPDATE_TRIGGER_CREATE =
    """CREATE TRIGGER sealed_account_tranfer_update
 BEFORE UPDATE OF $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, $KEY_AMOUNT, $KEY_CATID, $KEY_ACCOUNTID, $KEY_PAYEEID, $KEY_TRANSFER_PEER, $KEY_TRANSFER_ACCOUNT, $KEY_METHODID, $KEY_PARENTID, $KEY_REFERENCE_NUMBER, $KEY_UUID, $KEY_ORIGINAL_AMOUNT, $KEY_ORIGINAL_CURRENCY, $KEY_EQUIVALENT_AMOUNT, $KEY_DEBT_ID
 ON $TABLE_TRANSACTIONS
 WHEN (SELECT $KEY_SEALED FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = old.$KEY_TRANSFER_ACCOUNT) = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""


const val TRANSACTIONS_SEALED_DELETE_TRIGGER_CREATE =
    """CREATE TRIGGER sealed_account_transaction_delete
 BEFORE DELETE ON $TABLE_TRANSACTIONS
 WHEN (SELECT $KEY_SEALED FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = old.$KEY_ACCOUNTID) = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""

abstract class BaseTransactionDatabase(val prefHandler: PrefHandler) :
    SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    fun createOrRefreshTransactionUsageTriggers(db: SupportSQLiteDatabase) {
        db.execSQL(INCREASE_CATEGORY_USAGE_INSERT_TRIGGER)
        db.execSQL(INCREASE_CATEGORY_USAGE_UPDATE_TRIGGER)
        db.execSQL(INCREASE_ACCOUNT_USAGE_INSERT_TRIGGER)
        db.execSQL(INCREASE_ACCOUNT_USAGE_UPDATE_TRIGGER)
    }

    fun upgradeTo117(db: SupportSQLiteDatabase) {
        migrateCurrency(db, "VEB", CurrencyEnum.VES)
        migrateCurrency(db, "MRO", CurrencyEnum.MRU)
        migrateCurrency(db, "STD", CurrencyEnum.STN)
    }

    fun upgradeTo118(db: SupportSQLiteDatabase) {
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

    fun upgradeTo119(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions add column debt_id integer references debts (_id) ON DELETE SET NULL")
        db.execSQL(
            "CREATE TABLE debts (_id integer primary key autoincrement, payee_id integer references payee(_id) ON DELETE CASCADE, date datetime not null, label text not null, amount integer, currency text not null, description text, sealed boolean default 0);"
        )
        createOrRefreshTransactionDebtTriggers(db)
    }

    fun upgradeTo120(db: SupportSQLiteDatabase) {
        with(db) {
            execSQL("DROP TRIGGER IF EXISTS transaction_debt_insert")
            execSQL("DROP TRIGGER IF EXISTS transaction_debt_update")
        }
    }

    fun upgradeTo122(db: SupportSQLiteDatabase) {
        //repair transactions corrupted due to bug https://github.com/mtotschnig/MyExpenses/issues/921
        repairWithSealedAccountsAndDebts(db) {
            db.execSQL(
                "update transactions set transfer_account = (select account_id from transactions peer where _id = transactions.transfer_peer);"
            )
        }
        db.execSQL("DROP TRIGGER IF EXISTS account_remap_transfer_transaction_update")
        db.execSQL(ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE)
    }

    fun upgradeTo124(db: SupportSQLiteDatabase) {
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

    fun upgradeTo125(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories RENAME to categories_old")
        db.execSQL(
            "CREATE TABLE categories (_id integer primary key autoincrement, label text not null, label_normalized text, parent_id integer references categories(_id) ON DELETE CASCADE, usages integer default 0, last_used datetime, color integer, icon string, UNIQUE (label,parent_id));"
        )
        db.execSQL("INSERT INTO categories (_id, label, label_normalized, parent_id, usages, last_used, color, icon) SELECT _id, label, label_normalized, parent_id, usages, last_used, color, icon FROM categories_old")
        db.execSQL("DROP TABLE categories_old")
        createOrRefreshCategoryMainCategoryUniqueLabel(db)
        createOrRefreshCategoryHierarchyTrigger(db)
    }

    fun upgradeTo126(db: SupportSQLiteDatabase) {
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
        ).use {
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

    fun upgradeTo128(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE categories SET icon = replace(icon,'_','-')")
        upgradeIcons(
            db, mapOf(
                "apple-alt" to "apple-whole",
                "balance-scale" to "scale-balanced",
                "birthday-cake" to "cake-candles",
                "blind" to "person-walking-with-cane",
                "burn" to "fire-flame-simple",
                "car-crash" to "car-burst",
                "cocktail" to "martini-glass-citrus",
                "concierge-bell" to "bell-concierge",
                "cut" to "scissors",
                "donate" to "circle-dollar-to-slot",
                "dot-circle" to "circle-dot",
                "funnel-dollar" to "filter-circle-dollar",
                "glass-whiskey" to "whiskey-glass",
                "hand-holding-usd" to "hand-holding-dollar",
                "hands-helping" to "handshake-angle",
                "heart-broken" to "heart-crack",
                "home" to "house",
                "house-damage" to "house-chimney-crack",
                "medkit" to "suitcase-medical",
                "parking" to "square-parking",
                "portrait" to "image-portrait",
                "prescription-bottle-alt" to "prescription-bottle-medical",
                "running" to "person-running",
                "search-dollar" to "magnifying-glass-dollar",
                "search-plus" to "magnifying-glass-plus",
                "shield-alt" to "shield-halved",
                "shopping-basket" to "basket-shopping",
                "shopping-cart" to "cart-shopping",
                "sign-in-alt" to "right-to-bracket",
                "sign-out-alt" to "right-from-bracket",
                "subway" to "train-subway",
                "table-tennis" to "table-tennis-paddle-ball",
                "tools" to "screwdriver-wrench",
                "tram" to "cable-car",
                "tshirt" to "shirt",
                "university" to "building-columns",
                "user-cog" to "user-gear",
                "user-md" to "user-doctor",
                "walking" to "person-walking",
                "premium" to "award",
                "retirement" to "person-cane",
                "ic-check" to "check",
                "ic-expand-more" to "angle-down"
            )
        )
    }

    fun upgradeTo129(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE budgets_neu ( _id integer primary key autoincrement, title text not null default '', description text not null, grouping text not null check (grouping in ('NONE','DAY','WEEK','MONTH','YEAR')), account_id integer references accounts(_id) ON DELETE CASCADE, currency text, start datetime, `end` datetime)")
        db.execSQL("CREATE TABLE budget_allocations ( budget_id integer not null references budgets(_id) ON DELETE CASCADE, cat_id integer not null references categories(_id) ON DELETE CASCADE, year integer, second integer, budget integer, rollOverPrevious integer, rollOverNext integer, oneTime boolean default 0, primary key (budget_id,cat_id,year,second))")
        db.execSQL("INSERT INTO budgets_neu (_id, title, description, grouping, account_id, currency, start, `end`) SELECT _id, title, coalesce(description,''), grouping, account_id, currency, start, `end` FROM budgets")
        db.execSQL("INSERT INTO budget_allocations (budget_id, cat_id, budget) SELECT _id, 0, budget FROM budgets")
        db.execSQL("INSERT INTO budget_allocations (budget_id, cat_id, budget) SELECT budget_id, cat_id, budget FROM budget_categories")
        db.execSQL("DROP TABLE budgets")
        db.execSQL("DROP TABLE budget_categories")
        db.execSQL("ALTER TABLE budgets_neu RENAME to budgets")
        db.execSQL("CREATE INDEX budget_allocations_cat_id_index on budget_allocations(cat_id)")
    }

    fun upgradeTo130(db: SupportSQLiteDatabase) {
        upgradeIcons(db, mapOf("car-crash" to "car-burst"))
    }

    fun upgradeTo131(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budgets add column is_default boolean default 0")
    }

    fun upgradeTo133(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE currency add column sort_direction text not null check (sort_direction  in ('ASC','DESC')) default 'DESC'"
        )
    }

    fun upgradeTo136(db: SupportSQLiteDatabase) {
        db.query(
            "categories",
            arrayOf("_id"),
            "uuid is null",
            null,
            null,
            null,
            null
        ).use {
            it.asSequence.forEach {
                db.update(
                    "categories",
                    ContentValues(1).apply {
                        put(KEY_UUID, Model.generateUuid())
                    },
                    "_id = ?",
                    arrayOf(it.getLong(0).toString())
                )
            }
        }
    }

    fun SupportSQLiteDatabase.upgradeTo145() {
        execSQL("CREATE TABLE banks (_id integer primary key autoincrement, blz text not null, bic text not null, name text not null, user_id text not null, unique(blz, user_id))")
        execSQL("ALTER TABLE accounts add column bank_id integer references banks(_id) ON DELETE SET NULL")
        execSQL("ALTER TABLE payee RENAME to payee_old")
        execSQL(
            "CREATE TABLE payee (_id integer primary key autoincrement, name text not null, iban text, bic text, name_normalized text, unique(name, iban))"
        )
        execSQL("INSERT INTO payee (_id, name, name_normalized) SELECT _id, name, name_normalized FROM payee_old")
        execSQL("DROP TABLE payee_old")
        execSQL("CREATE TABLE attributes (_id integer primary key autoincrement,attribute_name text not null,context text not null, unique (attribute_name, context))")
        Attribute.initDatabaseInternal(
            this@upgradeTo145, FinTsAttribute::class.java,
            "attributes", "attribute_name", "context"
        )
        Attribute.initDatabaseInternal(
            this@upgradeTo145, BankingAttribute::class.java,
            "attributes", "attribute_name", "context"
        )
        execSQL(
            "CREATE TABLE transaction_attributes (transaction_id integer references transactions(_id) ON DELETE CASCADE,attribute_id integer references attributes(_id) ON DELETE CASCADE,value text not null,primary key (transaction_id, attribute_id))"
        )
        execSQL(
            "CREATE TABLE account_attributes (account_id integer references accounts(_id) ON DELETE CASCADE,attribute_id integer references attributes(_id) ON DELETE CASCADE,value text not null,primary key (account_id, attribute_id))"
        )
    }

    fun SupportSQLiteDatabase.upgradeTo148() {
        execSQL("CREATE TABLE attachments (_id integer primary key autoincrement, uri text not null unique, uuid text not null unique)")
        execSQL("CREATE TABLE transaction_attachments (transaction_id integer references transactions(_id) ON DELETE CASCADE, attachment_id integer references attachments(_id), primary key (transaction_id, attachment_id))")
        execSQL("DROP TRIGGER IF EXISTS cache_stale_uri")
        //insert existing attachments from transaction table into attachments table
        //drop column picture_id
        query(
            "transactions",
            arrayOf("_id", "picture_id"),
            "picture_id is not null"
        ).use { cursor ->
            val attachmentValues = ContentValues(2)
            val joinValues = ContentValues(2)
            cursor.asSequence.forEach {
                attachmentValues.clear()
                attachmentValues.put(KEY_URI, it.getString(1))
                attachmentValues.put(KEY_UUID, Model.generateUuid())
                val id = insert("attachments", attachmentValues)
                joinValues.clear()
                joinValues.put(KEY_TRANSACTIONID, it.getLong(0))
                joinValues.put(KEY_ATTACHMENT_ID, id)
                insert("transaction_attachments", joinValues)
            }
        }
        query("stale_uris", arrayOf("picture_id"), selection = null).use { cursor ->
            val attachmentValues = ContentValues(2)
            cursor.asSequence.forEach {
                attachmentValues.clear()
                attachmentValues.put(KEY_URI, it.getString(0))
                attachmentValues.put(KEY_UUID, Model.generateUuid())
                insert("attachments", attachmentValues)
            }
        }
        execSQL("DROP table stale_uris")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            execSQL("ALTER TABLE transactions RENAME to transactions_old")
            execSQL(
                "CREATE TABLE transactions(" +
                        "_id integer primary key autoincrement, " +
                        "comment text, " +
                        "date datetime not null, " +
                        "value_date datetime not null, " +
                        "amount integer not null, " +
                        "cat_id integer references categories(_id), " +
                        "account_id integer not null references accounts(_id) ON DELETE CASCADE, " +
                        "payee_id integer references payee(_id), " +
                        "transfer_peer integer references transactions(_id), " +
                        "transfer_account integer references accounts(_id)," +
                        "method_id integer references paymentmethods(_id)," +
                        "parent_id integer references transactions(_id) ON DELETE CASCADE, " +
                        "status integer default 0, " +
                        "cr_status text not null check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')) default 'RECONCILED', " +
                        "number text, " +
                        "uuid text, " +
                        "original_amount integer, " +
                        "original_currency text, " +
                        "equivalent_amount integer, " +
                        "debt_id integer references debts(_id) ON DELETE SET NULL)"
            )
            execSQL(
                "INSERT INTO transactions (" +
                        "_id,comment,date,value_date,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status,number,uuid,original_amount,original_currency,equivalent_amount,debt_id) " +
                        "SELECT " +
                        "_id,comment,date,coalesce(value_date,date),amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status,number,uuid,original_amount,original_currency,equivalent_amount,debt_id FROM transactions_old"
            )
            execSQL("DROP TABLE transactions_old")
            createOrRefreshTransactionUsageTriggers(this)
            createOrRefreshTransactionDebtTriggers(this)
            execSQL(ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE)
            execSQL(TRANSACTIONS_UUID_INDEX_CREATE)
            execSQL(TRANSACTIONS_CAT_ID_INDEX)
            execSQL(TRANSACTIONS_PAYEE_ID_INDEX)
            execSQL("ALTER TABLE changes RENAME to changes_old")
            execSQL(
                "CREATE TABLE changes (" +
                        "account_id integer not null references accounts(_id) ON DELETE CASCADE," +
                        "type text not null check (type in ('created','updated','deleted','unsplit','metadata','link')), " +
                        "sync_sequence_local integer, " +
                        "uuid text not null, " +
                        "timestamp datetime DEFAULT (strftime('%s','now')), " +
                        "parent_uuid text, " +
                        "comment text, " +
                        "date datetime, " +
                        "value_date datetime, " +
                        "amount integer, " +
                        "original_amount integer, " +
                        "original_currency text, " +
                        "equivalent_amount integer, " +
                        "cat_id integer references categories(_id) ON DELETE SET NULL, " +
                        "payee_id integer references payee(_id) ON DELETE SET NULL, " +
                        "transfer_account integer references accounts(_id) ON DELETE SET NULL," +
                        "method_id integer references paymentmethods(_id) ON DELETE SET NULL," +
                        "cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID'))," +
                        "number text)"
            )
            execSQL(
                "INSERT INTO changes (" +
                        "account_id,type,sync_sequence_local,uuid,timestamp,parent_uuid,comment,date,value_date,amount,original_amount,original_currency,equivalent_amount,cat_id,payee_id,transfer_account,method_id,cr_status,number) " +
                        "SELECT " +
                        "account_id,type,sync_sequence_local,uuid,timestamp,parent_uuid,comment,date,value_date,amount,original_amount,original_currency,equivalent_amount,cat_id,payee_id,transfer_account,method_id,cr_status,number FROM changes_old"
            )
            execSQL("DROP TABLE changes_old")
        } else {
            execSQL("DROP TRIGGER IF EXISTS insert_change_log")
            execSQL("DROP TRIGGER IF EXISTS insert_after_update_change_log")
            execSQL("DROP TRIGGER IF EXISTS update_change_log")
            execSQL("DROP VIEW IF EXISTS $VIEW_COMMITTED")
            execSQL("DROP VIEW IF EXISTS $VIEW_UNCOMMITTED")
            execSQL("DROP VIEW IF EXISTS $VIEW_ALL")
            execSQL("DROP VIEW IF EXISTS $VIEW_EXTENDED")
            execSQL("DROP VIEW IF EXISTS $VIEW_CHANGES_EXTENDED")
            execSQL("DROP VIEW IF EXISTS $VIEW_WITH_ACCOUNT")
            execSQL("ALTER TABLE transactions DROP COLUMN picture_id")
            execSQL("ALTER TABLE changes DROP COLUMN picture_id")
        }
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        prefHandler.putInt(PrefKey.FIRST_INSTALL_DB_SCHEMA_VERSION, DATABASE_VERSION)
    }

    private fun upgradeIcons(db: SupportSQLiteDatabase, map: Map<String, String>) {
        map.forEach {
            db.update(
                "categories",
                ContentValues(1).apply {
                    put("icon", it.value)
                },
                "icon = ?",
                arrayOf(it.key)
            )
        }
    }

    private fun migrateCurrency(
        db: SupportSQLiteDatabase,
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
        if (db.insert("currency", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply {
                put("code", newCurrency.name)
            }) != -1L) {
            Timber.d("Currency %s inserted", newCurrency.name)
        }
    }

    fun createOrRefreshTransactionDebtTriggers(db: SupportSQLiteDatabase) {
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

    fun repairWithSealedAccounts(db: SupportSQLiteDatabase, run: Runnable) {
        db.execSQL("update accounts set sealed = -1 where sealed = 1")
        run.run()
        db.execSQL("update accounts set sealed = 1 where sealed = -1")
    }

    fun repairWithSealedAccountsAndDebts(db: SupportSQLiteDatabase, run: Runnable) {
        db.execSQL("update accounts set sealed = -1 where sealed = 1")
        db.execSQL("update debts set sealed = -1 where sealed = 1")
        run.run()
        db.execSQL("update accounts set sealed = 1 where sealed = -1")
        db.execSQL("update debts set sealed = 1 where sealed = -1")
    }

    fun createOrRefreshCategoryHierarchyTrigger(db: SupportSQLiteDatabase) {
        with(db) {
            execSQL("DROP TRIGGER IF EXISTS category_hierarchy_update")
            execSQL(CATEGORY_HIERARCHY_TRIGGER)
        }
    }

    fun createOrRefreshCategoryMainCategoryUniqueLabel(db: SupportSQLiteDatabase) {
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

    fun insertFinTSAttributes(db: SupportSQLiteDatabase) {
        Attribute.initDatabase(db, FinTsAttribute::class.java)
        Attribute.initDatabase(db, BankingAttribute::class.java)
    }
}

