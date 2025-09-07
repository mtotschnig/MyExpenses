package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.content.contentValuesOf
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Attribute
import org.totschnig.myexpenses.db2.BankingAttribute
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.model.DEFAULT_FLAG_ID
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.PREDEFINED_NAME_INACTIVE
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CONTEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IBAN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_ASSET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENT_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUPPORTS_RECONCILIATION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VERSION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VISIBLE
import org.totschnig.myexpenses.provider.DatabaseConstants.NULL_CHANGE_INDICATOR
import org.totschnig.myexpenses.provider.DatabaseConstants.NULL_ROW_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVED
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_EXCHANGE_RATES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_FLAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_TYPES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTACHMENTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BANKS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CHANGES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_EQUIVALENT_AMOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_METHODS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PLAN_INSTANCE_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PRICES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_SYNC_STATE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS_TAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTACHMENTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_ALL
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_CHANGES_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_PRIORITIZED_PRICES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_UNCOMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_WITH_ACCOUNT
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import kotlin.math.pow

const val DATABASE_VERSION = 181

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

private const val CATEGORY_HIERARCHY_TRIGGER = """
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

private const val CATEGORY_TYPE_INSERT_TRIGGER = """
CREATE TRIGGER category_type_insert
    AFTER INSERT
    ON $TABLE_CATEGORIES
    WHEN new.$KEY_PARENTID IS NOT NULL
    BEGIN
        UPDATE $TABLE_CATEGORIES SET $KEY_TYPE = (SELECT $KEY_TYPE FROM $TABLE_CATEGORIES WHERE $KEY_ROWID = new.$KEY_PARENTID) WHERE $KEY_ROWID = new.$KEY_ROWID;
    END
"""

const val CATEGORY_TYPE_UPDATE_TRIGGER_MAIN = """
CREATE TRIGGER category_type_update_type_main
    AFTER UPDATE
    ON $TABLE_CATEGORIES
    WHEN new.$KEY_TYPE IS NOT old.$KEY_TYPE
    BEGIN
        UPDATE $TABLE_CATEGORIES SET $KEY_TYPE = new.$KEY_TYPE WHERE $KEY_PARENTID = new.$KEY_ROWID;
    END
"""

private const val CATEGORY_TYPE_UPDATE_TRIGGER_SUB = """
CREATE TRIGGER category_type_update_type_sub
    BEFORE UPDATE
    ON $TABLE_CATEGORIES
    WHEN new.$KEY_TYPE IS NOT old.$KEY_TYPE AND new.$KEY_PARENTID IS NOT NULL AND new.$KEY_TYPE IS NOT (SELECT $KEY_TYPE FROM $TABLE_CATEGORIES WHERE $KEY_ROWID = new.$KEY_PARENTID)
    BEGIN
        SELECT RAISE (ABORT, 'sub category type must match parent type');
    END
"""

private const val CATEGORY_TYPE_MOVE_TRIGGER = """
CREATE TRIGGER category_type_move
    AFTER UPDATE
    ON $TABLE_CATEGORIES
    WHEN new.$KEY_PARENTID IS NOT old.$KEY_PARENTID AND new.$KEY_PARENTID IS NOT NULL
    BEGIN
        UPDATE $TABLE_CATEGORIES SET $KEY_TYPE = (SELECT $KEY_TYPE FROM $TABLE_CATEGORIES WHERE $KEY_ROWID = new.$KEY_PARENTID) WHERE $KEY_ROWID = new.$KEY_ROWID;
    END
"""

const val BANK_CREATE = """
CREATE TABLE $TABLE_BANKS ($KEY_ROWID integer primary key autoincrement, $KEY_BLZ text not null, $KEY_BIC text not null, $KEY_BANK_NAME text not null, $KEY_USER_ID text not null, $KEY_VERSION interger not null, unique($KEY_BLZ, $KEY_USER_ID))
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

const val ACCOUNT_TYPE_CREATE = """
CREATE TABLE $TABLE_ACCOUNT_TYPES (
    $KEY_ROWID integer primary key autoincrement,
    $KEY_LABEL text not null,
    $KEY_TYPE_SORT_KEY integer not null default 0,
    $KEY_IS_ASSET boolean not null,
    $KEY_SUPPORTS_RECONCILIATION boolean not null
)
"""

const val ACCOUNT_FLAG_CREATE = """
CREATE TABLE $TABLE_ACCOUNT_FLAGS (
    $KEY_ROWID integer primary key autoincrement,
    $KEY_FLAG_LABEL text unique not null,
    $KEY_FLAG_SORT_KEY integer not null default 0,
    $KEY_FLAG_ICON text,
    $KEY_VISIBLE boolean not null
)
"""

//the unique index on ($KEY_PAYEE_NAME, $KEY_IBAN) does not prevent duplicate names when iban is null
const val PAYEE_UNIQUE_INDEX = """
CREATE UNIQUE INDEX payee_name ON $TABLE_PAYEES($KEY_PAYEE_NAME) WHERE $KEY_IBAN IS NULL;
"""

const val TAGS_CREATE =
    """CREATE TABLE $TABLE_TAGS (
        $KEY_ROWID integer primary key autoincrement,
        $KEY_LABEL text UNIQUE not null,
        $KEY_COLOR integer default null);"""

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

const val EQUIVALENT_AMOUNTS_CREATE = """
    CREATE TABLE $TABLE_EQUIVALENT_AMOUNTS (
    $KEY_TRANSACTIONID integer NOT NULL references $TABLE_TRANSACTIONS($KEY_ROWID) ON DELETE CASCADE,
    $KEY_CURRENCY text NOT NULL references $TABLE_CURRENCIES ($KEY_CODE) ON DELETE CASCADE,
    $KEY_EQUIVALENT_AMOUNT integer NOT NULL,
    primary key ($KEY_TRANSACTIONID, $KEY_CURRENCY)
);
"""

const val PRICES_CREATE = """
    CREATE TABLE $TABLE_PRICES (
    $KEY_COMMODITY text NOT NULL,
    $KEY_CURRENCY text NOT NULL references $TABLE_CURRENCIES ($KEY_CODE) ON DELETE CASCADE,
    $KEY_DATE datetime NOT NULL,
    $KEY_SOURCE text NOT NULL,
    $KEY_TYPE text default 'unknown',
    $KEY_VALUE real not NULL,
    primary key($KEY_COMMODITY, $KEY_CURRENCY, $KEY_DATE, $KEY_SOURCE, $KEY_TYPE)
);
"""

val PRIORITIZED_PRICES_CREATE = """
    CREATE VIEW $VIEW_PRIORITIZED_PRICES AS SELECT
    p1.$KEY_CURRENCY,
    p1.$KEY_COMMODITY,
    p1.$KEY_DATE,
    p1.$KEY_SOURCE,
    p1.$KEY_VALUE
FROM
    $TABLE_PRICES AS p1 WHERE
    p1.$KEY_SOURCE = (
        SELECT p2.$KEY_SOURCE
        FROM $TABLE_PRICES AS p2
        WHERE p2.$KEY_CURRENCY = p1.$KEY_CURRENCY AND p2.$KEY_COMMODITY = p1.$KEY_COMMODITY AND p2.$KEY_DATE = p1.$KEY_DATE
        ORDER BY ${priceSort("p2")}
        LIMIT 1
    );
"""

fun priceSort(alias: String? = null): String {
    val prefix = alias?.let { "$it." } ?: ""
    return """
    CASE
                WHEN $prefix$KEY_SOURCE = '${ExchangeRateSource.User.name}' THEN 1
                WHEN $prefix$KEY_SOURCE = '${ExchangeRateSource.Calculation.name}' THEN 3
                ELSE 2
            END,
            $prefix$KEY_SOURCE DESC
""".trimIndent()
}

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
    "CREATE TRIGGER insert_increase_category_usage AFTER INSERT ON $TABLE_TRANSACTIONS WHEN new.$KEY_CATID IS NOT NULL AND new.$KEY_CATID != $SPLIT_CATID$INCREASE_CATEGORY_USAGE_ACTION"

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

const val TRANSACTIONS_PARENT_ID_INDEX =
    "CREATE INDEX transactions_parent_id_index on $TABLE_TRANSACTIONS($KEY_PARENTID)"

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
 BEFORE UPDATE OF $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, $KEY_AMOUNT, $KEY_CATID, $KEY_ACCOUNTID, $KEY_PAYEEID, $KEY_TRANSFER_PEER, $KEY_TRANSFER_ACCOUNT, $KEY_METHODID, $KEY_PARENTID, $KEY_REFERENCE_NUMBER, $KEY_UUID, $KEY_ORIGINAL_AMOUNT, $KEY_ORIGINAL_CURRENCY, $KEY_DEBT_ID, $KEY_CR_STATUS
 ON $TABLE_TRANSACTIONS
 WHEN (SELECT max($KEY_SEALED) FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID IN (new.$KEY_ACCOUNTID,old.$KEY_ACCOUNTID)) = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""

//we allow update of cr_status and status
const val TRANSFER_SEALED_UPDATE_TRIGGER_CREATE =
    """CREATE TRIGGER sealed_account_tranfer_update
 BEFORE UPDATE OF $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, $KEY_AMOUNT, $KEY_CATID, $KEY_ACCOUNTID, $KEY_PAYEEID, $KEY_TRANSFER_PEER, $KEY_TRANSFER_ACCOUNT, $KEY_METHODID, $KEY_PARENTID, $KEY_REFERENCE_NUMBER, $KEY_UUID, $KEY_ORIGINAL_AMOUNT, $KEY_ORIGINAL_CURRENCY, $KEY_DEBT_ID
 ON $TABLE_TRANSACTIONS
 WHEN (SELECT $KEY_SEALED FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = old.$KEY_TRANSFER_ACCOUNT) = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""

const val TRANSACTIONS_SEALED_DELETE_TRIGGER_CREATE =
    """CREATE TRIGGER sealed_account_transaction_delete
 BEFORE DELETE ON $TABLE_TRANSACTIONS
 WHEN (SELECT $KEY_SEALED FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = old.$KEY_ACCOUNTID) = 1
 BEGIN $RAISE_UPDATE_SEALED_ACCOUNT END"""

private const val TRANSACTIONS_ARCHIVE_TRIGGER =
    """CREATE TRIGGER transaction_archive_trigger
        AFTER UPDATE ON $TABLE_TRANSACTIONS WHEN new.$KEY_STATUS != old.$KEY_STATUS AND new.$KEY_STATUS = $STATUS_ARCHIVED
        BEGIN UPDATE $TABLE_TRANSACTIONS SET $KEY_STATUS = $STATUS_ARCHIVED WHERE $KEY_PARENTID = new.$KEY_ROWID; END;
    """

private const val TRANSACTIONS_UNARCHIVE_TRIGGER =
    """CREATE TRIGGER transaction_unarchive_trigger
        AFTER UPDATE ON $TABLE_TRANSACTIONS WHEN new.$KEY_STATUS != old.$KEY_STATUS AND old.$KEY_STATUS = $STATUS_ARCHIVED
        BEGIN UPDATE $TABLE_TRANSACTIONS SET $KEY_STATUS = new.$KEY_STATUS WHERE $KEY_PARENTID = new.$KEY_ROWID; END;
    """

const val VIEW_WITH_ACCOUNT_DEFINITION =
    """CREATE VIEW $VIEW_WITH_ACCOUNT AS SELECT $TABLE_TRANSACTIONS.*, $TABLE_CATEGORIES.$KEY_TYPE, $TABLE_ACCOUNTS.$KEY_COLOR, $KEY_CURRENCY, $KEY_EXCLUDE_FROM_TOTALS, $KEY_DYNAMIC, $TABLE_ACCOUNTS.$KEY_TYPE AS $KEY_ACCOUNT_TYPE, $TABLE_ACCOUNTS.$KEY_LABEL AS $KEY_ACCOUNT_LABEL FROM $TABLE_TRANSACTIONS LEFT JOIN $TABLE_CATEGORIES on $KEY_CATID = $TABLE_CATEGORIES.$KEY_ROWID LEFT JOIN $TABLE_ACCOUNTS ON $KEY_ACCOUNTID = $TABLE_ACCOUNTS.$KEY_ROWID WHERE $KEY_STATUS != $STATUS_UNCOMMITTED"""

const val SPLIT_PART_CR_STATUS_TRIGGER_CREATE =
    """CREATE TRIGGER split_part_cr_status_trigger
 AFTER UPDATE OF $KEY_CR_STATUS ON $TABLE_TRANSACTIONS
 BEGIN UPDATE $TABLE_TRANSACTIONS SET $KEY_CR_STATUS = new.$KEY_CR_STATUS WHERE $KEY_PARENTID = new.$KEY_ROWID; END"""

private const val DEFAULT_TRANSFER_CATEGORY_UUID = "9d84b522-4c8c-40bd-a8f8-18c8788ee59e"

fun buildChangeTriggerDefinitionForColumnNotNull(column: String) =
    "CASE WHEN old.$column = new.$column THEN NULL ELSE new.$column END"

fun buildChangeTriggerDefinitionForTextColumn(column: String) =
    "CASE WHEN old.$column = new.$column THEN NULL WHEN old.$column IS NOT NULL AND new.$column IS NULL THEN '' ELSE new.$column END"

fun buildChangeTriggerDefinitionForIntegerColumn(column: String) =
    "CASE WHEN old.$column = new.$column THEN NULL WHEN old.$column IS NOT NULL AND new.$column IS NULL THEN ${Long.MIN_VALUE} ELSE new.$column END"

fun buildChangeTriggerDefinitionForReferenceColumn(column: String) =
    "CASE WHEN old.$column = new.$column THEN NULL WHEN old.$column IS NOT NULL AND new.$column IS NULL THEN $NULL_ROW_ID ELSE new.$column END"

fun SupportSQLiteDatabase.createOrRefreshTransactionLinkedTableTriggers() {
    linkedTableTrigger("INSERT", TABLE_TRANSACTIONS_TAGS)
    linkedTableTrigger("DELETE", TABLE_TRANSACTIONS_TAGS)
    linkedTableTrigger("INSERT", TABLE_TRANSACTION_ATTACHMENTS)
    linkedTableTrigger("DELETE", TABLE_TRANSACTION_ATTACHMENTS)
}

fun SupportSQLiteDatabase.linkedTableTrigger(
    operation: String,
    table: String
) {
    val reference = when (operation) {
        "INSERT" -> "new"
        "DELETE" -> "old"
        else -> throw IllegalArgumentException()
    }
    val type = when (table) {
        TABLE_TRANSACTIONS_TAGS -> TransactionChange.Type.tags
        TABLE_TRANSACTION_ATTACHMENTS -> TransactionChange.Type.attachments
        else -> throw IllegalArgumentException()
    }
    val triggerName = triggerName(operation, table)
    execSQL("DROP TRIGGER IF EXISTS $triggerName")
    execSQL(
        """
    CREATE TRIGGER $triggerName AFTER $operation ON $table
    WHEN ${shouldWriteChangeTemplate(reference, table)}
        BEGIN INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_UUID, $KEY_PARENT_UUID, $KEY_ACCOUNTID, $KEY_SYNC_SEQUENCE_LOCAL)
        VALUES ('${type.name}', (SELECT $KEY_UUID FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = $reference.$KEY_TRANSACTIONID),
        ${parentUuidExpression(reference, table)},
        (SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = $reference.$KEY_TRANSACTIONID), 
        ${sequenceNumberSelect(reference, table)}); END
"""
    )
}

fun triggerName(operation: String, table: String) =
    "${operation.lowercase()}_change_log_${table.substringAfter('_')}"

@JvmOverloads
fun shouldWriteChangeTemplate(reference: String, table: String = TABLE_TRANSACTIONS) =
    """EXISTS (SELECT 1 FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = ${
        referenceForTable(reference, table, KEY_ACCOUNTID)
    } AND $KEY_SYNC_ACCOUNT_NAME IS NOT NULL AND $KEY_SYNC_SEQUENCE_LOCAL > 0) AND NOT EXISTS (SELECT 1 FROM $TABLE_SYNC_STATE)"""

private fun referenceForTable(reference: String, table: String, column: String) = when (table) {
    TABLE_TRANSACTIONS -> "$reference.$column"
    TABLE_TRANSACTIONS_TAGS, TABLE_TRANSACTION_ATTACHMENTS, TABLE_EQUIVALENT_AMOUNTS -> "(SELECT $column FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = $reference.$KEY_TRANSACTIONID)"
    else -> throw IllegalArgumentException()
}

@JvmOverloads
fun sequenceNumberSelect(reference: String, table: String = TABLE_TRANSACTIONS) =
    "(SELECT $KEY_SYNC_SEQUENCE_LOCAL FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = ${
        referenceForTable(reference, table, KEY_ACCOUNTID)
    })"

@JvmOverloads
fun parentUuidExpression(reference: String, table: String = TABLE_TRANSACTIONS) =
    """CASE
        WHEN ${referenceForTable(reference, table, KEY_PARENTID)} IS NULL
        THEN NULL
        ELSE (
            SELECT $KEY_UUID
            FROM $TABLE_TRANSACTIONS parent
            WHERE $KEY_ROWID = ${referenceForTable(reference, table, KEY_PARENTID)}
        )
       END"""


const val UPDATE_ACCOUNT_SYNC_NULL_TRIGGER =
    "CREATE TRIGGER update_account_sync_null AFTER UPDATE ON $TABLE_ACCOUNTS WHEN new.$KEY_SYNC_ACCOUNT_NAME IS NULL AND old.$KEY_SYNC_ACCOUNT_NAME IS NOT NULL BEGIN UPDATE $TABLE_ACCOUNTS SET $KEY_SYNC_SEQUENCE_LOCAL = 0 WHERE $KEY_ROWID = old.$KEY_ROWID; DELETE FROM $TABLE_CHANGES WHERE $KEY_ACCOUNTID = old.$KEY_ROWID; END;"

const val ACCOUNTS_TRIGGER_CREATE =
    "CREATE TRIGGER sort_key_default AFTER INSERT ON $TABLE_ACCOUNTS BEGIN UPDATE $TABLE_ACCOUNTS SET $KEY_SORT_KEY = (SELECT coalesce(max($KEY_SORT_KEY),0) FROM $TABLE_ACCOUNTS) + 1 WHERE $KEY_ROWID = NEW.$KEY_ROWID; END"

const val UPDATE_ACCOUNT_METADATA_TRIGGER =
    """CREATE TRIGGER update_account_metadata AFTER UPDATE OF $KEY_LABEL,$KEY_OPENING_BALANCE,$KEY_DESCRIPTION,$KEY_CURRENCY,$KEY_TYPE,$KEY_COLOR,$KEY_EXCLUDE_FROM_TOTALS,$KEY_CRITERION ON $TABLE_ACCOUNTS 
       WHEN new.$KEY_SYNC_ACCOUNT_NAME IS NOT NULL AND new.$KEY_SYNC_SEQUENCE_LOCAL > 0 AND NOT EXISTS (SELECT 1 FROM $TABLE_SYNC_STATE)
       BEGIN INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_UUID, $KEY_ACCOUNTID, $KEY_SYNC_SEQUENCE_LOCAL) VALUES ('metadata', '_ignored_', new.$KEY_ROWID, new.$KEY_SYNC_SEQUENCE_LOCAL); END;
"""

val UPDATE_ACCOUNT_EXCHANGE_RATE_TRIGGER =
    """CREATE TRIGGER update_account_exchange_rate AFTER UPDATE ON $TABLE_ACCOUNT_EXCHANGE_RATES
    WHEN ${shouldWriteChangeTemplate("new")}
    BEGIN INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_UUID, $KEY_ACCOUNTID, $KEY_SYNC_SEQUENCE_LOCAL)
    VALUES ('metadata', '_ignored_', new.$KEY_ACCOUNTID, ${sequenceNumberSelect("old")});
    END;"""

const val DEFAULT_FLAG_TRIGGER =
    """CREATE TRIGGER protect_default_flag BEFORE DELETE ON $TABLE_ACCOUNT_FLAGS WHEN (OLD.$KEY_ROWID = 0) BEGIN SELECT RAISE (FAIL, 'default flag can not be deleted'); END;"""

abstract class BaseTransactionDatabase(
    val context: Context,
    val prefHandler: PrefHandler
) :
    SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    fun SupportSQLiteDatabase.createOrRefreshTransactionUsageTriggers() {
        execSQL(INCREASE_CATEGORY_USAGE_INSERT_TRIGGER)
        execSQL(INCREASE_CATEGORY_USAGE_UPDATE_TRIGGER)
        execSQL(INCREASE_ACCOUNT_USAGE_INSERT_TRIGGER)
        execSQL(INCREASE_ACCOUNT_USAGE_UPDATE_TRIGGER)
    }

    fun SupportSQLiteDatabase.upgradeTo117() {
        migrateCurrency("VEB", CurrencyEnum.VES)
        migrateCurrency("MRO", CurrencyEnum.MRU)
        migrateCurrency("STD", CurrencyEnum.STN)
    }

    fun SupportSQLiteDatabase.upgradeTo118() {
        execSQL("ALTER TABLE planinstance_transaction RENAME to planinstance_transaction_old")
        //make sure we have only one instance per template
        execSQL(
            "CREATE TABLE planinstance_transaction " +
                    "(template_id integer references templates(_id) ON DELETE CASCADE, " +
                    "instance_id integer, " +
                    "transaction_id integer unique references transactions(_id) ON DELETE CASCADE," +
                    "primary key (template_id, instance_id));"
        )
        execSQL(
            ("INSERT OR IGNORE INTO planinstance_transaction " +
                    "(template_id,instance_id,transaction_id)" +
                    "SELECT " +
                    "template_id,instance_id,transaction_id FROM planinstance_transaction_old")
        )
        execSQL("DROP TABLE planinstance_transaction_old")
    }

    fun SupportSQLiteDatabase.upgradeTo119() {
        execSQL("ALTER TABLE transactions add column debt_id integer references debts (_id) ON DELETE SET NULL")
        execSQL(
            "CREATE TABLE debts (_id integer primary key autoincrement, payee_id integer references payee(_id) ON DELETE CASCADE, date datetime not null, label text not null, amount integer, currency text not null, description text, sealed boolean default 0);"
        )
        createOrRefreshTransactionDebtTriggers()
    }

    fun SupportSQLiteDatabase.upgradeTo120() {
        execSQL("DROP TRIGGER IF EXISTS transaction_debt_insert")
        execSQL("DROP TRIGGER IF EXISTS transaction_debt_update")
    }

    fun SupportSQLiteDatabase.upgradeTo122() {
        //repair transactions corrupted due to bug https://github.com/mtotschnig/MyExpenses/issues/921
        repairWithSealedAccountsAndDebts {
            execSQL(
                "update transactions set transfer_account = (select account_id from transactions peer where _id = transactions.transfer_peer);"
            )
        }
        execSQL("DROP TRIGGER IF EXISTS account_remap_transfer_transaction_update")
        execSQL(ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE)
    }

    fun SupportSQLiteDatabase.upgradeTo124() {
        repairWithSealedAccounts {
            query("accounts", arrayOf("_id"), "uuid is null", null, null, null, null)
                .use { cursor ->
                    cursor.asSequence.forEach {
                        execSQL(
                            "update accounts set uuid = ? where _id =?",
                            arrayOf(Model.generateUuid(), it.getLong(0))
                        )
                    }
                }
        }
    }

    fun SupportSQLiteDatabase.upgradeTo125() {
        execSQL("ALTER TABLE categories RENAME to categories_old")
        execSQL(
            "CREATE TABLE categories (_id integer primary key autoincrement, label text not null, label_normalized text, parent_id integer references categories(_id) ON DELETE CASCADE, usages integer default 0, last_used datetime, color integer, icon string, UNIQUE (label,parent_id));"
        )
        execSQL("INSERT INTO categories (_id, label, label_normalized, parent_id, usages, last_used, color, icon) SELECT _id, label, label_normalized, parent_id, usages, last_used, color, icon FROM categories_old")
        execSQL("DROP TABLE categories_old")
        createOrRefreshCategoryMainCategoryUniqueLabel()
        createOrRefreshCategoryHierarchyTrigger()
    }

    fun SupportSQLiteDatabase.upgradeTo126() {
        //trigger caused a hanging query, because it did not check if parent_id was updated
        createOrRefreshCategoryHierarchyTrigger()
        //subcategories should not have a color
        update(
            "categories",
            ContentValues(1).apply {
                putNull("color")
            },
            "parent_id IS NOT NULL", null
        )
        //main categories need a color
        query(
            "categories",
            arrayOf("_id"),
            "parent_id is null AND color is null",
            null,
            null,
            null,
            null
        ).use { cursor ->
            cursor.asSequence.forEach {
                update(
                    "categories",
                    ContentValues(1).apply {
                        put(KEY_COLOR, suggestNewCategoryColor(this@upgradeTo126))
                    },
                    "_id = ?",
                    arrayOf(it.getLong(0).toString())
                )
            }
        }
    }

    fun SupportSQLiteDatabase.upgradeTo128() {
        execSQL("UPDATE categories SET icon = replace(icon,'_','-')")
        upgradeIcons(
            this, mapOf(
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

    fun SupportSQLiteDatabase.upgradeTo129() {
        execSQL("CREATE TABLE budgets_neu (_id integer primary key autoincrement, title text not null default '', description text not null, grouping text not null check (grouping in ('NONE','DAY','WEEK','MONTH','YEAR')), account_id integer references accounts(_id) ON DELETE CASCADE, currency text, start datetime, `end` datetime)")
        execSQL("CREATE TABLE budget_allocations (budget_id integer not null references budgets(_id) ON DELETE CASCADE, cat_id integer not null references categories(_id) ON DELETE CASCADE, year integer, second integer, budget integer, rollOverPrevious integer, rollOverNext integer, oneTime boolean default 0, primary key (budget_id,cat_id,year,second))")
        execSQL("INSERT INTO budgets_neu (_id, title, description, grouping, account_id, currency, start, `end`) SELECT _id, title, coalesce(description,''), grouping, account_id, currency, start, `end` FROM budgets")
        execSQL("INSERT INTO budget_allocations (budget_id, cat_id, budget) SELECT _id, 0, budget FROM budgets")
        execSQL("INSERT INTO budget_allocations (budget_id, cat_id, budget) SELECT budget_id, cat_id, budget FROM budget_categories")
        execSQL("DROP TABLE budgets")
        execSQL("DROP TABLE budget_categories")
        execSQL("ALTER TABLE budgets_neu RENAME to budgets")
        execSQL("CREATE INDEX budget_allocations_cat_id_index on budget_allocations(cat_id)")
    }

    fun SupportSQLiteDatabase.upgradeTo130() {
        upgradeIcons(this, mapOf("car-crash" to "car-burst"))
    }

    fun SupportSQLiteDatabase.upgradeTo131() {
        execSQL("ALTER TABLE budgets add column is_default boolean default 0")
    }

    fun SupportSQLiteDatabase.upgradeTo133() {
        execSQL(
            "ALTER TABLE currency add column sort_direction text not null check (sort_direction  in ('ASC','DESC')) default 'DESC'"
        )
    }

    fun SupportSQLiteDatabase.upgradeTo136() {
        query(
            "categories",
            arrayOf("_id"),
            "uuid is null",
            null,
            null,
            null,
            null
        ).use { cursor ->
            cursor.asSequence.forEach {
                update(
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
            this, FinTsAttribute::class.java,
            "attributes", "attribute_name", "context"
        )
        Attribute.initDatabaseInternal(
            this, BankingAttribute::class.java,
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
        val attachmentValues = ContentValues(2)
        val joinValues = ContentValues(2)
        query(
            "transactions",
            arrayOf("_id", "picture_id"),
            "picture_id is not null"
        ).useAndMapToList { cursor ->
            cursor.getLong(0) to cursor.getString(1)
        }.groupBy({ it.second }, { it.first }).forEach { (uri, transactionIds) ->
            attachmentValues.clear()
            joinValues.clear()
            attachmentValues.put(KEY_URI, uri)
            attachmentValues.put(KEY_UUID, Model.generateUuid())
            val id = insert("attachments", attachmentValues)
            joinValues.put(KEY_ATTACHMENT_ID, id)
            transactionIds.forEach {
                joinValues.put(KEY_TRANSACTIONID, it)
                insert("transaction_attachments", joinValues)
            }
        }
        query("stale_uris", arrayOf("picture_id"), selection = null).use { cursor ->
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
            createOrRefreshTransactionUsageTriggers()
            createOrRefreshTransactionDebtTriggers()
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

    fun SupportSQLiteDatabase.upgradeTo155() {
        val defaultTransferCategoryLabel = context.getString(R.string.transfer)
        val conflictingEntry = query(
            TABLE_CATEGORIES,
            arrayOf(KEY_ROWID, KEY_TYPE),
            "$KEY_LABEL = ?",
            arrayOf(defaultTransferCategoryLabel)
        ).use {
            if (it.moveToFirst()) it.getLong(0) to it.getInt(1) else null
        }
        if (conflictingEntry == null) {
            insertDefaultTransferCategory(this, defaultTransferCategoryLabel)
        } else {
            if (conflictingEntry.second == FLAG_TRANSFER.toInt()) {
                prefHandler.putLong(PrefKey.DEFAULT_TRANSFER_CATEGORY, conflictingEntry.first)
            } else {
                insertDefaultTransferCategory(this, "_${defaultTransferCategoryLabel}_")
            }
        }
    }

    fun SupportSQLiteDatabase.upgradeTo156() {
        //repair split transactions corrupted due to bug https://github.com/mtotschnig/MyExpenses/issues/1333
        repairWithSealedAccountsAndDebts {
            val affected =
                query("select _id,account_id from transactions where cat_id = 0 and exists(select 1 from transactions parts where parts.parent_id = transactions._id and parts.account_id != transactions.account_id)").use { cursor ->
                    cursor.asSequence.map { it.getLong(0) to it.getLong(1) }.toList()
                }
            if (affected.isNotEmpty()) {
                affected.forEach {
                    Timber.w(
                        "Updated %d transactions",
                        update(
                            "transactions",
                            ContentValues(1).apply { put("account_id", it.second) },
                            "parent_id = ?",
                            arrayOf(it.first)
                        )
                    )
                }
                CrashHandler.report(Exception("Found and repaired ${affected.size} corrupted split transactions"))
            }
        }
    }

    fun SupportSQLiteDatabase.upgradeTo157() {
        repairWithSealedAccountsAndDebts {
            prefHandler.defaultTransferCategory?.let {
                execSQL("UPDATE transactions SET cat_id = $it WHERE cat_id IS NULL AND transfer_peer is not null")
            }
        }
    }

    fun SupportSQLiteDatabase.upgradeTo158() {
        prefHandler.defaultTransferCategory?.let {
            execSQL("UPDATE categories SET uuid = '$DEFAULT_TRANSFER_CATEGORY_UUID' WHERE _id = $it")
        }
    }

    fun SupportSQLiteDatabase.upgradeTo159() {
        prefHandler.defaultTransferCategory?.let {
            execSQL("UPDATE templates SET cat_id = $it WHERE cat_id IS NULL AND transfer_account is not null")
        }
    }

    fun SupportSQLiteDatabase.upgradeTo160() {
        insertNullRows()
        execSQL("DROP TRIGGER IF EXISTS update_change_log")
        execSQL(TRANSACTIONS_UPDATE_TRIGGER_CREATE)
    }

    fun SupportSQLiteDatabase.upgradeTo161() {
        execSQL("DROP VIEW IF EXISTS $VIEW_CHANGES_EXTENDED")
        //add new change type
        execSQL("ALTER TABLE changes RENAME to changes_old")
        execSQL(
            "CREATE TABLE changes (account_id integer not null references accounts(_id) ON DELETE CASCADE,type text not null check (type in ('created','updated','deleted','unsplit','metadata','link','tags','attachments')), sync_sequence_local integer, uuid text not null, timestamp datetime DEFAULT (strftime('%s','now')), parent_uuid text, comment text, date datetime, value_date datetime, amount integer, original_amount integer, original_currency text, equivalent_amount integer, cat_id integer references categories(_id) ON DELETE SET NULL, payee_id integer references payee(_id) ON DELETE SET NULL, transfer_account integer references accounts(_id) ON DELETE SET NULL,method_id integer references paymentmethods(_id) ON DELETE SET NULL,cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')),number text)"
        )
        execSQL(
            "INSERT INTO changes SELECT * FROM changes_old"
        )
        execSQL("DROP TABLE changes_old")
        execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES))
        createOrRefreshTransactionLinkedTableTriggers()
    }

    fun SupportSQLiteDatabase.upgradeTo163() {
        execSQL("DROP TRIGGER IF EXISTS uuid_update_change_log")
        execSQL(TRANSACTIONS_UUID_UPDATE_TRIGGER_CREATE)
    }

    fun SupportSQLiteDatabase.upgradeTo164() {
        safeInsert("attributes", ContentValues().apply {
            put("attribute_name", BankingAttribute.BLZ.name)
            put("context", BankingAttribute.BLZ.context)
        })
    }

    fun SupportSQLiteDatabase.upgradeTo165() {
        execSQL("ALTER TABLE templates add column original_amount integer")
        execSQL("ALTER TABLE templates add column original_currency text")
    }

    fun SupportSQLiteDatabase.upgradeTo167() {
        safeInsert("attributes", ContentValues().apply {
            put("attribute_name", BankingAttribute.NAME.name)
            put("context", BankingAttribute.NAME.context)
        })
        safeInsert("attributes", ContentValues().apply {
            put("attribute_name", BankingAttribute.BIC.name)
            put("context", BankingAttribute.BIC.context)
        })
    }

    fun SupportSQLiteDatabase.upgradeTo168() {
        execSQL("DROP VIEW IF EXISTS $VIEW_CHANGES_EXTENDED")
        //add new change type
        execSQL("ALTER TABLE changes RENAME to changes_old")
        execSQL(
            "CREATE TABLE changes (account_id integer not null references accounts(_id) ON DELETE CASCADE,type text not null check (type in ('created','updated','deleted','unsplit','metadata','link','tags','attachments','unarchive')), sync_sequence_local integer, uuid text not null, timestamp datetime DEFAULT (strftime('%s','now')), parent_uuid text, comment text, date datetime, value_date datetime, amount integer, original_amount integer, original_currency text, equivalent_amount integer, cat_id integer references categories(_id) ON DELETE SET NULL, payee_id integer references payee(_id) ON DELETE SET NULL, transfer_account integer references accounts(_id) ON DELETE SET NULL,method_id integer references paymentmethods(_id) ON DELETE SET NULL,cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')), status integer default 0, number text)"
        )
        execSQL(
            "INSERT INTO changes (" +
                    "account_id,type,sync_sequence_local,uuid,timestamp,parent_uuid,comment,date,value_date,amount,original_amount,original_currency,equivalent_amount,cat_id,payee_id,transfer_account,method_id,cr_status,status,number) " +
                    "SELECT " +
                    "account_id,type,sync_sequence_local,uuid,timestamp,parent_uuid,comment,date,value_date,amount,original_amount,original_currency,equivalent_amount,cat_id,payee_id,transfer_account,method_id,cr_status,0,number FROM changes_old"
        )
        execSQL("DROP TABLE changes_old")
        execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES))
        createOrRefreshChangeLogTriggers()
    }

    fun SupportSQLiteDatabase.upgradeTo169() {
        repairWithSealedAccountsAndDebts {
            execSQL("UPDATE transactions SET status = 0 WHERE status = 5 AND parent_id is null")
        }
    }

    fun SupportSQLiteDatabase.upgradeTo170() {
        repairWithSealedAccountsAndDebts {
            // KEY_ARCHIVED = 5, SPLIT_CAT_ID = 0
            val newStatus = ContentValues(1).apply {
                put("status", 5)
            }
            query(
                "transactions",
                arrayOf("_id"),
                "status = ? AND cat_id = ?",
                arrayOf(5, 0)
            ).use { cursor ->
                cursor.asSequence.forEach {
                    update("transactions", newStatus, "parent_id = ?", arrayOf(it.getLong(0)))
                }
            }
        }
        createArchiveTriggers()
    }

    fun SupportSQLiteDatabase.upgradeTo171() {
        execSQL("ALTER TABLE budgets add column uuid text")
        query("budgets", arrayOf("_id"), null, null, null, null, null)
            .use { cursor ->
                cursor.asSequence.forEach {
                    execSQL(
                        "update budgets set uuid = ? where _id =?",
                        arrayOf(Model.generateUuid(), it.getLong(0))
                    )
                }
            }
    }

    fun SupportSQLiteDatabase.upgradeTo173() {
        //create new tables
        execSQL("CREATE TABLE prices (commodity text NOT NULL, currency text NOT NULL references currency(code) ON DELETE CASCADE, date datetime NOT NULL, source text NOT NULL, type text default 'unknown', value real not NULL, primary key(commodity, currency, date, source, type));")
        execSQL("CREATE TABLE equivalent_amounts (transaction_id integer not null references transactions(_id) ON DELETE CASCADE, currency text not null references currency (code) ON DELETE CASCADE, equivalent_amount integer not null, primary key (transaction_id, currency));")
        //ADD column dynamic
        execSQL("ALTER TABLE accounts add column dynamic boolean default 0;")
        //set dynamic flag for accounts where we have equivalent amounts
        execSQL("UPDATE accounts set dynamic = 1 where exists (SELECT 1 from transactions where account_id = accounts._id and equivalent_amount is not null)")
        //Migrate equivalent amounts from transactions table to new join table
        execSQL(
            "INSERT INTO equivalent_amounts (transaction_id, currency, equivalent_amount) SELECT _id, '${context.injector.currencyContext().homeCurrencyString}', equivalent_amount FROM transactions WHERE equivalent_amount IS NOT NULL;"
        )
        createOrRefreshEquivalentAmountTriggers()
        //DROP column equivalent_amount
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            execSQL("ALTER TABLE transactions RENAME TO transactions_old")
            execSQL(
                "CREATE TABLE transactions(_id integer primary key autoincrement, comment text, date datetime not null, value_date datetime not null, amount integer not null, cat_id integer references categories(_id), account_id integer not null references accounts(_id) ON DELETE CASCADE, payee_id integer references payee(_id), transfer_peer integer references transactions(_id), transfer_account integer references accounts(_id),method_id integer references paymentmethods(_id),parent_id integer references transactions(_id) ON DELETE CASCADE, status integer default 0, cr_status text not null check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')) default 'RECONCILED', number text, uuid text, original_amount integer, original_currency text, debt_id integer references debts(_id) ON DELETE SET NULL);\n"
            )
            execSQL(
                "INSERT INTO transactions (" +
                        "_id,comment,date,value_date,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status,number,uuid,original_amount,original_currency,debt_id) " +
                        "SELECT " +
                        "_id,comment,date,value_date,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status,number,uuid,original_amount,original_currency,debt_id FROM transactions_old"
            )
            execSQL("DROP TABLE transactions_old")
            createOrRefreshTransactionUsageTriggers()
            createOrRefreshTransactionDebtTriggers()
            execSQL(ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE)
            execSQL(TRANSACTIONS_UUID_INDEX_CREATE)
            execSQL(TRANSACTIONS_CAT_ID_INDEX)
            execSQL(TRANSACTIONS_PAYEE_ID_INDEX)
            execSQL(TRANSACTIONS_PARENT_ID_INDEX)
            execSQL(SPLIT_PART_CR_STATUS_TRIGGER_CREATE)
            createArchiveTriggers()
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
            execSQL("ALTER TABLE transactions DROP COLUMN equivalent_amount")
        }
    }

    fun SupportSQLiteDatabase.upgradeTo175() {
        val currencyContext = context.injector.currencyContext()
        query("select distinct commodity, currency from prices").use { cursor ->
            cursor.asSequence.forEach {
                val commodity = it.getString(0)
                val currency = it.getString(1)
                val delta =
                    currencyContext[currency].fractionDigits - currencyContext[commodity].fractionDigits
                if (delta != 0) {
                    val coefficient = 10.0.pow(delta)
                    execSQL("update prices set value = value * $coefficient where commodity = '$commodity' and currency = '$currency'")
                }
            }
        }
    }

    fun SupportSQLiteDatabase.upgradeTo177() {
        execSQL("CREATE TABLE account_types (_id integer primary key autoincrement, label text not null, isAsset boolean not null, supportsReconciliation boolean not null)")
        val accountTypes = insertDefaultAccountTypesForMigration()
        val cash = accountTypes[AccountType.CASH.name]!!
        val bank = accountTypes[AccountType.BANK.name]!!
        val ccard = accountTypes[AccountType.CCARD.name]!!
        val asset = accountTypes[AccountType.ASSET.name]!!
        val liability = accountTypes[AccountType.LIABILITY.name]!!
        val migrateTypeCaseStatement = "CASE type WHEN 'CASH' THEN $cash WHEN 'BANK' THEN $bank WHEN 'CCARD' THEN $ccard WHEN 'ASSET' THEN $asset WHEN 'LIABILITY' THEN $liability ELSE $cash END"


        execSQL("ALTER TABLE accounts RENAME to accounts_old")
        execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, opening_balance integer, description text, currency text not null  references currency(code), type integer references account_types(_id), color integer default -3355444, grouping text not null check (grouping in ('NONE','DAY','WEEK','MONTH','YEAR')) default 'NONE', usages integer default 0,last_used datetime, sort_key integer, sync_account_name text, sync_sequence_local integer default 0,exclude_from_totals boolean default 0, uuid text, sort_by text default 'date', sort_direction text not null check (sort_direction in ('ASC','DESC')) default 'DESC',criterion integer,hidden boolean default 0,sealed boolean default 0,dynamic boolean default 0,bank_id integer references banks(_id) ON DELETE SET NULL)")
        execSQL("""INSERT INTO accounts (_id, label, opening_balance, description, currency, type, color, grouping, usages, last_used, sort_key, sync_account_name, sync_sequence_local, exclude_from_totals, uuid, sort_by, sort_direction, criterion, hidden, sealed, dynamic, bank_id)
            SELECT _id, label, opening_balance, description, currency, $migrateTypeCaseStatement, color, grouping, usages, last_used, sort_key, sync_account_name, sync_sequence_local, exclude_from_totals, uuid, sort_by, sort_direction, criterion, hidden, sealed, dynamic, bank_id FROM accounts_old;
        """)
        execSQL("DROP TABLE accounts_old")
        execSQL("CREATE UNIQUE INDEX accounts_uuid ON accounts(uuid)")
        createOrRefreshAccountTriggers();
        createOrRefreshAccountMetadataTrigger();

        execSQL("ALTER TABLE accounttype_paymentmethod RENAME to accounttype_paymentmethod_old")
        execSQL("CREATE TABLE accounttype_paymentmethod (type integer references account_types(_id), method_id integer references paymentmethods(_id), primary key (type,method_id))")
        execSQL("""INSERT INTO accounttype_paymentmethod (type, method_id)
            SELECT $migrateTypeCaseStatement, method_id FROM accounttype_paymentmethod_old;
        """)
        execSQL("DROP TABLE accounttype_paymentmethod_old")
    }

    fun SupportSQLiteDatabase.upgradeTo178() {
        repairWithSealedAccountsAndDebts {
            execSQL("update transactions set payee_id = null where parent_id in (select _id from transactions where cat_id = 0 and status != 4)")
        }
    }

    fun SupportSQLiteDatabase.upgradeTo179() {
        execSQL(
            "CREATE TABLE account_flags (_id integer primary key autoincrement, flag_label text unique not null, flag_sort_key integer not null default 0, flag_icon text, visible boolean not null)"
        )
        val accountFlags = insertDefaultAccountFlagsForMigration()
        execSQL(DEFAULT_FLAG_TRIGGER)
        val inactive = accountFlags[PREDEFINED_NAME_INACTIVE]
        execSQL("ALTER TABLE accounts RENAME to accounts_old")
        execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, opening_balance integer, description text, currency text not null  references currency(code), type integer references account_types(_id), color integer default -3355444, grouping text not null check (grouping in ('NONE','DAY','WEEK','MONTH','YEAR')) default 'NONE', usages integer default 0,last_used datetime, sort_key integer, sync_account_name text, sync_sequence_local integer default 0,exclude_from_totals boolean default 0, uuid text, sort_by text default 'date', sort_direction text not null check (sort_direction in ('ASC','DESC')) default 'DESC',criterion integer,flag integer references account_flags(_id) NOT NULL default 0,sealed boolean default 0,dynamic boolean default 0,bank_id integer references banks(_id) ON DELETE SET NULL)")
        execSQL("""INSERT INTO accounts (_id, label, opening_balance, description, currency, type, color, grouping, usages, last_used, sort_key, sync_account_name, sync_sequence_local, exclude_from_totals, uuid, sort_by, sort_direction, criterion, flag, sealed, dynamic, bank_id)
            SELECT _id, label, opening_balance, description, currency, type, color, grouping, usages, last_used, sort_key, sync_account_name, sync_sequence_local, exclude_from_totals, uuid, sort_by, sort_direction, criterion, case when hidden then $inactive else 0 end, sealed, dynamic, bank_id FROM accounts_old;
        """)
        execSQL("DROP TABLE accounts_old")
        execSQL("CREATE UNIQUE INDEX accounts_uuid ON accounts(uuid)")
        createOrRefreshAccountTriggers();
        createOrRefreshAccountMetadataTrigger();
    }

    fun SupportSQLiteDatabase.upgradeTo180() {
        execSQL("ALTER TABLE account_types add column type_sort_key integer not null default 0")
        AccountType.initialAccountTypes.filter { it.sortKey != 0 }.forEach {
            execSQL("UPDATE account_types set type_sort_key = ? WHERE label = ?", arrayOf(it.sortKey.toString(), it.name))
        }
    }

    protected fun SupportSQLiteDatabase.createOrRefreshAccountTriggers() {
        execSQL("DROP TRIGGER IF EXISTS update_account_sync_null")
        execSQL("DROP TRIGGER IF EXISTS sort_key_default")
        execSQL(UPDATE_ACCOUNT_SYNC_NULL_TRIGGER)
        execSQL(ACCOUNTS_TRIGGER_CREATE)
        createOrRefreshAccountSealedTrigger()
    }

    protected fun SupportSQLiteDatabase.createOrRefreshAccountSealedTrigger() {
        execSQL("DROP TRIGGER IF EXISTS sealed_account_update")
        execSQL(ACCOUNTS_SEALED_TRIGGER_CREATE)
    }

    protected fun SupportSQLiteDatabase.createOrRefreshAccountMetadataTrigger() {
        execSQL("DROP TRIGGER IF EXISTS update_account_metadata")
        execSQL("DROP TRIGGER IF EXISTS update_account_exchange_rate")
        execSQL(UPDATE_ACCOUNT_METADATA_TRIGGER)
        execSQL(UPDATE_ACCOUNT_EXCHANGE_RATE_TRIGGER)
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

    private fun SupportSQLiteDatabase.migrateCurrency(
        oldCurrency: String,
        newCurrency: CurrencyEnum
    ) {
        if (query(
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
        } else if (delete("currency", "code = ?", arrayOf(oldCurrency)) == 1) {
            Timber.d("Currency %s deleted", oldCurrency)
        }
        //if new currency is already defined, error is logged
        if (insert("currency", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply {
                put("code", newCurrency.name)
            }) != -1L) {
            Timber.d("Currency %s inserted", newCurrency.name)
        }
    }

    fun SupportSQLiteDatabase.createOrRefreshTransactionDebtTriggers() {
        execSQL("DROP TRIGGER IF EXISTS sealed_debt_update")
        execSQL("DROP TRIGGER IF EXISTS sealed_debt_transaction_insert")
        execSQL("DROP TRIGGER IF EXISTS sealed_debt_transaction_update")
        execSQL("DROP TRIGGER IF EXISTS sealed_debt_transaction_delete")
        execSQL(DEBTS_SEALED_TRIGGER_CREATE)
        execSQL(TRANSACTIONS_SEALED_DEBT_INSERT_TRIGGER_CREATE)
        execSQL(TRANSACTIONS_SEALED_DEBT_UPDATE_TRIGGER_CREATE)
        execSQL(TRANSACTIONS_SEALED_DEBT_DELETE_TRIGGER_CREATE)
    }

    fun SupportSQLiteDatabase.repairWithSealedAccounts(run: Runnable) {
        execSQL("update accounts set sealed = -1 where sealed = 1")
        run.run()
        execSQL("update accounts set sealed = 1 where sealed = -1")
    }

    fun SupportSQLiteDatabase.repairWithSealedAccountsAndDebts(run: Runnable) {
        execSQL("update accounts set sealed = -1 where sealed = 1")
        execSQL("update debts set sealed = -1 where sealed = 1")
        run.run()
        execSQL("update accounts set sealed = 1 where sealed = -1")
        execSQL("update debts set sealed = 1 where sealed = -1")
    }

    fun SupportSQLiteDatabase.createOrRefreshCategoryHierarchyTrigger() {
        execSQL("DROP TRIGGER IF EXISTS category_hierarchy_update")
        execSQL(CATEGORY_HIERARCHY_TRIGGER)
    }

    fun SupportSQLiteDatabase.createOrRefreshCategoryMainCategoryUniqueLabel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && "robolectric" != Build.FINGERPRINT) {
            execSQL("DROP INDEX if exists categories_label")
            execSQL(CATEGORY_LABEL_INDEX_CREATE)
        } else {
            execSQL("DROP TRIGGER IF EXISTS category_label_unique_insert")
            execSQL("DROP TRIGGER IF EXISTS category_label_unique_update")
            execSQL(CATEGORY_LABEL_LEGACY_TRIGGER_INSERT)
            execSQL(CATEGORY_LABEL_LEGACY_TRIGGER_UPDATE)
        }
    }

    fun SupportSQLiteDatabase.createArchiveTriggers() {
        execSQL(TRANSACTIONS_ARCHIVE_TRIGGER)
        execSQL(TRANSACTIONS_UNARCHIVE_TRIGGER)
    }

    fun SupportSQLiteDatabase.createCategoryTypeTriggers() {
        execSQL(CATEGORY_TYPE_INSERT_TRIGGER)
        execSQL(CATEGORY_TYPE_UPDATE_TRIGGER_MAIN)
        execSQL(CATEGORY_TYPE_UPDATE_TRIGGER_SUB)
        execSQL(CATEGORY_TYPE_MOVE_TRIGGER)
    }

    fun insertFinTSAttributes(db: SupportSQLiteDatabase) {
        Attribute.initDatabase(db, FinTsAttribute::class.java)
        Attribute.initDatabase(db, BankingAttribute::class.java)
    }

    fun buildViewDefinitionExtended(tableName: String) = buildString {
        append(" AS ")
        if (tableName != TABLE_CHANGES) {
            append(getCategoryTreeForView())
        }
        if (tableName == TABLE_TRANSACTIONS) {
            fun associationCTE(
                cte: String,
                aggregateExpression: String,
                associationTable: String,
                associated: Pair<String, String>? = null
            ): String {
                return "$cte as (SELECT $KEY_TRANSACTIONID, $aggregateExpression FROM $associationTable " +
                        (associated?.let { "LEFT JOIN ${it.first} ON ${it.second} = ${it.first}.$KEY_ROWID" }
                            ?: "") +
                        " GROUP BY $KEY_TRANSACTIONID)"
            }
            append(",")
            append(
                associationCTE(
                    "cte_tags",
                    TAG_LIST_EXPRESSION,
                    TABLE_TRANSACTIONS_TAGS,
                    null
                )
            )
            append(",")
            append(
                associationCTE(
                    "cte_attachments",
                    "count($KEY_URI) AS $KEY_ATTACHMENT_COUNT",
                    TABLE_TRANSACTION_ATTACHMENTS,
                    TABLE_ATTACHMENTS to KEY_ATTACHMENT_ID
                )
            )
        }
        append(" SELECT $tableName.*, $TABLE_PAYEES.$KEY_SHORT_NAME, $TABLE_PAYEES.$KEY_PAYEE_NAME, ")
        append("$TABLE_METHODS.$KEY_LABEL AS $KEY_METHOD_LABEL, ")
        append("$TABLE_METHODS.$KEY_ICON AS $KEY_METHOD_ICON")
        if (tableName != TABLE_CHANGES) {
            append(", Tree.$KEY_PATH, Tree.$KEY_ICON, Tree.$KEY_TYPE, $KEY_COLOR, $KEY_CURRENCY, $KEY_SEALED, $KEY_EXCLUDE_FROM_TOTALS, $KEY_DYNAMIC")
            append(", $TABLE_ACCOUNTS.$KEY_TYPE AS $KEY_ACCOUNT_TYPE")
            append(", $TABLE_ACCOUNTS.$KEY_LABEL AS $KEY_ACCOUNT_LABEL")
        }
        if (tableName == TABLE_TRANSACTIONS) {
            append(", $TABLE_PLAN_INSTANCE_STATUS.$KEY_TEMPLATEID, ")
            append(KEY_TAGLIST)
            append(", $KEY_ATTACHMENT_COUNT, ")
            append(KEY_IBAN)
        }
        append(" FROM $tableName")
        append(" LEFT JOIN $TABLE_PAYEES ON $KEY_PAYEEID = $TABLE_PAYEES.$KEY_ROWID")
        append(" LEFT JOIN $TABLE_METHODS ON $KEY_METHODID = $TABLE_METHODS.$KEY_ROWID")
        if (tableName != TABLE_CHANGES) {
            append(" LEFT JOIN $TABLE_ACCOUNTS ON $KEY_ACCOUNTID = $TABLE_ACCOUNTS.$KEY_ROWID")
            append(" LEFT JOIN Tree ON $KEY_CATID = Tree.$KEY_ROWID")
        }
        if (tableName == TABLE_TRANSACTIONS) {
            append(" LEFT JOIN $TABLE_PLAN_INSTANCE_STATUS ON $tableName.$KEY_ROWID = $TABLE_PLAN_INSTANCE_STATUS.$KEY_TRANSACTIONID")
            append(" LEFT JOIN cte_tags ON cte_tags.$KEY_TRANSACTIONID = $tableName.$KEY_ROWID")
            append(" LEFT JOIN cte_attachments ON cte_attachments.$KEY_TRANSACTIONID = $tableName.$KEY_ROWID")
        }
    }

    fun insertDefaultTransferCategory(db: SupportSQLiteDatabase, label: String) {
        prefHandler.putLong(
            PrefKey.DEFAULT_TRANSFER_CATEGORY,
            db.insert(
                TABLE_CATEGORIES, SQLiteDatabase.CONFLICT_NONE,
                ContentValues(3).apply {
                    put(KEY_LABEL, label)
                    put(KEY_TYPE, 0)
                    put(KEY_COLOR, suggestNewCategoryColor(db))
                    put(KEY_UUID, DEFAULT_TRANSFER_CATEGORY_UUID)
                }
            )
        )
    }

    fun SupportSQLiteDatabase.insertNullRows() {
        //rows that allow us to record changes where payee or method gets set to null
        insert(TABLE_PAYEES, SQLiteDatabase.CONFLICT_NONE, ContentValues().apply {
            put(KEY_ROWID, NULL_ROW_ID)
            put(KEY_PAYEE_NAME, NULL_CHANGE_INDICATOR)
        })
        insert(TABLE_METHODS, SQLiteDatabase.CONFLICT_NONE, ContentValues().apply {
            put(KEY_ROWID, NULL_ROW_ID)
            put(KEY_LABEL, NULL_CHANGE_INDICATOR)
        })
    }

    fun SupportSQLiteDatabase.insertDefaultAccountTypes() =
        AccountType.initialAccountTypes.associate {
            it.name to
                    insert(
                        TABLE_ACCOUNT_TYPES,
                        SQLiteDatabase.CONFLICT_NONE,
                        it.asContentValues
                    )
        }

    fun SupportSQLiteDatabase.insertDefaultAccountTypesForMigration() =
        AccountType.initialAccountTypes.associate {
            it.name to
                    insert(
                        "account_types",
                        SQLiteDatabase.CONFLICT_NONE,
                        contentValuesOf(
                            KEY_LABEL to it.name,
                            KEY_IS_ASSET to it.isAsset,
                            KEY_SUPPORTS_RECONCILIATION to it.supportsReconciliation
                        )
                    )
        }

    fun SupportSQLiteDatabase.insertDefaultAccountFlags() =
        AccountFlag.initialFlags.associate {
            it.label to
                    insert(
                        TABLE_ACCOUNT_FLAGS,
                        SQLiteDatabase.CONFLICT_NONE,
                        it.asContentValues
                    )
        }

    fun SupportSQLiteDatabase.insertDefaultAccountFlagsForMigration() =
        AccountFlag.initialFlags.associate {
            it.label to
                    insert(
                        "account_flags",
                        SQLiteDatabase.CONFLICT_NONE,
                        contentValuesOf(
                            KEY_FLAG_LABEL to it.label,
                            KEY_FLAG_SORT_KEY to it.sortKey,
                            KEY_FLAG_ICON to it.icon,
                            KEY_VISIBLE to it.isVisible
                        ).apply {
                            if (it.label == "_DEFAULT_") {
                                put(KEY_ROWID, DEFAULT_FLAG_ID)
                            }
                        }
                    )
        }


    fun SupportSQLiteDatabase.insertDefaultAccountTypesAndMethods() {
        val accountTypes = insertDefaultAccountTypes()
        val bank = accountTypes[AccountType.BANK.name]!!
        for (pm in PreDefinedPaymentMethod.entries) {
            val id = insert(
                TABLE_METHODS, SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put(KEY_LABEL, pm.name)
                    put(KEY_TYPE, pm.paymentType)
                    put(DatabaseConstants.KEY_IS_NUMBERED, pm.isNumbered)
                    put(KEY_ICON, pm.icon)
                }
            )
            insert(
                DatabaseConstants.TABLE_ACCOUNTTYES_METHODS,
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put(KEY_METHODID, id)
                    put(KEY_TYPE, bank)
                }
            )
        }
    }
}

