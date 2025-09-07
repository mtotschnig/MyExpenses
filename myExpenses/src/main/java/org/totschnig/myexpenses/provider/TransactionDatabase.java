/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.totschnig.myexpenses.provider;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_NONE;
import static org.totschnig.myexpenses.model2.PaymentMethodKt.PAYMENT_METHOD_EXPENSE;
import static org.totschnig.myexpenses.model2.PaymentMethodKt.PAYMENT_METHOD_INCOME;
import static org.totschnig.myexpenses.model2.PaymentMethodKt.PAYMENT_METHOD_NEUTRAL;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.ACCOUNT_ATTRIBUTES_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.ACCOUNT_FLAG_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.ACCOUNT_TYPE_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.ATTACHMENTS_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.ATTRIBUTES_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.BANK_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.CATEGORY_TYPE_UPDATE_TRIGGER_MAIN;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.DEFAULT_FLAG_TRIGGER;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.EQUIVALENT_AMOUNTS_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.PARTY_HIERARCHY_TRIGGER;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.PAYEE_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.PAYEE_UNIQUE_INDEX;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.PRICES_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.SPLIT_PART_CR_STATUS_TRIGGER_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TAGS_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_ATTACHMENTS_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_CAT_ID_INDEX;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_PARENT_ID_INDEX;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_PAYEE_ID_INDEX;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_SEALED_DELETE_TRIGGER_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_SEALED_INSERT_TRIGGER_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTIONS_UUID_INDEX_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSACTION_ATTRIBUTES_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.TRANSFER_SEALED_UPDATE_TRIGGER_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.VIEW_WITH_ACCOUNT_DEFINITION;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.createOrRefreshTransactionLinkedTableTriggers;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.getPRIORITIZED_PRICES_CREATE;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.sequenceNumberSelect;
import static org.totschnig.myexpenses.provider.BaseTransactionDatabaseKt.shouldWriteChangeTemplate;
import static org.totschnig.myexpenses.provider.ChangeLogTriggersKt.createOrRefreshChangeLogTriggers;
import static org.totschnig.myexpenses.provider.ChangeLogTriggersKt.createOrRefreshEquivalentAmountTriggers;
import static org.totschnig.myexpenses.provider.DataBaseAccount.HOME_AGGREGATE_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;
import static org.totschnig.myexpenses.provider.DbConstantsKt.buildViewDefinition;
import static org.totschnig.myexpenses.provider.DbConstantsKt.tagGroupBy;
import static org.totschnig.myexpenses.util.ColorUtils.MAIN_COLORS;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.provider.CalendarContract.Events;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.File;
import java.util.Locale;

import timber.log.Timber;

public class TransactionDatabase extends BaseTransactionDatabase {
  protected boolean shouldInsertDefaultTransferCategory;

  /**
   * SQL statement for expenses TABLE
   * both transactions and transfers are stored in this table
   * for transfers there are two rows (one per account) which
   * are linked by KEY_TRANSFER_PEER
   * for normal transactions KEY_TRANSFER_PEER is set to NULL
   * split parts are linked with their parents through KEY_PARENTID
   * KEY_STATUS has STATUS_EXPORTED if transaction is exported, and
   * STATUS_UNCOMMITTED for transactions that are created during editing of splits
   * KEY_CR_STATUS stores cleared/reconciled
   */
  private static final String DATABASE_CREATE =
      "CREATE TABLE " + TABLE_TRANSACTIONS + "( "
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_COMMENT + " text, "
          + KEY_DATE + " datetime not null, "
          + KEY_VALUE_DATE + " datetime not null, "
          + KEY_AMOUNT + " integer not null, "
          + KEY_CATID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + "), "
          + KEY_ACCOUNTID + " integer not null references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE,"
          + KEY_PAYEEID + " integer references " + TABLE_PAYEES + "(" + KEY_ROWID + "), "
          + KEY_TRANSFER_PEER + " integer references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + "), "
          + KEY_TRANSFER_ACCOUNT + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + "),"
          + KEY_METHODID + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + "),"
          + KEY_PARENTID + " integer references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_STATUS + " integer default 0, "
          + KEY_CR_STATUS + " text not null check (" + KEY_CR_STATUS + " in (" + CrStatus.JOIN + ")) default '" + CrStatus.RECONCILED.name() + "',"
          + KEY_REFERENCE_NUMBER + " text, "
          + KEY_UUID + " text, "
          + KEY_ORIGINAL_AMOUNT + " integer, "
          + KEY_ORIGINAL_CURRENCY + " text, "
          + KEY_DEBT_ID + " integer references " + TABLE_DEBTS + "(" + KEY_ROWID + ") ON DELETE SET NULL);";

  public TransactionDatabase(@NonNull Context context, @NonNull PrefHandler prefHandler, boolean shouldInsertDefaultTransferCategory) {
    super(context, prefHandler);
    this.shouldInsertDefaultTransferCategory = shouldInsertDefaultTransferCategory;
  }

  /**
   * SQL statement for accounts TABLE
   */
  private static final String ACCOUNTS_CREATE =
      "CREATE TABLE " + TABLE_ACCOUNTS + " ("
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_LABEL + " text not null, "
          + KEY_OPENING_BALANCE + " integer, "
          + KEY_DESCRIPTION + " text, "
          + KEY_CURRENCY + " text not null  references " + TABLE_CURRENCIES + "(" + KEY_CODE + "), "
          + KEY_TYPE + " integer references " + TABLE_ACCOUNT_TYPES + "(" + KEY_ROWID + ") NOT NULL, "
          + KEY_COLOR + " integer default -3355444, "
          + KEY_GROUPING + " text not null check (" + KEY_GROUPING + " in (" + Grouping.JOIN + ")) default '" + Grouping.NONE.name() + "', "
          + KEY_USAGES + " integer default 0,"
          + KEY_LAST_USED + " datetime, "
          + KEY_SORT_KEY + " integer, "
          + KEY_SYNC_ACCOUNT_NAME + " text, "
          + KEY_SYNC_SEQUENCE_LOCAL + " integer default 0,"
          + KEY_EXCLUDE_FROM_TOTALS + " boolean default 0, "
          + KEY_UUID + " text, "
          + KEY_SORT_BY + " text default 'date', "
          + KEY_SORT_DIRECTION + " text not null check (" + KEY_SORT_DIRECTION + " in ('ASC','DESC')) default 'DESC',"
          + KEY_CRITERION + " integer,"
          + KEY_FLAG + " integer references " + TABLE_ACCOUNT_FLAGS + "(" + KEY_ROWID + ") NOT NULL default 0, "
          + KEY_SEALED + " boolean default 0,"
          + KEY_DYNAMIC + " boolean default 0,"
          + KEY_BANK_ID + " integer references " + TABLE_BANKS + "(" + KEY_ROWID + ") ON DELETE SET NULL);";

  private static final String SYNC_STATE_CREATE =
      "CREATE TABLE " + TABLE_SYNC_STATE + " ("
          + KEY_STATUS + " integer );";

  private static final String ACCOUNTS_UUID_INDEX_CREATE = "CREATE UNIQUE INDEX accounts_uuid ON "
      + TABLE_ACCOUNTS + "(" + KEY_UUID + ")";

  private static final String ACCOUNT_EXCHANGE_RATES_CREATE =
      "CREATE TABLE " + TABLE_ACCOUNT_EXCHANGE_RATES + " ("
          + KEY_ACCOUNTID + " integer not null references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE,"
          + KEY_CURRENCY_SELF + " text not null, "
          + KEY_CURRENCY_OTHER + " text not null, "
          + KEY_EXCHANGE_RATE + " real not null, "
          + "UNIQUE (" + KEY_ACCOUNTID + "," + KEY_CURRENCY_SELF + "," + KEY_CURRENCY_OTHER + "));";

  /**
   * SQL statement for categories TABLE
   * Table definition reflects format of Grisbis categories
   * Main categories have parent_id 0
   * usages counts how often the cat is selected
   */
  private static final String CATEGORIES_CREATE =
      "CREATE TABLE " + TABLE_CATEGORIES + " ("
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_LABEL + " text not null, "
          + KEY_LABEL_NORMALIZED + " text,"
          + KEY_PARENTID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_USAGES + " integer default 0, "
          + KEY_LAST_USED + " datetime, "
          + KEY_COLOR + " integer, "
          + KEY_ICON + " string, " //TODO migrate to text
          + KEY_UUID + " text, "
          + KEY_TYPE + " integer, "
          + "UNIQUE (" + KEY_LABEL + "," + KEY_PARENTID + "));";

  private static final String CATEGORY_UUID_INDEX_CREATE = "CREATE UNIQUE INDEX categories_uuid ON "
      + TABLE_CATEGORIES + "(" + KEY_UUID + ")";

  private static final String PAYMENT_METHODS_CREATE =
      "CREATE TABLE " + TABLE_METHODS + " ("
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_LABEL + " text not null, "
          + KEY_IS_NUMBERED + " boolean default 0, "
          + KEY_TYPE + " integer " +
          "check (" + KEY_TYPE + " in ("
          + PAYMENT_METHOD_EXPENSE + ","
          + PAYMENT_METHOD_NEUTRAL + ","
          + PAYMENT_METHOD_INCOME + ")) default 0, "
          + KEY_ICON + " text);";

  private static final String ACCOUNTTYE_METHOD_CREATE =
      "CREATE TABLE " + TABLE_ACCOUNTTYES_METHODS + " ("
          + KEY_TYPE + " integer references " + TABLE_ACCOUNT_TYPES + "(" + KEY_ROWID + "), "
          + KEY_METHODID + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + "), "
          + "primary key (" + KEY_TYPE + "," + KEY_METHODID + "));";

  /**
   * {@link DatabaseConstants#KEY_PLANID} references an event in com.android.providers.calendar
   */
  private static final String TEMPLATE_CREATE =
      "CREATE TABLE " + TABLE_TEMPLATES + " ( "
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_COMMENT + " text, "
          + KEY_AMOUNT + " integer not null, "
          + KEY_CATID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + "), "
          + KEY_ACCOUNTID + " integer not null references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE,"
          + KEY_PAYEEID + " integer references " + TABLE_PAYEES + "(" + KEY_ROWID + "), "
          + KEY_TRANSFER_ACCOUNT + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE,"
          + KEY_METHODID + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + "), "
          + KEY_TITLE + " text not null, "
          + KEY_USAGES + " integer default 0, "
          + KEY_PLANID + " integer, "
          + KEY_PLAN_EXECUTION + " boolean default 0, "
          + KEY_UUID + " text, "
          + KEY_LAST_USED + " datetime, "
          + KEY_PARENTID + " integer references " + TABLE_TEMPLATES + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_STATUS + " integer default 0, "
          + KEY_PLAN_EXECUTION_ADVANCE + " integer default 0, "
          + KEY_DEFAULT_ACTION + " text not null check (" + KEY_DEFAULT_ACTION + " in (" + Template.Action.JOIN + ")) default '" + Template.Action.SAVE.name() + "', "
          + KEY_ORIGINAL_AMOUNT + " integer, "
          + KEY_ORIGINAL_CURRENCY + " text, "
          + KEY_DEBT_ID + " integer references " + TABLE_DEBTS + "(" + KEY_ROWID + ") ON DELETE SET NULL);";

  private static final String EVENT_CACHE_CREATE =
      "CREATE TABLE " + TABLE_EVENT_CACHE + " ( " +
          Events.TITLE + " TEXT," +
          Events.DESCRIPTION + " TEXT," +
          Events.DTSTART + " INTEGER," +
          Events.DTEND + " INTEGER," +
          Events.EVENT_TIMEZONE + " TEXT," +
          Events.DURATION + " TEXT," +
          Events.ALL_DAY + " INTEGER NOT NULL DEFAULT 0," +
          Events.RRULE + " TEXT," +
          Events.CUSTOM_APP_PACKAGE + " TEXT," +
          Events.CUSTOM_APP_URI + " TEXT);";

  private static final String DEBT_CREATE =
      "CREATE TABLE " + TABLE_DEBTS
          + " (" + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_PAYEEID + " integer references " + TABLE_PAYEES + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_DATE + " datetime not null, "
          + KEY_LABEL + " text not null, "
          + KEY_AMOUNT + " integer, "
          + KEY_EQUIVALENT_AMOUNT + " integer,  "
          + KEY_CURRENCY + " text not null, "
          + KEY_DESCRIPTION + " text, "
          + KEY_SEALED + " boolean default 0);";

  private static final String CURRENCY_CREATE =
      "CREATE TABLE " + TABLE_CURRENCIES
          + " (" + KEY_ROWID + " integer primary key autoincrement, " +
          KEY_CODE + " text UNIQUE not null," +
          KEY_GROUPING + " text not null check (" + KEY_GROUPING + " in (" + Grouping.JOIN + ")) default '" + Grouping.NONE.name() + "'," +
          KEY_SORT_BY + " text default 'date', " +
          KEY_SORT_DIRECTION + " text not null check (" + KEY_SORT_DIRECTION + " in ('ASC','DESC')) default 'DESC'," +
          KEY_LABEL + " text);";

  /**
   * in this table we store links between plan instances and transactions,
   * thus allowing us to track if an instance has been applied, and to allow editing or cancellation of
   * transactions added from plan instances
   */
  private static final String PLAN_INSTANCE_STATUS_CREATE =
      "CREATE TABLE " + TABLE_PLAN_INSTANCE_STATUS
          + " ( " + KEY_TEMPLATEID + " integer references " + TABLE_TEMPLATES + "(" + KEY_ROWID + ") ON DELETE CASCADE," +
          KEY_INSTANCEID + " integer," + // NO LONGER references Instances._ID in calendar content provider; instanceId is calculated from day
          KEY_TRANSACTIONID + " integer UNIQUE references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + ") ON DELETE CASCADE, " +
          "primary key (" + KEY_TEMPLATEID + "," + KEY_INSTANCEID + "));";

  private static final String CHANGES_CREATE =
      "CREATE TABLE " + TABLE_CHANGES
          + " ( " + KEY_ACCOUNTID + " integer not null references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE,"
          + KEY_TYPE + " text not null check (" + KEY_TYPE + " in (" + TransactionChange.Type.JOIN + ")), "
          + KEY_SYNC_SEQUENCE_LOCAL + " integer, "
          + KEY_UUID + " text not null, "
          + KEY_TIMESTAMP + " datetime DEFAULT (strftime('%s','now')), "
          + KEY_PARENT_UUID + " text, "
          + KEY_COMMENT + " text, "
          + KEY_DATE + " datetime, "
          + KEY_VALUE_DATE + " datetime, "
          + KEY_AMOUNT + " integer, "
          + KEY_ORIGINAL_AMOUNT + " integer, "
          + KEY_ORIGINAL_CURRENCY + " text, "
          + KEY_EQUIVALENT_AMOUNT + " integer, "
          + KEY_CATID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + ") ON DELETE SET NULL, "
          + KEY_PAYEEID + " integer references " + TABLE_PAYEES + "(" + KEY_ROWID + ") ON DELETE SET NULL, "
          + KEY_TRANSFER_ACCOUNT + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE SET NULL,"
          + KEY_METHODID + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + ") ON DELETE SET NULL,"
          + KEY_CR_STATUS + " text check (" + KEY_CR_STATUS + " in (" + CrStatus.JOIN + ")),"
          + KEY_STATUS + " integer default 0, "
          + KEY_REFERENCE_NUMBER + " text);";

  private static final String BUDGETS_CREATE =
      "CREATE TABLE " + TABLE_BUDGETS + " ( "
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_TITLE + " text not null default '', "
          + KEY_DESCRIPTION + " text not null, "
          + KEY_GROUPING + " text not null check (" + KEY_GROUPING + " in (" + Grouping.JOIN + ")), "
          + KEY_ACCOUNTID + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_CURRENCY + " text, "
          + KEY_START + " datetime, "
          + KEY_END + " datetime, "
          + KEY_IS_DEFAULT + " boolean default 0, "
          + KEY_UUID + " text)";

  private static final String BUDGETS_CATEGORY_CREATE =
      "CREATE TABLE " + TABLE_BUDGET_ALLOCATIONS + " ( "
          + KEY_BUDGETID + " integer not null references " + TABLE_BUDGETS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_CATID + " integer not null references " + TABLE_CATEGORIES + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_YEAR + " integer, "
          + KEY_SECOND_GROUP + " integer, "
          + KEY_BUDGET + " integer, "
          + KEY_BUDGET_ROLLOVER_PREVIOUS + " integer, "
          + KEY_BUDGET_ROLLOVER_NEXT + " integer, "
          + KEY_ONE_TIME + " boolean default 0, "
          + "primary key (" + KEY_BUDGETID + "," + KEY_CATID + "," + KEY_YEAR + "," + KEY_SECOND_GROUP + "));";


  private static final String SETTINGS_CREATE =
      "CREATE TABLE " + TABLE_SETTINGS + " ("
          + KEY_KEY + " text unique not null, "
          + KEY_VALUE + " text);";

  private static final String TRANSACTIONS_TAGS_CREATE =
      "CREATE TABLE " + TABLE_TRANSACTIONS_TAGS
          + " ( " + KEY_TAGID + " integer references " + TABLE_TAGS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_TRANSACTIONID + " integer references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + "primary key (" + KEY_TAGID + "," + KEY_TRANSACTIONID + "));";

  private static final String ACCOUNT_TAGS_CREATE =
      "CREATE TABLE " + TABLE_ACCOUNTS_TAGS
          + " ( " + KEY_TAGID + " integer references " + TABLE_TAGS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_ACCOUNTID + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + "primary key (" + KEY_TAGID + "," + KEY_ACCOUNTID + "));";

  private static final String INSERT_TRANSFER_TAGS_TRIGGER =
      String.format(Locale.ROOT, "CREATE TRIGGER insert_transfer_tags AFTER INSERT ON %1$s "
              + "WHEN %2$s IS NOT NULL "
              + "BEGIN INSERT INTO %1$s (%3$s, %4$s) VALUES (%2$s, new.%4$s); END",
          TABLE_TRANSACTIONS_TAGS, SELECT_TRANSFER_PEER("new"), KEY_TRANSACTIONID, KEY_TAGID);

  private static final String DELETE_TRANSFER_TAGS_TRIGGER =
      String.format(Locale.ROOT, "CREATE TRIGGER delete_transfer_tags AFTER DELETE ON %1$s "
              + "WHEN %2$s IS NOT NULL "
              + "BEGIN DELETE FROM %1$s WHERE %3$s = %2$s; END",
          TABLE_TRANSACTIONS_TAGS, SELECT_TRANSFER_PEER("old"), KEY_TRANSACTIONID);

  private static String SELECT_TRANSFER_PEER(String reference) {
    return String.format(Locale.ROOT, "(SELECT %1$s FROM %2$s WHERE %3$s = %4$s.%5$s)", KEY_TRANSFER_PEER, TABLE_TRANSACTIONS, KEY_ROWID, reference, KEY_TRANSACTIONID);
  }

  private static final String TEMPLATES_TAGS_CREATE =
      "CREATE TABLE " + TABLE_TEMPLATES_TAGS
          + " ( " + KEY_TAGID + " integer references " + TABLE_TAGS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_TEMPLATEID + " integer references " + TABLE_TEMPLATES + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + "primary key (" + KEY_TAGID + "," + KEY_TEMPLATEID + "));";

  @Override
  public void onConfigure(@NonNull SupportSQLiteDatabase db) {
    super.onConfigure(db);
    if (!db.isReadOnly()) {
      db.execSQL("PRAGMA legacy_alter_table=ON;");
    }
  }

  @Override
  public void onCorruption(@NonNull SupportSQLiteDatabase db) {
    throw new RuntimeException("Database corrupted");
  }

  @Override
  public void onOpen(@NonNull SupportSQLiteDatabase db) {
    super.onOpen(db);
    //since API 16 we could use onConfigure to enable foreign keys
    //which is run before onUpgrade
    //but this makes upgrades more difficult, since then you have to maintain the constraint in
    //each step of a multi statement upgrade with table rename
    //we stick to doing upgrades with foreign keys disabled which forces us
    //to take care of ensuring consistency during upgrades
    if (!db.isReadOnly()) {
      db.execSQL("PRAGMA foreign_keys=ON;");
      db.execSQL("PRAGMA recursive_triggers = ON;");
    }
    try {
      String uncommitedSelect = String.format(Locale.ROOT, "(SELECT %s from %s where %s = %d)",
          KEY_ROWID, TABLE_TRANSACTIONS, KEY_STATUS, STATUS_UNCOMMITTED);
      String uncommitedParentSelect = String.format(Locale.ROOT, "%s IN %s", KEY_PARENTID, uncommitedSelect);
      final String whereClause = String.format(Locale.ROOT,
          "%1$s IN %2$s OR %3$s OR %4$s IN (SELECT %5$s FROM %6$s WHERE %3$s)",
          KEY_ROWID, uncommitedSelect, uncommitedParentSelect, KEY_TRANSFER_PEER, KEY_ROWID, TABLE_TRANSACTIONS);
      Timber.d(whereClause);
      MoreDbUtilsKt.safeUpdateWithSealed(db, () -> db.delete(TABLE_TRANSACTIONS, whereClause, null));
    } catch (SQLiteException e) {
      CrashHandler.report(e);
    }
  }

  @Override
  public void onCreate(SupportSQLiteDatabase db) {
    db.execSQL(DATABASE_CREATE);
    db.execSQL(SPLIT_PART_CR_STATUS_TRIGGER_CREATE);
    db.execSQL(EQUIVALENT_AMOUNTS_CREATE);
    db.execSQL(ATTACHMENTS_CREATE);
    db.execSQL(TRANSACTIONS_ATTACHMENTS_CREATE);
    db.execSQL(TRANSACTIONS_UUID_INDEX_CREATE);
    db.execSQL(PAYEE_CREATE);
    db.execSQL(PAYEE_UNIQUE_INDEX);
    db.execSQL(PAYMENT_METHODS_CREATE);
    db.execSQL(TEMPLATE_CREATE);
    db.execSQL(PLAN_INSTANCE_STATUS_CREATE);
    db.execSQL(CATEGORIES_CREATE);
    db.execSQL(CATEGORY_UUID_INDEX_CREATE);
    createOrRefreshCategoryMainCategoryUniqueLabel(db);
    db.execSQL(ACCOUNTS_CREATE);
    db.execSQL(ACCOUNTS_UUID_INDEX_CREATE);
    db.execSQL(SYNC_STATE_CREATE);
    db.execSQL(ACCOUNT_TYPE_CREATE);
    db.execSQL(ACCOUNT_FLAG_CREATE);
    db.execSQL(ACCOUNTTYE_METHOD_CREATE);
    insertDefaultAccountTypesAndMethods(db);
    insertDefaultAccountFlags(db);
    db.execSQL(DEFAULT_FLAG_TRIGGER);
    db.execSQL(CURRENCY_CREATE);
    //category for splits needed to honour foreign constraint
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_ROWID, SPLIT_CATID);
    initialValues.put(KEY_PARENTID, SPLIT_CATID);
    initialValues.put(KEY_LABEL, "__SPLIT_TRANSACTION__");
    db.insert(TABLE_CATEGORIES, CONFLICT_NONE, initialValues);
    if (shouldInsertDefaultTransferCategory) {
      insertDefaultTransferCategory(db, getContext().getString(R.string.transfer));
    }
    insertCurrencies(db);
    db.execSQL(EVENT_CACHE_CREATE);
    db.execSQL(CHANGES_CREATE);
    db.execSQL(BANK_CREATE);
    db.execSQL(ATTRIBUTES_CREATE);
    insertFinTSAttributes(db);
    db.execSQL(TRANSACTION_ATTRIBUTES_CREATE);
    db.execSQL(ACCOUNT_ATTRIBUTES_CREATE);

    //Index
    db.execSQL(TRANSACTIONS_CAT_ID_INDEX);
    db.execSQL("CREATE INDEX templates_cat_id_index on " + TABLE_TEMPLATES + "(" + KEY_CATID + ")");
    db.execSQL(TRANSACTIONS_PAYEE_ID_INDEX);
    db.execSQL("CREATE INDEX templates_payee_id_index on " + TABLE_TEMPLATES + "(" + KEY_PAYEEID + ")");
    db.execSQL(TRANSACTIONS_PARENT_ID_INDEX);

    db.execSQL(TAGS_CREATE);
    db.execSQL(TRANSACTIONS_TAGS_CREATE);
    db.execSQL(ACCOUNT_TAGS_CREATE);
    createOrRefreshTransferTagsTriggers(db);
    db.execSQL(TEMPLATES_TAGS_CREATE);

    // Triggers
    createOrRefreshTransactionTriggers(db);
    createOrRefreshTransactionLinkedTableTriggers(db);
    createOrRefreshTransactionUsageTriggers(db);
    createOrRefreshAccountTriggers(db);
    createCategoryTypeTriggers(db);
    createOrRefreshEquivalentAmountTriggers(db);

    db.execSQL(SETTINGS_CREATE);
    //TODO evaluate if we should get rid of the split transaction category id
    db.execSQL("CREATE TRIGGER protect_split_transaction" +
        "   BEFORE DELETE" +
        "   ON " + TABLE_CATEGORIES +
        "   WHEN (OLD." + KEY_ROWID + " = " + SPLIT_CATID + ")" +
        "   BEGIN" +
        "   SELECT RAISE (FAIL, 'split category can not be deleted'); " +
        "   END;");
    db.execSQL(ACCOUNT_EXCHANGE_RATES_CREATE);
    createOrRefreshAccountMetadataTrigger(db);
    db.execSQL(BUDGETS_CREATE);
    db.execSQL(BUDGETS_CATEGORY_CREATE);
    db.execSQL("CREATE INDEX budget_allocations_cat_id_index on " + TABLE_BUDGET_ALLOCATIONS + "(" + KEY_CATID + ")");

    db.execSQL(DEBT_CREATE);
    createOrRefreshTransactionDebtTriggers(db);

    db.execSQL(ACCOUNT_REMAP_TRANSFER_TRIGGER_CREATE);

    createOrRefreshCategoryHierarchyTrigger(db);
    createArchiveTriggers(db);

    db.execSQL(PARTY_HIERARCHY_TRIGGER);

    createOrRefreshViews(db);
    //insertTestData(db, 50, 50);

    insertNullRows(db);

    db.execSQL(PRICES_CREATE);
    db.execSQL(getPRIORITIZED_PRICES_CREATE());
    super.onCreate(db);
  }

  public void createOrRefreshTransferTagsTriggers(SupportSQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS insert_transfer_tags");
    db.execSQL("DROP TRIGGER IF EXISTS delete_transfer_tags");
    db.execSQL(INSERT_TRANSFER_TAGS_TRIGGER);
    db.execSQL(DELETE_TRANSFER_TAGS_TRIGGER);
  }

/*  private void insertTestData(SupportSQLiteDatabase db, int countGroup, int countChild) {
    int categories = MoreDbUtilsKt.setupDefaultCategories(db, MyApplication.Companion.getInstance().getResources()).getFirst();
    for (int i = 1; i <= countGroup; i++) {
      LocalDateTime date = LocalDateTime.now().plusDays(25);
      AccountInfo testAccount = new AccountInfo("Test account " + i, AccountType.BANK, 0);
      long testAccountId = db.insert(DatabaseConstants.TABLE_ACCOUNTS, CONFLICT_NONE, testAccount.getContentValues());
      for (int j = 1; j <= countChild; j++) {
        long catId = j % categories;
        long payeeId = db.insert(DatabaseConstants.TABLE_PAYEES, CONFLICT_NONE, new PayeeInfo("Payee " + i + "_" + j).getContentValues());
        date = date.minusDays(1);
        TransactionInfo transactionInfo = new TransactionInfo(testAccountId, 0, date, "Transaction " + j, payeeId, null, catId, null, null, CrStatus.UNRECONCILED);
        db.insert(
            DatabaseConstants.TABLE_TRANSACTIONS,
            CONFLICT_NONE,
            transactionInfo.getContentValues()
        );
      }
    }
  }*/

  private void insertCurrencies(SupportSQLiteDatabase db) {
    ContentValues initialValues = new ContentValues();
    for (CurrencyEnum currency : CurrencyEnum.values()) {
      initialValues.put(KEY_CODE, currency.name());
      db.insert(TABLE_CURRENCIES, CONFLICT_NONE, initialValues);
    }
  }

  /*
   * in onUpgrade, we can not rely on the constants, since we need the statements to be executed as defined
   * as is
   * if we would use the constants, and they change in the future, we would no longer have the same upgrade
   * and this can lead to bugs, if a later upgrade relies on column names as defined earlier,
   * and a user upgrading several versions at once would get a broken upgrade process
   */
  @Override
  public void onUpgrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {
    try {
      Timber.i("Upgrading database from version %d to %d", oldVersion, newVersion);
      if (oldVersion < 17) {
        db.execSQL("drop table accounts");
        db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, " +
            "opening_balance integer, description text, currency text not null);");
        //db.execSQL("ALTER TABLE expenses add column account_id integer");
      }

      if (oldVersion < 18) {
        db.execSQL("CREATE TABLE payee (_id integer primary key autoincrement, name text unique not null);");
        db.execSQL("ALTER TABLE expenses add column payee text");
      }

      if (oldVersion < 19) {
        db.execSQL("ALTER TABLE expenses add column transfer_peer text");
      }

      if (oldVersion < 20) {
        db.execSQL("CREATE TABLE transactions ( _id integer primary key autoincrement, comment text not null, "
            + "date datetime not null, amount integer not null, cat_id integer, account_id integer, "
            + "payee  text, transfer_peer integer default null);");
        db.execSQL("INSERT INTO transactions (comment,date,amount,cat_id,account_id,payee,transfer_peer)" +
            " SELECT comment,date,CAST(ROUND(amount*100) AS INTEGER),cat_id,account_id,payee,transfer_peer FROM expenses");
        db.execSQL("DROP TABLE expenses");
        db.execSQL("ALTER TABLE accounts RENAME to accounts_old");
        db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, " +
            "opening_balance integer, description text, currency text not null);");
        db.execSQL("INSERT INTO accounts (label,opening_balance,description,currency)" +
            " SELECT label,CAST(ROUND(opening_balance*100) AS INTEGER),description,currency FROM accounts_old");
        db.execSQL("DROP TABLE accounts_old");
      }

      if (oldVersion < 21) {
        db.execSQL("CREATE TABLE paymentmethods (_id integer primary key autoincrement, label text not null, type integer default 0);");
        db.execSQL("CREATE TABLE accounttype_paymentmethod (type text, method_id integer, primary key (type,method_id));");
        ContentValues initialValues;
        long _id;
        for (PreDefinedPaymentMethod pm : PreDefinedPaymentMethod.values()) {
          initialValues = new ContentValues();
          initialValues.put("label", pm.name());
          initialValues.put("type", pm.getPaymentType());
          _id = db.insert("paymentmethods", CONFLICT_NONE, initialValues);
          initialValues = new ContentValues();
          initialValues.put("method_id", _id);
          initialValues.put("type", "BANK");
          db.insert("accounttype_paymentmethod", CONFLICT_NONE, initialValues);
        }
        db.execSQL("ALTER TABLE transactions add column payment_method_id integer");
        db.execSQL("ALTER TABLE accounts add column type text default 'CASH'");
      }

      if (oldVersion < 22) {
        db.execSQL("CREATE TABLE templates ( _id integer primary key autoincrement, comment text not null, "
            + "amount integer not null, cat_id integer, account_id integer, payee text, transfer_peer integer default null, "
            + "payment_method_id integer, title text not null);");
      }

      if (oldVersion < 23) {
        db.execSQL("ALTER TABLE templates RENAME to templates_old");
        db.execSQL("CREATE TABLE templates ( _id integer primary key autoincrement, comment text not null, "
            + "amount integer not null, cat_id integer, account_id integer, payee text, transfer_peer integer default null, "
            + "payment_method_id integer, title text not null, unique(account_id, title));");
        try {
          db.execSQL("INSERT INTO templates(comment,amount,cat_id,account_id,payee,transfer_peer,payment_method_id,title)" +
              " SELECT comment,amount,cat_id,account_id,payee,transfer_peer,payment_method_id,title FROM templates_old");
        } catch (SQLiteConstraintException e) {
          Timber.w(e);
          //theoretically we could have entered duplicate titles for one account
          //we silently give up in that case (since this concerns only a narrowly distributed alpha version)
        }
        db.execSQL("DROP TABLE templates_old");
      }

      if (oldVersion < 24) {
        db.execSQL("ALTER TABLE templates add column usages integer default 0");
      }

      if (oldVersion < 25) {
        //for transactions that were not transfers, transfer_peer was set to null in transactions, but to 0 in templates
        db.execSQL("update transactions set transfer_peer=0 WHERE transfer_peer is null;");
      }

      if (oldVersion < 26) {
        db.execSQL("alter table accounts add column color integer default -6697984");
      }

      if (oldVersion < 27) {
        db.execSQL("CREATE TABLE feature_used (feature text not null);");
      }

      if (oldVersion < 28) {
        db.execSQL("ALTER TABLE transactions RENAME to transactions_old");
        db.execSQL("CREATE TABLE transactions(_id integer primary key autoincrement, comment text, date datetime not null, amount integer not null, " +
            "cat_id integer references categories(_id), account_id integer not null references accounts(_id),payee text, " +
            "transfer_peer integer references transactions(_id), transfer_account integer references accounts(_id), " +
            "method_id integer references paymentmethods(_id));");
        db.execSQL("INSERT INTO transactions (_id,comment,date,amount,cat_id,account_id,payee,transfer_peer,transfer_account,method_id) " +
            "SELECT _id,comment,date,amount, " +
            "CASE WHEN transfer_peer THEN null ELSE CASE WHEN cat_id THEN cat_id ELSE null END END, " +
            "account_id,payee, " +
            "CASE WHEN transfer_peer THEN transfer_peer ELSE null END, " +
            "CASE WHEN transfer_peer THEN cat_id ELSE null END, " +
            "CASE WHEN payment_method_id THEN payment_method_id ELSE null END " +
            "FROM transactions_old");
        db.execSQL("ALTER TABLE accounts RENAME to accounts_old");
        db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, opening_balance integer, description text, " +
            "currency text not null, type text not null check (type in ('CASH','BANK','CCARD','ASSET','LIABILITY')) default 'CASH', color integer default -3355444);");
        db.execSQL("INSERT INTO accounts (_id,label,opening_balance,description,currency,type,color) " +
            "SELECT _id,label,opening_balance,description,currency,type,color FROM accounts_old");
        //previously templates where not deleted if referred to accounts were deleted
        db.execSQL("DELETE FROM templates where account_id not in (SELECT _id FROM accounts) or (cat_id != 0 and transfer_peer = 1 and cat_id not in (SELECT _id from accounts))");
        db.execSQL("ALTER TABLE templates RENAME to templates_old");
        db.execSQL("CREATE TABLE templates ( _id integer primary key autoincrement, comment text not null, amount integer not null, " +
            "cat_id integer references categories(_id), account_id integer not null references accounts(_id),payee text, " +
            "transfer_peer boolean default 0, transfer_account integer references accounts(_id),method_id integer references paymentmethods(_id), " +
            "title text not null, usages integer default 0, unique(account_id,title));");
        db.execSQL("INSERT INTO templates (_id,comment,amount,cat_id,account_id,payee,transfer_peer,transfer_account,method_id,title,usages) " +
            "SELECT _id,comment,amount," +
            "CASE WHEN transfer_peer THEN null ELSE CASE WHEN cat_id THEN cat_id ELSE null END END, " +
            "account_id,payee, " +
            "CASE WHEN transfer_peer THEN 1 ELSE 0 END, " +
            "CASE WHEN transfer_peer THEN cat_id ELSE null END, " +
            "CASE WHEN payment_method_id THEN payment_method_id ELSE null END, " +
            "title,usages FROM templates_old");
        db.execSQL("ALTER TABLE categories RENAME to categories_old");
        db.execSQL("CREATE TABLE categories (_id integer primary key autoincrement, label text not null, parent_id integer references categories(_id), " +
            "usages integer default 0, unique (label,parent_id));");
        db.execSQL("INSERT INTO categories (_id,label,parent_id,usages) " +
            "SELECT _id,label,CASE WHEN parent_id THEN parent_id ELSE null END,usages FROM categories_old");
        db.execSQL("ALTER TABLE paymentmethods RENAME to paymentmethods_old");
        db.execSQL("CREATE TABLE paymentmethods (_id integer primary key autoincrement, label text not null, type integer check (type in (-1,0,1)) default 0);");
        db.execSQL("INSERT INTO paymentmethods (_id,label,type) SELECT _id,label,type FROM paymentmethods_old");
        db.execSQL("ALTER TABLE accounttype_paymentmethod RENAME to accounttype_paymentmethod_old");
        db.execSQL("CREATE TABLE accounttype_paymentmethod (type text not null check (type in ('CASH','BANK','CCARD','ASSET','LIABILITY')), method_id integer references paymentmethods (_id), primary key (type,method_id));");
        db.execSQL("INSERT INTO accounttype_paymentmethod (type,method_id) SELECT type,method_id FROM accounttype_paymentmethod_old");
        db.execSQL("DROP TABLE transactions_old");
        db.execSQL("DROP TABLE accounts_old");
        db.execSQL("DROP TABLE templates_old");
        db.execSQL("DROP TABLE categories_old");
        db.execSQL("DROP TABLE paymentmethods_old");
        db.execSQL("DROP TABLE accounttype_paymentmethod_old");
        //Changes to handle
        //1) Transfer account no longer stored as cat_id but in transfer_account (in transactions and templates)
        //2) parent_id for categories uses foreign key on itself, hence root categories have null instead of 0 as parent_id
        //3) catId etc now need to be null instead of 0
        //4) transactions payment_method_id renamed to method_id
      }

      if (oldVersion < 29) {
        db.execSQL("ALTER TABLE transactions add column status integer default 0");
      }

      if (oldVersion < 30) {
        db.execSQL("ALTER TABLE transactions add column parent_id integer references transactions (_id)");
        //      db.execSQL("CREATE VIEW committed AS SELECT * FROM transactions WHERE status != 2;");
        //      db.execSQL("CREATE VIEW uncommitted AS SELECT * FROM transactions WHERE status = 2;");
        ContentValues initialValues = new ContentValues();
        initialValues.put("_id", 0);
        initialValues.put("parent_id", 0);
        initialValues.put("label", "__SPLIT_TRANSACTION__");
        db.insert("categories", CONFLICT_NONE, initialValues);
      }

      if (oldVersion < 31) {
        //in an alpha version distributed on Google Play, we had SPLIT_CATID as -1
        ContentValues initialValues = new ContentValues();
        initialValues.put("_id", 0);
        initialValues.put("parent_id", 0);
        db.update("categories", CONFLICT_NONE, initialValues, "_id=-1", null);
      }

      if (oldVersion < 32) {
        db.execSQL("ALTER TABLE accounts add column grouping text not null check (grouping in " +
            "('NONE','DAY','WEEK','MONTH','YEAR')) default 'NONE'");
      }

      if (oldVersion < 33) {
        db.execSQL("ALTER TABLE accounts add column usages integer default 0");
        db.execSQL("UPDATE accounts SET usages = (SELECT count(*) FROM transactions WHERE account_id = accounts._id AND parent_id IS null)");
      }

      if (oldVersion < 34) {
        //fix for https://github.com/mtotschnig/MyExpenses/issues/69
        db.execSQL("UPDATE transactions set date = (SELECT date from transactions parent WHERE parent._id = transactions.parent_id) WHERE parent_id IS NOT null");
      }

      if (oldVersion < 35) {
        db.execSQL("ALTER TABLE transactions add column cr_status text not null check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED')) default 'UNRECONCILED'");
      }

      if (oldVersion < 36) {
        //move payee field in transactions from text to foreign key
        db.execSQL("ALTER TABLE transactions RENAME to transactions_old");
        db.execSQL("CREATE TABLE transactions (" +
            " _id integer primary key autoincrement," +
            " comment text, date datetime not null," +
            " amount integer not null," +
            " cat_id integer references categories(_id)," +
            " account_id integer not null references accounts(_id)," +
            " payee_id integer references payee(_id)," +
            " transfer_peer integer references transactions(_id)," +
            " transfer_account integer references accounts(_id)," +
            " method_id integer references paymentmethods(_id)," +
            " parent_id integer references transactions(_id)," +
            " status integer default 0," +
            " cr_status text not null check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED')) default 'RECONCILED')");
        //insert all payees that are stored in transactions, but are not in payee
        db.execSQL("INSERT INTO payee (name) SELECT DISTINCT payee FROM transactions_old WHERE payee != '' AND NOT exists (SELECT 1 FROM payee WHERE name=transactions_old.payee)");
        db.execSQL("INSERT INTO transactions " +
            "(_id,comment,date,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status) " +
            "SELECT " +
            "_id, " +
            "comment, " +
            "date, " +
            "amount, " +
            "cat_id, " +
            "account_id, " +
            "(SELECT _id from payee WHERE name = payee), " +
            "transfer_peer, " +
            "transfer_account, " +
            "method_id," +
            "parent_id," +
            "status," +
            "cr_status " +
            "FROM transactions_old");
        db.execSQL("DROP TABLE transactions_old");

        //move payee field in templates from text to foreign key
        db.execSQL("ALTER TABLE templates RENAME to templates_old");
        db.execSQL("CREATE TABLE templates (" +
            " _id integer primary key autoincrement," +
            " comment text," +
            " amount integer not null," +
            " cat_id integer references categories(_id)," +
            " account_id integer not null references accounts(_id)," +
            " payee_id integer references payee(_id)," +
            " transfer_peer boolean default 0," +
            " transfer_account integer references accounts(_id)," +
            " method_id integer references paymentmethods(_id)," +
            " title text not null," +
            " usages integer default 0," +
            " unique(account_id,title));");
        //insert all payees that are stored in templates, but are not in payee
        db.execSQL("INSERT INTO payee (name) SELECT DISTINCT payee FROM templates_old WHERE payee != '' AND NOT exists (SELECT 1 FROM payee WHERE name=templates_old.payee)");
        db.execSQL("INSERT INTO templates " +
            "(_id,comment,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,title,usages) " +
            "SELECT " +
            "_id, " +
            "comment, " +
            "amount, " +
            "cat_id, " +
            "account_id, " +
            "(SELECT _id from payee WHERE name = payee), " +
            "transfer_peer, " +
            "transfer_account, " +
            "method_id," +
            "title," +
            "usages " +
            "FROM templates_old");
        db.execSQL("DROP TABLE templates_old");

        db.execSQL("DROP VIEW IF EXISTS committed");
        db.execSQL("DROP VIEW IF EXISTS uncommitted");
        //for the definition of the view, it is safe to rely on the constants,
        //since we will not alter the view, but drop it, and recreate it, if needed
        //      String viewTransactions = VIEW_DEFINITION(TABLE_TRANSACTIONS);
        //      db.execSQL("CREATE VIEW transactions_committed "  + viewTransactions + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
        //      db.execSQL("CREATE VIEW transactions_uncommitted" + viewTransactions + " WHERE " + KEY_STATUS +  " = " + STATUS_UNCOMMITTED + ";");
        //      db.execSQL("CREATE VIEW transactions_all" + viewTransactions);
        //      db.execSQL("CREATE VIEW templates_all" +  VIEW_DEFINITION(TABLE_TEMPLATES));
      }

      if (oldVersion < 37) {
        db.execSQL("ALTER TABLE transactions add column number text");
        db.execSQL("ALTER TABLE paymentmethods add column is_numbered boolean default 0");
        ContentValues initialValues = new ContentValues();
        initialValues.put("is_numbered", true);
        db.update("paymentmethods", CONFLICT_NONE, initialValues, "label = ?", new String[]{"CHEQUE"});
      }

      if (oldVersion < 38) {
        db.execSQL("ALTER TABLE templates add column plan_id integer");
        db.execSQL("ALTER TABLE templates add column plan_execution boolean default 0");
      }

      if (oldVersion < 39) {
        //      db.execSQL("CREATE VIEW transactions_extended" + VIEW_DEFINITION_EXTENDED(TABLE_TRANSACTIONS) + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
        //      db.execSQL("CREATE VIEW templates_extended" +  VIEW_DEFINITION_EXTENDED(TABLE_TEMPLATES));
        db.execSQL("CREATE TABLE currency (_id integer primary key autoincrement, code text unique not null);");
        insertCurrencies(db);
      }

      if (oldVersion < 40) {
        //added currency to extended view
        db.execSQL("DROP VIEW IF EXISTS transactions_extended");
        db.execSQL("DROP VIEW IF EXISTS templates_extended");
        //      db.execSQL("CREATE VIEW transactions_extended" + VIEW_DEFINITION_EXTENDED(TABLE_TRANSACTIONS) + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
        //      db.execSQL("CREATE VIEW templates_extended" +  VIEW_DEFINITION_EXTENDED(TABLE_TEMPLATES));
      }

      if (oldVersion < 41) {
        db.execSQL("CREATE TABLE planinstance_transaction " +
            "(template_id integer references templates(_id), " +
            "instance_id integer, " +
            "transaction_id integer references transactions(_id), " +
            "primary key (instance_id,transaction_id));");
      }

      if (oldVersion < 42) {
        //migrate date field to unix time stamp (UTC)
        db.execSQL("ALTER TABLE transactions RENAME to transactions_old");
        db.execSQL("CREATE TABLE transactions (" +
            " _id integer primary key autoincrement," +
            " comment text, date datetime not null," +
            " amount integer not null," +
            " cat_id integer references categories(_id)," +
            " account_id integer not null references accounts(_id)," +
            " payee_id integer references payee(_id)," +
            " transfer_peer integer references transactions(_id)," +
            " transfer_account integer references accounts(_id)," +
            " method_id integer references paymentmethods(_id)," +
            " parent_id integer references transactions(_id)," +
            " status integer default 0," +
            " cr_status text not null check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED')) default 'RECONCILED'," +
            " number text)");
        db.execSQL("INSERT INTO transactions " +
            "(_id,comment,date,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status,number) " +
            "SELECT " +
            "_id, " +
            "comment, " +
            "strftime('%s',date,'utc'), " +
            "amount, " +
            "cat_id, " +
            "account_id, " +
            "payee_id, " +
            "transfer_peer, " +
            "transfer_account, " +
            "method_id," +
            "parent_id," +
            "status," +
            "cr_status, " +
            "number " +
            "FROM transactions_old");
        db.execSQL("DROP TABLE transactions_old");
      }

      if (oldVersion < 43) {
        db.execSQL("UPDATE accounts set currency = 'ZMW' WHERE currency = 'ZMK'");
        db.execSQL("UPDATE currency set code = 'ZMW' WHERE code = 'ZMK'");
      }

      if (oldVersion < 44) {
        //add ON DELETE CASCADE
        //accounts table sort_key column
        db.execSQL("ALTER TABLE planinstance_transaction RENAME to planinstance_transaction_old");
        db.execSQL("CREATE TABLE planinstance_transaction " +
            "(template_id integer references templates(_id) ON DELETE CASCADE, " +
            "instance_id integer, " +
            "transaction_id integer references transactions(_id) ON DELETE CASCADE, " +
            "primary key (instance_id,transaction_id));");
        db.execSQL("INSERT INTO planinstance_transaction " +
            "(template_id,instance_id,transaction_id)" +
            "SELECT " +
            "template_id,instance_id,transaction_id FROM planinstance_transaction_old");
        db.execSQL("DROP TABLE planinstance_transaction_old");
        db.execSQL("ALTER TABLE transactions RENAME to transactions_old");
        db.execSQL("CREATE TABLE transactions (" +
            " _id integer primary key autoincrement," +
            " comment text, date datetime not null," +
            " amount integer not null," +
            " cat_id integer references categories(_id)," +
            " account_id integer not null references accounts(_id) ON DELETE CASCADE," +
            " payee_id integer references payee(_id)," +
            " transfer_peer integer references transactions(_id)," +
            " transfer_account integer references accounts(_id)," +
            " method_id integer references paymentmethods(_id)," +
            " parent_id integer references transactions(_id) ON DELETE CASCADE," +
            " status integer default 0," +
            " cr_status text not null check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED')) default 'RECONCILED'," +
            " number text)");
        db.execSQL("INSERT INTO transactions " +
            "(_id,comment,date,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status,number) " +
            "SELECT " +
            "_id, " +
            "comment, " +
            "date, " +
            "amount, " +
            "cat_id, " +
            "account_id, " +
            "payee_id, " +
            "transfer_peer, " +
            "transfer_account, " +
            "method_id," +
            "parent_id," +
            "status," +
            "cr_status, " +
            "number " +
            "FROM transactions_old");
        db.execSQL("DROP TABLE transactions_old");
        db.execSQL("ALTER TABLE templates RENAME to templates_old");
        db.execSQL("CREATE TABLE templates (" +
            " _id integer primary key autoincrement," +
            " comment text," +
            " amount integer not null," +
            " cat_id integer references categories(_id)," +
            " account_id integer not null references accounts(_id) ON DELETE CASCADE," +
            " payee_id integer references payee(_id)," +
            " transfer_peer boolean default 0," +
            " transfer_account integer references accounts(_id) ON DELETE CASCADE," +
            " method_id integer references paymentmethods(_id)," +
            " title text not null," +
            " usages integer default 0," +
            " plan_id integer, " +
            " plan_execution boolean default 0, " +
            " unique(account_id,title));");
        db.execSQL("INSERT INTO templates " +
            "(_id,comment,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,title,usages,plan_id,plan_execution) " +
            "SELECT " +
            "_id, " +
            "comment, " +
            "amount, " +
            "cat_id, " +
            "account_id, " +
            "payee_id, " +
            "transfer_peer, " +
            "transfer_account, " +
            "method_id," +
            "title," +
            "usages, " +
            "plan_id, " +
            "plan_execution " +
            "FROM templates_old");
        db.execSQL("ALTER TABLE accounts add column sort_key integer");
      }

      if (oldVersion < 45) {
        db.execSQL("ALTER TABLE accounts add column exclude_from_totals boolean default 0");
        //added  to extended view
        db.execSQL("DROP VIEW IF EXISTS transactions_extended");
        db.execSQL("DROP VIEW IF EXISTS templates_extended");
        //      db.execSQL("CREATE VIEW transactions_extended" + VIEW_DEFINITION_EXTENDED(TABLE_TRANSACTIONS) + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
        //      db.execSQL("CREATE VIEW templates_extended" +  VIEW_DEFINITION_EXTENDED(TABLE_TEMPLATES));
      }

      if (oldVersion < 46) {
        db.execSQL("ALTER TABLE payee add column name_normalized text");
        Cursor c = MoreDbUtilsKt.query(db, "payee", new String[]{"_id", "name"}, null, null, null, null, null, null);
        if (c.moveToFirst()) {
          ContentValues v = new ContentValues();
          while (c.getPosition() < c.getCount()) {
            v.put("name_normalized", Utils.normalize(c.getString(1)));
            db.update("payee", CONFLICT_NONE, v, "_id = " + c.getLong(0), null);
            c.moveToNext();
          }
        }
        c.close();
      }

      if (oldVersion < 47) {
        db.execSQL("ALTER TABLE templates add column uuid text");
        db.execSQL(EVENT_CACHE_CREATE);
      }

      if (oldVersion < 48) {
        //added method_label to extended view

        if (oldVersion < 47) {
          String[] projection = new String[]{
              "templates._id",
              "amount",
              "comment",
              "cat_id",
              "CASE WHEN " +
                  "  " + "transfer_peer" + " " +
                  " THEN " +
                  "  (SELECT " + "label" + " FROM " + "accounts" + " WHERE " + "_id" + " = " + "transfer_account" + ") " +
                  " ELSE " +
                  " CASE WHEN " +
                  " (SELECT " + "parent_id" + " FROM " + "categories" + " WHERE " + "_id" + " = " + "cat_id" + ") " +
                  " THEN " +
                  " (SELECT " + "label" + " FROM " + "categories" + " WHERE " + "_id" + " = " +
                  " (SELECT " + "parent_id" + " FROM " + "categories" + " WHERE " + "_id" + " = " + "cat_id" + ")) " +
                  "  || ' : ' || " +
                  " (SELECT " + "label" + " FROM " + "categories" + " WHERE " + "_id" + " = " + "cat_id" + ") " +
                  " ELSE" +
                  " (SELECT " + "label" + " FROM " + "categories" + " WHERE " + "_id" + " = " + "cat_id" + ") " +
                  " END " +
                  " END AS  " + "label",
              "name",
              "transfer_peer",
              "transfer_account",
              "account_id",
              "method_id",
              "paymentmethods.label AS method_label",
              "title",
              "plan_id",
              "plan_execution",
              "uuid",
              "currency"
          };
          SupportSQLiteQuery q = SupportSQLiteQueryBuilder
                  .builder("templates LEFT JOIN payee ON payee_id = payee._id" +
              " LEFT JOIN accounts ON account_id = accounts._id" +
              " LEFT JOIN paymentmethods ON method_id = paymentmethods._id").columns(projection).create();
          Cursor c = db.query(q);
          if (c != null) {
            if (c.moveToFirst()) {
              ContentValues templateValues = new ContentValues();
              while (c.getPosition() < c.getCount()) {
                templateValues.put("uuid", Model.generateUuid());
                long templateId = c.getLong(c.getColumnIndexOrThrow("_id"));
                db.update("templates", CONFLICT_NONE, templateValues, "_id = " + templateId, null);
                c.moveToNext();
              }
            }
            c.close();
          }
        }
      }

      if (oldVersion < 49) {
        //forgotten to drop in previous upgrade
        db.execSQL("DROP TABLE IF EXISTS templates_old");
      }

      if (oldVersion < 50) {
        db.execSQL("ALTER TABLE transactions add column picture_id text");
        db.execSQL("DROP TABLE IF EXISTS feature_used");
      }

      if (oldVersion < 51) {
        File pictureDir = PictureDirHelper.getPictureDir(MyApplication.Companion.getInstance(), false);
        //fallback if not mounted
        if (pictureDir == null) {
          pictureDir = new File(
              Environment.getExternalStorageDirectory().getPath() +
                  "/Android/data/" + MyApplication.Companion.getInstance().getPackageName() + "/files",
              Environment.DIRECTORY_PICTURES);
        }
        if (!pictureDir.exists()) {
          CrashHandler.report(new Exception("Unable to calculate pictureDir during upgrade"));
        }
        //if pictureDir does not exist, we use its URI nonetheless, in order to have the data around
        //for potential trouble handling
        String prefix = Uri.fromFile(pictureDir).toString() + "/";
        String postfix = ".jpg";
        //if picture_id concat expression will also be null
        db.execSQL("UPDATE transactions set picture_id = '" + prefix + "'||picture_id||'" + postfix + "'");

        db.execSQL("CREATE TABLE stale_uris ( picture_id text);");
        db.execSQL("CREATE TRIGGER cache_stale_uri BEFORE DELETE ON transactions WHEN old.picture_id NOT NULL "
            + " BEGIN INSERT INTO stale_uris VALUES (old.picture_id); END");
      }

      if (oldVersion < 52) {
        db.execSQL("CREATE INDEX transactions_cat_id_index on transactions(cat_id)");
        db.execSQL("CREATE INDEX templates_cat_id_index on templates(cat_id)");
      }

      if (oldVersion < 53) {
        //add VOID status
        db.execSQL("ALTER TABLE transactions RENAME to transactions_old");
        db.execSQL("CREATE TABLE " + "transactions" + "( "
            + "_id" + " integer primary key autoincrement, "
            + "comment" + " text, "
            + "date" + " datetime not null, "
            + "amount" + " integer not null, "
            + "cat_id" + " integer references " + "categories" + "(" + "_id" + "), "
            + "account_id" + " integer not null references " + "accounts" + "(" + "_id" + ") ON DELETE CASCADE,"
            + "payee_id" + " integer references " + "payee" + "(" + "_id" + "), "
            + "transfer_peer" + " integer references " + "transactions" + "(" + "_id" + "), "
            + "transfer_account" + " integer references " + "accounts" + "(" + "_id" + "),"
            + "method_id" + " integer references " + "paymentmethods" + "(" + "_id" + "),"
            + "parent_id" + " integer references " + "transactions" + "(" + "_id" + ") ON DELETE CASCADE, "
            + "status" + " integer default 0, "
            + "cr_status" + " text not null check (" + "cr_status" + " in ('UNRECONCILED','CLEARED','RECONCILED','VOID')) default 'RECONCILED', "
            + "number" + " text, "
            + "picture_id" + " text);");
        db.execSQL("INSERT INTO transactions " +
            "(_id,comment,date,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,parent_id,status,cr_status,number,picture_id) " +
            "SELECT " +
            "_id, " +
            "comment, " +
            "date, " +
            "amount, " +
            "cat_id, " +
            "account_id, " +
            "payee_id, " +
            "transfer_peer, " +
            "transfer_account, " +
            "method_id," +
            "parent_id," +
            "status," +
            "cr_status, " +
            "number, " +
            "picture_id " +
            "FROM transactions_old");
        db.execSQL("DROP TABLE transactions_old");
        db.execSQL("CREATE TRIGGER cache_stale_uri BEFORE DELETE ON transactions WHEN old.picture_id NOT NULL "
            + " BEGIN INSERT INTO stale_uris VALUES (old.picture_id); END");
        db.execSQL("CREATE INDEX transactions_cat_id_index on transactions(cat_id)");
      }

      if (oldVersion < 54) {
        db.execSQL("DROP TRIGGER cache_stale_uri");
        db.execSQL("CREATE TRIGGER cache_stale_uri " +
            "AFTER DELETE ON " + "transactions" + " " +
            "WHEN old." + "picture_id" + " NOT NULL " +
            "AND NOT EXISTS " +
            "(SELECT 1 FROM " + "transactions" + " " +
            "WHERE " + "picture_id" + " = old." + "picture_id" + ") " +
            "BEGIN INSERT INTO " + "stale_uris" + " VALUES (old." + "picture_id" + "); END");
        //all Accounts with old default color are updated to the new one
        db.execSQL(String.format(Locale.US, "UPDATE accounts set color = %d WHERE color = %d", 0xff009688, 0xff99CC00));
      }

      if (oldVersion < 55) {
        db.execSQL("ALTER TABLE categories add column label_normalized text");
        Cursor c = MoreDbUtilsKt.query(db,"categories", new String[]{"_id", "label"}, null, null, null, null, null, null);
        if (c.moveToFirst()) {
          ContentValues v = new ContentValues();
          while (c.getPosition() < c.getCount()) {
            v.put("label_normalized", Utils.normalize(c.getString(1)));
            db.update("categories", CONFLICT_NONE, v, "_id = " + c.getLong(0), null);
            c.moveToNext();
          }
        }
        c.close();
      }

      if (oldVersion < 56) {
        db.execSQL("ALTER TABLE templates add column last_used datetime");
        db.execSQL("ALTER TABLE categories add column last_used datetime");
        db.execSQL("ALTER TABLE accounts add column last_used datetime");
//        db.execSQL("CREATE TRIGGER sort_key_default AFTER INSERT ON accounts " +
//            "BEGIN UPDATE accounts SET sort_key = (SELECT coalesce(max(sort_key),0) FROM accounts) + 1 " +
//            "WHERE _id = NEW._id; END");
        //The sort key could be set by user in previous versions, now it is handled internally
        Cursor c = MoreDbUtilsKt.query(db, "accounts", new String[]{"_id", "sort_key"}, null, null, null, null, "sort_key ASC", null);
        boolean hasAccountSortKeySet = false;
        if (c.moveToFirst()) {
          ContentValues v = new ContentValues();
          while (c.getPosition() < c.getCount()) {
            v.put("sort_key", c.getPosition() + 1);
            db.update("accounts", CONFLICT_NONE, v, "_id = ?", new String[]{c.getString(0)});
            if (c.getInt(1) != 0) hasAccountSortKeySet = true;
            c.moveToNext();
          }
        }
        c.close();
        PrefHandler prefHandler = getPrefHandler();
        String legacy = prefHandler.getString(PrefKey.SORT_ORDER_LEGACY, "USAGES");
        prefHandler.putString(PrefKey.SORT_ORDER_TEMPLATES, legacy);
        prefHandler.putString(PrefKey.SORT_ORDER_CATEGORIES, legacy);
        prefHandler.putString(PrefKey.SORT_ORDER_ACCOUNTS, hasAccountSortKeySet ? "CUSTOM" : legacy);
        prefHandler.remove(PrefKey.SORT_ORDER_LEGACY);
      }

      if (oldVersion < 57) {
        //fix custom app uris
        try {
          if (CALENDAR.hasPermission(MyApplication.Companion.getInstance())) {
            Cursor c = MoreDbUtilsKt.query(db,"templates", new String[]{"_id", "plan_id"}, "plan_id IS NOT null", null, null, null, null, null);
            if (c.moveToFirst()) {
              while (!c.isAfterLast()) {
                Plan.updateCustomAppUri(
                        MyApplication.Companion.getInstance().getContentResolver(),
                        c.getLong(1),
                        Template.buildCustomAppUri(c.getLong(0))
                );
                c.moveToNext();
              }
            }
            c.close();
          }
        } catch (Exception e) {
          //we have seen updateCustomAppUri fail, this should not prevent the database upgrade
          CrashHandler.report(e);
        }

        //Drop unique constraint on templates

        db.execSQL("ALTER TABLE templates RENAME to templates_old");
        db.execSQL("CREATE TABLE templates (" +
            " _id integer primary key autoincrement," +
            " comment text," +
            " amount integer not null," +
            " cat_id integer references categories(_id)," +
            " account_id integer not null references accounts(_id) ON DELETE CASCADE," +
            " payee_id integer references payee(_id)," +
            " transfer_peer boolean default 0," +
            " transfer_account integer references accounts(_id) ON DELETE CASCADE," +
            " method_id integer references paymentmethods(_id)," +
            " title text not null," +
            " usages integer default 0," +
            " plan_id integer, " +
            " plan_execution boolean default 0, " +
            " uuid text, " +
            " last_used datetime);");
        db.execSQL("INSERT INTO templates " +
            "(_id,comment,amount,cat_id,account_id,payee_id,transfer_peer,transfer_account,method_id,title,usages,plan_id,plan_execution,uuid,last_used) " +
            "SELECT " +
            "_id, " +
            "comment, " +
            "amount, " +
            "cat_id, " +
            "account_id, " +
            "payee_id, " +
            "transfer_peer, " +
            "transfer_account, " +
            "method_id," +
            "title," +
            "usages, " +
            "plan_id, " +
            "plan_execution, uuid, last_used " +
            "FROM templates_old");
        db.execSQL("DROP TABLE templates_old");
        //refreshViews1(db);
      }

      if (oldVersion < 58) {
        //cache fraction digits
        Cursor c = db.query("SELECT distinct currency from accounts");
        if (c.moveToFirst()) {
          while (!c.isAfterLast()) {
            CurrencyContext currencyContext = MyApplication.Companion.getInstance().getAppComponent().currencyContext();
            currencyContext.ensureFractionDigitsAreCached(currencyContext.get(c.getString(0)));
            c.moveToNext();
          }
        }
        c.close();
      }

      if (oldVersion < 59) {
        db.execSQL("ALTER TABLE transactions add column uuid text");
        db.execSQL("CREATE UNIQUE INDEX transactions_account_uuid ON transactions(account_id,uuid,status)");
        db.execSQL("ALTER TABLE accounts add column sync_account_name text");
        db.execSQL("ALTER TABLE accounts add column sync_sequence_local integer default 0");
        db.execSQL("ALTER TABLE accounts add column sync_from_adapter integer default 0");
        db.execSQL("ALTER TABLE accounts add column uuid text");
        db.execSQL("CREATE UNIQUE INDEX accounts_uuid ON accounts(uuid)");
        db.execSQL("CREATE TABLE changes ( account_id integer not null references accounts(_id) ON DELETE CASCADE,type text not null check (type in ('created','updated','deleted')), sync_sequence_local integer, uuid text, timestamp datetime DEFAULT (strftime('%s','now')), parent_uuid text, comment text, date datetime, amount integer, cat_id integer references categories(_id) ON DELETE SET NULL, payee_id integer references payee(_id) ON DELETE SET NULL, transfer_account integer references accounts(_id) ON DELETE SET NULL,method_id integer references paymentmethods(_id),cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')),number text, picture_id text)");
        //createOrRefreshChangelogTriggers(db);
        db.execSQL("CREATE TRIGGER insert_increase_category_usage AFTER INSERT ON transactions WHEN new.cat_id IS NOT NULL AND new.cat_id != 0 BEGIN UPDATE categories SET usages = usages + 1, last_used = strftime('%s', 'now')  WHERE _id IN (new.cat_id , (SELECT parent_id FROM categories WHERE _id = new.cat_id)); END;");
        db.execSQL("CREATE TRIGGER update_increase_category_usage AFTER UPDATE ON transactions WHEN new.cat_id IS NOT NULL AND (old.cat_id IS NULL OR new.cat_id != old.cat_id) BEGIN UPDATE categories SET usages = usages + 1, last_used = strftime('%s', 'now')  WHERE _id IN (new.cat_id , (SELECT parent_id FROM categories WHERE _id = new.cat_id)); END;");
        db.execSQL("CREATE TRIGGER insert_increase_account_usage AFTER INSERT ON transactions WHEN new.parent_id IS NULL BEGIN UPDATE accounts SET usages = usages + 1, last_used = strftime('%s', 'now')  WHERE _id = new.account_id; END;");
        db.execSQL("CREATE TRIGGER update_increase_account_usage AFTER UPDATE ON transactions WHEN new.parent_id IS NULL AND new.account_id != old.account_id AND (old.transfer_account IS NULL OR new.account_id != old.transfer_account) BEGIN UPDATE accounts SET usages = usages + 1, last_used = strftime('%s', 'now')  WHERE _id = new.account_id; END;");
        //db.execSQL("CREATE TRIGGER update_account_sync_null AFTER UPDATE ON accounts WHEN new.sync_account_name IS NULL AND old.sync_account_name IS NOT NULL BEGIN UPDATE accounts SET sync_sequence_local = 0 WHERE _id = old._id; DELETE FROM changes WHERE account_id = old._id; END;");
        //refreshViews2(db);
      }

      if (oldVersion < 60) {
        // Repair inconsistent uuids for transfers
        db.execSQL("UPDATE transactions set uuid = (select uuid from transactions peers where peers._id = transactions.transfer_peer) where transfer_peer > _id");
      }

      if (oldVersion < 61) {
        //Repair failed uuid seeding of changes
        db.execSQL("UPDATE accounts set sync_sequence_local = 0 where _id in (select distinct account_id from changes where uuid is null)");
        db.execSQL("DELETE FROM changes where account_id in (select distinct account_id from changes where uuid is null)");

        //force changes to have uuid
        db.execSQL("ALTER TABLE changes RENAME to changes_old");
        db.execSQL("CREATE TABLE changes ( account_id integer not null references accounts(_id) ON DELETE CASCADE,type text not null check (type in ('created','updated','deleted')), sync_sequence_local integer, uuid text not null, timestamp datetime DEFAULT (strftime('%s','now')), parent_uuid text, comment text, date datetime, amount integer, cat_id integer references categories(_id) ON DELETE SET NULL, payee_id integer references payee(_id) ON DELETE SET NULL, transfer_account integer references accounts(_id) ON DELETE SET NULL,method_id integer references paymentmethods(_id),cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')),number text, picture_id text)");
        db.execSQL("INSERT INTO changes " +
            "(account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id)" +
            "SELECT account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id FROM changes_old");
        db.execSQL("DROP TABLE changes_old");
      }

      //if (oldVersion < 62) {
        //refreshViewsExtended(db);
      //}

      if (oldVersion < 63) {
        db.execSQL("CREATE TABLE _sync_state (status integer)");
        //createOrRefreshChangelogTriggers(db);
      }
      TransactionProvider.pauseChangeTrigger(db);

      if (oldVersion < 64) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("code", CurrencyEnum.BYN.name());
        //will log SQLiteConstraintException if value already exists in table
        db.insert("currency", CONFLICT_IGNORE, initialValues);
      }

      if (oldVersion < 65) {
        //unfortunately we have to drop information about canceled instances
        db.delete("planinstance_transaction", "transaction_id is null", null);
        //we update instance_id to negative numbers, in order to prevent Conflict, which would araise
        //in the rare case where an existing instance_id equals a newly calculated one
        db.execSQL("update planinstance_transaction set instance_id = - rowid");
        Cursor c = db.query("SELECT rowid, (SELECT date from transactions where _id = transaction_id) FROM planinstance_transaction");
        if (c.moveToFirst()) {
          ContentValues v = new ContentValues();
          while (c.getPosition() < c.getCount()) {
            String rowId = c.getString(0);
            long date = c.getLong(1);
            String whereClause = "rowid = ?";
            String[] whereArgs = {rowId};
            //This will be correct only for instances where date has not been edited by user, but it is the best we can do
            v.put("instance_id", CalendarProviderProxy.calculateId(date * 1000));
            try {
              db.update("planinstance_transaction", CONFLICT_NONE, v, whereClause, whereArgs);
            } catch (Exception e) {
              CrashHandler.report(e);
            }
            c.moveToNext();
          }
        }
        c.close();
      }

      if (oldVersion < 66) {
        db.execSQL(String.format(Locale.ROOT, "CREATE TABLE %s (%s text unique not null, %s text unique not null);", "settings", "key", "value"));
      }

      if (oldVersion < 67) {
        db.delete("planinstance_transaction", "instance_id < 0", null);
        db.execSQL("ALTER TABLE planinstance_transaction RENAME to planinstance_transaction_old");
        db.execSQL("CREATE TABLE planinstance_transaction " +
            "(template_id integer references templates(_id) ON DELETE CASCADE, " +
            "instance_id integer, " +
            "transaction_id integer unique references transactions(_id) ON DELETE CASCADE);");
        db.execSQL("INSERT INTO planinstance_transaction " +
            "(template_id,instance_id,transaction_id)" +
            "SELECT " +
            "template_id,instance_id,transaction_id FROM planinstance_transaction_old");
        db.execSQL("DROP TABLE planinstance_transaction_old");
      }

      if (oldVersion < 68) {
        db.execSQL("ALTER TABLE templates RENAME to templates_old");
        db.execSQL("CREATE TABLE templates ( _id integer primary key autoincrement, comment text, "
            + "amount integer not null, cat_id integer references categories(_id), "
            + "account_id integer not null references accounts(_id) ON DELETE CASCADE,"
            + "payee_id integer references payee(_id), "
            + "transfer_account integer references accounts(_id) ON DELETE CASCADE,"
            + "method_id integer references paymentmethods(_id), title text not null, "
            + "usages integer default 0, plan_id integer, plan_execution boolean default 0, uuid text, "
            + "last_used datetime,"
            + "parent_id integer references templates(_id) ON DELETE CASCADE, "
            + "status integer default 0);");
        db.execSQL("INSERT INTO templates " +
            "(_id,comment,amount,cat_id,account_id,payee_id,transfer_account,method_id,title,usages,plan_id,plan_execution,uuid,last_used) " +
            "SELECT " +
            " _id,comment,amount,cat_id,account_id,payee_id,transfer_account,method_id,title,usages,plan_id,plan_execution,uuid,last_used " +
            "FROM templates_old");
        db.execSQL("DROP TABLE templates_old");
        db.execSQL("ALTER TABLE accounts RENAME to accounts_old");
        db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, "
            + "opening_balance integer, description text, currency text not null, "
            + "type text not null check (type in ('CASH','BANK','CCARD','ASSET','LIABILITY')) default 'CASH', "
            + "color integer default -3355444, "
            + "grouping text not null check (grouping in ('NONE','DAY','WEEK','MONTH','YEAR')) default 'NONE', "
            + "usages integer default 0, last_used datetime, sort_key integer, sync_account_name text, "
            + "sync_sequence_local integer default 0, exclude_from_totals boolean default 0, "
            + "uuid text);");
        db.execSQL("INSERT INTO accounts " +
            "(_id,label,opening_balance,description,currency,type,color,grouping,usages,last_used,sort_key,sync_account_name,sync_sequence_local,exclude_from_totals,uuid) " +
            " SELECT " +
            " _id,label,opening_balance,description,currency,type,color,grouping,usages,last_used,sort_key,sync_account_name,sync_sequence_local,exclude_from_totals,uuid " +
            "FROM accounts_old");
        db.execSQL("DROP TABLE accounts_old");
        //createOrRefreshViews(db);

        db.execSQL("CREATE TRIGGER protect_split_transaction BEFORE DELETE ON categories " +
            " WHEN (OLD._id = 0)" +
            " BEGIN SELECT RAISE (FAIL, 'split category can not be deleted'); " +
            " END;");
      }

      if (oldVersion < 69) {
        //repair missed trigger recreation
        //createOrRefreshAccountTriggers(db);
        //while trigger was not set new accounts were added without sort key leading to crash
        //https://github.com/mtotschnig/MyExpenses/issues/420
        //we now set sort_key again for all accounts trying to preserve existing order
        Cursor c = MoreDbUtilsKt.query(db, "accounts", new String[]{"_id"}, null, null, null, null, "sort_key ASC", null);
        if (c.moveToFirst()) {
          ContentValues v = new ContentValues();
          while (c.getPosition() < c.getCount()) {
            v.put("sort_key", c.getPosition() + 1);
            db.update("accounts", CONFLICT_NONE, v, "_id = ?", new String[]{c.getString(0)});
            c.moveToNext();
          }
        }
        c.close();
      }

      if (oldVersion < 70) {
        db.execSQL("ALTER TABLE accounts add column sort_direction text not null check (sort_direction in " +
            "('ASC','DESC')) default 'DESC'");
      }

      if (oldVersion < 71) {
        db.execSQL("CREATE TABLE account_exchangerates (account_id integer not null references accounts(_id) ON DELETE CASCADE," +
            "currency_self text not null, currency_other text not null, exchange_rate real not null, " +
            "UNIQUE (account_id,currency_self,currency_other));");
        db.execSQL("ALTER TABLE transactions add column original_amount integer");
        db.execSQL("ALTER TABLE transactions add column original_currency text");
        db.execSQL("ALTER TABLE transactions add column equivalent_amount integer");
        db.execSQL("ALTER TABLE changes add column original_amount integer");
        db.execSQL("ALTER TABLE changes add column original_currency text");
        db.execSQL("ALTER TABLE changes add column equivalent_amount integer");
      }

      if (oldVersion < 72) {
        //add new change type
        db.execSQL("ALTER TABLE changes RENAME to changes_old");
        db.execSQL("CREATE TABLE changes ( account_id integer not null references accounts(_id) ON DELETE CASCADE, " +
            "type text not null check (type in ('created','updated','deleted','unsplit')), " +
            "sync_sequence_local integer, uuid text not null, timestamp datetime DEFAULT (strftime('%s','now')), " +
            "parent_uuid text, comment text, date datetime, " +
            "amount integer, original_amount integer, original_currency text, equivalent_amount integer, " +
            "cat_id integer references categories(_id) ON DELETE SET NULL, " +
            "payee_id integer references payee(_id) ON DELETE SET NULL, " +
            "transfer_account integer references accounts(_id) ON DELETE SET NULL, " +
            "method_id integer references paymentmethods(_id), " +
            "cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')), " +
            "number text, picture_id text)");
        db.execSQL("INSERT INTO changes " +
            "(account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id)" +
            "SELECT account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id FROM changes_old");
        db.execSQL("DROP TABLE changes_old");
      }

      if (oldVersion < 73) {
        db.execSQL("ALTER TABLE transactions add column value_date");
        db.execSQL("ALTER TABLE changes add column value_date");
      }

      if (oldVersion < 74) {
        db.execSQL("DROP TRIGGER IF EXISTS insert_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS insert_after_update_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS delete_after_update_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS delete_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS update_change_log");
        db.execSQL("update transactions set transfer_peer = (select _id from transactions peer where peer.transfer_peer = transactions._id) where transfer_peer is null;");
        //createOrRefreshTransactionTriggers(db);
      }

      if (oldVersion < 75) {
        //repair broken settings table
        db.execSQL("ALTER TABLE settings RENAME to settings_old");
        db.execSQL("CREATE TABLE settings (key text unique not null, value text);");
        db.execSQL("INSERT INTO settings (key, value) SELECT key, value from settings_old");
        db.execSQL("DROP TABLE settings_old");
      }

      if (oldVersion < 76) {
        db.execSQL("ALTER TABLE accounts add column criterion integer");
      }

      if (oldVersion < 77) {
        db.execSQL("DROP INDEX transactions_account_uuid");
        db.execSQL("CREATE UNIQUE INDEX transactions_account_uuid_index ON transactions(uuid,account_id,status)");
      }

      if (oldVersion < 78) {
        db.execSQL("ALTER TABLE categories add column color integer");
        Cursor c = MoreDbUtilsKt.query(db,"categories", new String[]{"_id"}, "parent_id is null", null, null, null, KEY_USAGES, null);
        if (c.moveToFirst()) {
          ContentValues v = new ContentValues();
          int count = 0;
          while (c.getPosition() < c.getCount()) {
            v.put(KEY_COLOR, MAIN_COLORS[count % MAIN_COLORS.length]);
            db.update("categories", CONFLICT_NONE, v, "_id = " + c.getLong(0), null);
            c.moveToNext();
            count++;
          }
        }
        c.close();
      }

      if (oldVersion < 79) {
        db.execSQL("DROP INDEX if exists transactions_account_uuid_index");
        db.execSQL("CREATE UNIQUE INDEX transactions_account_uuid_index ON transactions(account_id,uuid,status)");
      }

      if (oldVersion < 80) {
        db.execSQL("CREATE TABLE budgets (_id integer primary key autoincrement," +
            "grouping text not null check (grouping in ('NONE','DAY','WEEK','MONTH','YEAR')), budget integer not null, "
            + "account_id integer references accounts(_id) ON DELETE CASCADE, "
            + "currency text)");
        db.execSQL("CREATE TABLE budget_categories ( "
            + "budget_id integer references budgets(_id) ON DELETE CASCADE, "
            + "cat_id integer references categories(_id), "
            + "budget integer not null, "
            + "primary key (budget_id,cat_id));");
        db.execSQL("ALTER TABLE currency add column grouping text not null check (grouping in " +
            "('NONE','DAY','WEEK','MONTH','YEAR')) default 'NONE'");
        Cursor c = db.query("SELECT distinct currency from accounts");
        if (c.moveToFirst()) {
          String GROUPING_PREF_PREFIX = "AGGREGATE_GROUPING_";
          final SharedPreferences settings = MyApplication.Companion.getInstance().getSettings();
          final SharedPreferences.Editor editor = settings.edit();
          boolean updated = false;
          while (!c.isAfterLast()) {
            final String currency = c.getString(0);
            final String key = GROUPING_PREF_PREFIX + currency;
            final String grouping = settings.getString(key, "NONE");
            if (!grouping.equals("NONE")) {
              ContentValues initialValues = new ContentValues();
              initialValues.put("grouping", grouping);
              try {
                db.update("currency", CONFLICT_NONE, initialValues, "code = ?", new String[]{currency});
                editor.remove(key);
                updated = true;
              } catch (Exception e) {
                //since this setting is not critical, we can live with failure of migration
                CrashHandler.report(e);
              }
            }
            c.moveToNext();
          }
          if (updated) {
            editor.apply();
          }
        }
        c.close();
      }

      if (oldVersion < 81) {
        db.execSQL("ALTER TABLE currency add column label text");
        //add foreign key link to currency table
        db.execSQL("ALTER TABLE accounts RENAME to accounts_old");
        db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, "
            + "opening_balance integer, description text, currency text not null references currency (code), "
            + "type text not null check (type in ('CASH','BANK','CCARD','ASSET','LIABILITY')) default 'CASH', "
            + "color integer default -3355444, "
            + "grouping text not null check (grouping in ('NONE','DAY','WEEK','MONTH','YEAR')) default 'NONE', "
            + "usages integer default 0, last_used datetime, sort_key integer, sync_account_name text, "
            + "sync_sequence_local integer default 0, exclude_from_totals boolean default 0, "
            + "uuid text, sort_direction text not null check (sort_direction  in ('ASC','DESC')) default 'DESC', criterion integer);");
        db.execSQL("INSERT INTO accounts " +
            "(_id,label,opening_balance,description,currency,type,color,grouping,usages,last_used,sort_key,sync_account_name,sync_sequence_local,exclude_from_totals,uuid,sort_direction,criterion) " +
            " SELECT " +
            " _id,label,opening_balance,description,currency,type,color,grouping,usages,last_used,sort_key,sync_account_name,sync_sequence_local,exclude_from_totals,uuid,sort_direction,criterion " +
            "FROM accounts_old");
        db.execSQL("DROP TABLE accounts_old");
      }

      //if (oldVersion < 82) {
        //createOrRefreshAccountTriggers(db);
      //}

      if (oldVersion < 83) {
        final String auto_backup_cloud = MyApplication.Companion.getInstance().getSettings().getString("auto_backup_cloud", null);
        if (auto_backup_cloud != null) {
          ContentValues values = new ContentValues(2);
          values.put("key", "auto_backup_cloud");
          values.put("value", auto_backup_cloud);
          db.insert("settings", CONFLICT_NONE, values);
        }
      }

      if (oldVersion < 84) {
        try {
          db.execSQL("CREATE UNIQUE INDEX budgets_type_account ON budgets(grouping,account_id)");
          db.execSQL("CREATE UNIQUE INDEX budgets_type_currency ON budgets(grouping,currency);");
        } catch (SQLException e) {
          // We got one report where this failed, because there were already multiple budgets for
          // account /grouping pairs. At the moment, we silently live without the index.
          CrashHandler.report(e);
        }
      }

      if (oldVersion < 85) {
        db.execSQL("ALTER TABLE accounts add column hidden boolean default 0");
        db.execSQL("ALTER TABLE accounts add column sealed boolean default 0");
        createOrRefreshAccountSealedTrigger(db);
        //createOrRefreshTransactionSealedTriggers(db);
      }

/*
      if (oldVersion < 86) {
        db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
        db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
      }
*/

      //if (oldVersion < 87) {
        //createOrRefreshTemplateViews(db);
      //}

      if (oldVersion < 88) {
        db.execSQL("ALTER TABLE categories add column icon string");
      }

      //if (oldVersion < 89) {
        //createOrRefreshViews(db);
      //}

      if (oldVersion < 90) {
        db.execSQL("ALTER TABLE budget_categories RENAME to budget_categories_old");
        db.execSQL("CREATE TABLE budget_categories (budget_id integer references budgets(_id) ON DELETE CASCADE, "
            + "cat_id integer references categories(_id) ON DELETE CASCADE, budget integer not null, "
            + "primary key (budget_id,cat_id))");
        db.execSQL("INSERT INTO budget_categories (budget_id,cat_id,budget) " +
            " SELECT  budget_id,cat_id,budget FROM budget_categories_old");
        db.execSQL("DROP TABLE budget_categories_old");
      }

      if (oldVersion < 91) {
        db.execSQL("ALTER TABLE budgets ADD COLUMN title text not null default ''");
        db.execSQL("ALTER TABLE budgets ADD COLUMN description text");
        db.execSQL("ALTER TABLE budgets ADD COLUMN start datetime");
        db.execSQL("ALTER TABLE budgets ADD COLUMN end datetime");
        db.execSQL("DROP INDEX if exists budgets_type_account");
        db.execSQL("DROP INDEX if exists budgets_type_currency");
        Cursor c = MoreDbUtilsKt.query(db, "budgets", new String[]{"_id",
                String.format(Locale.ROOT, "coalesce(%1$s, -(select %2$s from %3$s where %4$s = %5$s), %6$d) AS %1$s",
                    "account_id", "_id", "currency", "code", "budgets.currency", HOME_AGGREGATE_ID), "grouping"},
            null, null, null, null, null, null);
        if (c.moveToFirst()) {
          final SharedPreferences settings = MyApplication.Companion.getInstance().getSettings();
          final SharedPreferences.Editor editor = settings.edit();
          while (c.getPosition() < c.getCount()) {
            final long accountId = c.getLong(1);
            editor.remove(String.format(Locale.ROOT, "current_budgetType_%d", accountId));
            editor.putLong(String.format(Locale.ROOT, "defaultBudget_%d_%s", accountId, c.getString(2)), c.getLong(0));
            c.moveToNext();
          }
          editor.apply();
        }
        c.close();
      }

      if (oldVersion < 92) {
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_CHANGES_EXTENDED);
        //method_id on delete set null
        db.execSQL("ALTER TABLE changes RENAME to changes_old");
        db.execSQL("CREATE TABLE changes ( account_id integer not null references accounts(_id) ON DELETE CASCADE, " +
            "type text not null check (type in ('created','updated','deleted','unsplit')), " +
            "sync_sequence_local integer, " +
            "uuid text not null," +
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
            "transfer_account integer references accounts(_id) ON DELETE SET NULL, " +
            "method_id integer references paymentmethods(_id) ON DELETE SET NULL, " +
            "cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')), " +
            "number text," +
            "picture_id text)");
        db.execSQL("INSERT INTO changes " +
            "(account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, value_date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id)" +
            "SELECT account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, value_date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id FROM changes_old");
        db.execSQL("DROP TABLE changes_old");
        //db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
      }

      //if (oldVersion < 93) {
        //on very recent versions of Sqlite renaming tables like done in upgrade to 92 breaks views AND triggers
        //createOrRefreshTransactionTriggers(db);
      //}

      //if (oldVersion < 94) {
        //createOrRefreshAccountTriggers(db);
      //}

/*      if (oldVersion < 95) {
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_EXTENDED);
        db.execSQL("CREATE VIEW " + VIEW_EXTENDED + buildViewDefinitionExtended(TABLE_TRANSACTIONS) + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
      }*/
/*      if (oldVersion < 96) {
        db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
        db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
      }*/
      if (oldVersion < 97) {
        //This index has been lost after a table rename
        db.execSQL("CREATE INDEX IF NOT EXISTS templates_cat_id_index on templates(cat_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS budget_categories_cat_id_index on budget_categories(cat_id);");
      }
      if (oldVersion < 99) {
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_CHANGES_EXTENDED);
        //add new change type
        db.execSQL("ALTER TABLE changes RENAME to changes_old");
        db.execSQL("CREATE TABLE changes ( account_id integer not null references accounts(_id) ON DELETE CASCADE, " +
            "type text not null check (type in ('created','updated','deleted','unsplit','metadata')), " +
            "sync_sequence_local integer, " +
            "uuid text not null," +
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
            "transfer_account integer references accounts(_id) ON DELETE SET NULL, " +
            "method_id integer references paymentmethods(_id) ON DELETE SET NULL, " +
            "cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')), " +
            "number text," +
            "picture_id text)");
        db.execSQL("INSERT INTO changes " +
            "(account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, value_date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id)" +
            "SELECT account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, value_date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id FROM changes_old");
        db.execSQL("DROP TABLE changes_old");
        //db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
        //createOrRefreshTransactionTriggers(db);
        //createOrRefreshAccountTriggers(db);
        //createOrRefreshAccountMetadataTrigger(db);
      }
/*      if (oldVersion < 100) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("code", CurrencyEnum.VEB.name());
        db.insert("currency", null, initialValues);
      }*/
      if (oldVersion < 102) {
        db.execSQL("CREATE TABLE tags (_id integer primary key autoincrement, label text UNIQUE not null)");
        db.execSQL("CREATE TABLE transactions_tags ( tag_id integer references tags(_id) ON DELETE CASCADE, transaction_id integer references transactions(_id) ON DELETE CASCADE, primary key (tag_id,transaction_id))");
        //createOrRefreshTransferTagsTriggers(db);
        db.execSQL("CREATE TABLE templates_tags ( tag_id integer references tags(_id) ON DELETE CASCADE, template_id integer references templates(_id) ON DELETE CASCADE, primary key (tag_id,template_id));");
        //createOrRefreshViews(db);
      }
      if (oldVersion < 103) {
        createOrRefreshTransferTagsTriggers(db);
      }
      if (oldVersion < 104) {
/*        db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
        db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);*/
        //repair uuids that got lost by bug
        repairTransferUuids(db);
      }
      if (oldVersion < 105) {
        //db.execSQL("DROP VIEW IF EXISTS " + VIEW_WITH_ACCOUNT);
        //db.execSQL(VIEW_WITH_ACCOUNT_DEFINITION);
      }
      if (oldVersion < 106) {
        //db.execSQL("DROP TRIGGER IF EXISTS update_change_log");
        //db.execSQL(TRANSACTIONS_UPDATE_TRIGGER_CREATE);
      }
      if (oldVersion < 107) {
        repairSplitPartDates(db);
      }
      if (oldVersion < 108) {
        db.execSQL("ALTER TABLE templates add column plan_execution_advance integer default 0");
      }
      if (oldVersion < 109) {
        db.execSQL("CREATE INDEX transactions_payee_id_index on transactions(payee_id)");
        db.execSQL("CREATE INDEX templates_payee_id_index on templates(payee_id)");
      }
      //if (oldVersion < 110) {
        //createOrRefreshTemplateViews(db);
      //}
      if (oldVersion < 111) {
        repairSplitPartDates(db);
      }
      if (oldVersion < 112) {
        String templateDefaultAction = getPrefHandler().requireString(PrefKey.TEMPLATE_CLICK_DEFAULT,"SAVE");
        if (!(templateDefaultAction.equals("SAVE") || templateDefaultAction.equals("EDIT"))) {
          templateDefaultAction = "SAVE";
        }
        db.execSQL(String.format(Locale.ROOT, "ALTER TABLE templates add column default_action text not null check (default_action in ('SAVE', 'EDIT')) default '%s'", templateDefaultAction));
      }
      if (oldVersion < 114) {
        repairTransferUuids(db);
      }
      if (oldVersion < 115) {
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_CHANGES_EXTENDED);
        //add new change type
        db.execSQL("ALTER TABLE changes RENAME to changes_old");
        db.execSQL("CREATE TABLE changes ( account_id integer not null references accounts(_id) ON DELETE CASCADE, " +
            "type text not null check (type in ('created','updated','deleted','unsplit','metadata','link')), " +
            "sync_sequence_local integer, " +
            "uuid text not null," +
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
            "transfer_account integer references accounts(_id) ON DELETE SET NULL, " +
            "method_id integer references paymentmethods(_id) ON DELETE SET NULL, " +
            "cr_status text check (cr_status in ('UNRECONCILED','CLEARED','RECONCILED','VOID')), " +
            "number text," +
            "picture_id text)");
        db.execSQL("INSERT INTO changes " +
            "(account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, value_date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id)" +
            "SELECT account_id, type, sync_sequence_local, uuid, timestamp, parent_uuid, comment, date, value_date, amount, original_amount, original_currency, equivalent_amount, cat_id, payee_id, transfer_account, method_id, cr_status, number, picture_id FROM changes_old");
        db.execSQL("DROP TABLE changes_old");
        //db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
        //createOrRefreshTransactionTriggers(db);
        createOrRefreshAccountTriggers(db);
        createOrRefreshAccountMetadataTrigger(db);
      }
      if (oldVersion < 116) {
        db.execSQL("CREATE TABLE accounts_tags ( tag_id integer references tags(_id) ON DELETE CASCADE, account_id integer references accounts(_id) ON DELETE CASCADE, primary key (tag_id,account_id));");
      }
      if (oldVersion < 117) {
        upgradeTo117(db);
      }
      if (oldVersion < 118) {
        upgradeTo118(db);
      }
      if (oldVersion < 119) {
        upgradeTo119(db);
      }
      if (oldVersion < 120) {
        upgradeTo120(db);
      }
      //if (oldVersion < 121) {
        //createOrRefreshViews(db);
      //}
      if (oldVersion < 122) {
        upgradeTo122(db);
      }
/*      if (oldVersion < 123) {
        db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
        db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
      }*/
      if (oldVersion < 124) {
        upgradeTo124(db);
      }
      if (oldVersion < 125) {
        upgradeTo125(db);
      }
      if (oldVersion < 126) {
        upgradeTo126(db);
      }
      //if (oldVersion < 127) {
        //createOrRefreshViews(db);
      //}
      if (oldVersion < 128) {
        upgradeTo128(db);
      }
      if (oldVersion < 129) {
        upgradeTo129(db);
      }
      if (oldVersion < 130) {
        upgradeTo130(db);
      }
      if (oldVersion < 131) {
        upgradeTo131(db);
      }
/*      if (oldVersion < 132) {
        createOrRefreshViews(db);
      }*/
      if (oldVersion < 133) {
        upgradeTo133(db);
      }
/*      if (oldVersion < 134) {
        createOrRefreshViews(db);
      }*/
      if (oldVersion < 135) {
        db.execSQL("ALTER TABLE categories add column uuid text");
        db.execSQL("CREATE UNIQUE INDEX categories_uuid ON categories(uuid)");
        MoreDbUtilsKt.insertUuidsForDefaultCategories(db, MyApplication.Companion.getInstance().getResources());
      }
      if (oldVersion < 136) {
        upgradeTo136(db);
      }
      if (oldVersion < 137) {
        createOrRefreshCategoryHierarchyTrigger(db);
      }
      if (oldVersion < 138) {
        db.execSQL("ALTER TABLE templates add column debt_id integer references debts (_id) ON DELETE SET NULL");
      }
      if (oldVersion < 139) {
        db.execSQL("ALTER TABLE debts add column equivalent_amount integer");
      }
/*      if (oldVersion < 141) {
        createOrRefreshViews(db);
      }*/
      if (oldVersion < 142) {
        db.execSQL("ALTER TABLE paymentmethods add column icon text");
        //createOrRefreshViews(db);
      }
      if (oldVersion < 143) {
        db.execSQL("ALTER TABLE accounts add column sort_by text default 'date'");
        db.execSQL("ALTER TABLE currency add column sort_by text default 'date'");
      }
      if (oldVersion < 144) {
        //due to bug #1235, we got transactions with dates as milliseconds
        db.execSQL("UPDATE transactions set date = date / 1000 WHERE date > 10000000000");
        db.execSQL("UPDATE transactions set value_date = value_date / 1000 WHERE value_date > 10000000000");
      }
      if (oldVersion < 145) {
        upgradeTo145(db);
      }
      if (oldVersion < 146) {
        db.execSQL("ALTER TABLE payee add column short_name text");
        db.execSQL(PAYEE_UNIQUE_INDEX);
        //createOrRefreshViews(db);
      }
      if (oldVersion < 147) {
        db.execSQL("ALTER TABLE payee add column parent_id integer references payee(_id) ON DELETE CASCADE");
        db.execSQL("update payee set short_name = null where short_name = ''");
        db.execSQL(PARTY_HIERARCHY_TRIGGER);
      }
      if (oldVersion < 148) {
        upgradeTo148(db);
        //createOrRefreshTransactionTriggers(db);
      }
      if (oldVersion < 149) {
        createOrRefreshTransactionSealedTriggers(db);
      }
/*      if (oldVersion < 150) {
        createOrRefreshViews(db);
      }*/
      if (oldVersion < 151) {
        db.execSQL("ALTER TABLE categories add column type integer");
        db.execSQL("UPDATE categories set type = 3 where _id != 0");
        //createOrRefreshViews(db);
        createCategoryTypeTriggers(db);
      }

      if (oldVersion < 152) {
        repairWithSealedAccountsAndDebts(db, () ->  db.execSQL("update transactions set cr_status = (select cr_status from transactions parent where parent._id = transactions.parent_id) where parent_id is not null"));
        db.execSQL(SPLIT_PART_CR_STATUS_TRIGGER_CREATE);
      }

      if (oldVersion < 153) {
        db.execSQL("DROP TRIGGER IF EXISTS category_type_update_type_main");
        db.execSQL(CATEGORY_TYPE_UPDATE_TRIGGER_MAIN);
        db.execSQL("UPDATE categories SET type = (SELECT type FROM categories parent WHERE parent._id = categories.parent_id) WHERE parent_id IN (SELECT _id FROM categories WHERE parent_id IS NULL)");
      }

/*      if (oldVersion < 154) {
        createOrRefreshViews(db);
      }*/

      if (oldVersion < 155) {
        upgradeTo155(db);
      }

      if (oldVersion < 156) {
        upgradeTo156(db);
      }

      if (oldVersion < 157) {
        upgradeTo157(db);
      }

      if (oldVersion < 158) {
        upgradeTo158(db);
      }

      if (oldVersion < 159) {
        upgradeTo159(db);
      }

      if (oldVersion < 160) {
        upgradeTo160(db);
      }

      if (oldVersion < 161) {
        upgradeTo161(db);
      }

      if (oldVersion < 162) {
        db.execSQL("alter table tags add column color integer default null");
        //createOrRefreshViews(db);
      }

      if (oldVersion < 163) {
        upgradeTo163(db);
      }
      //If oldVersion < 145, then attribute is already configured on current enum values
      if (oldVersion >= 145 && oldVersion < 164) {
        upgradeTo164(db);
      }

      if (oldVersion < 165) {
        upgradeTo165(db);
      }

      if (oldVersion < 166) {
        db.execSQL("alter table banks add column version integer");
        db.execSQL("UPDATE banks set version = 1");
      }

      if (oldVersion >= 145 && oldVersion < 167) {
        upgradeTo167(db);
      }

      if (oldVersion < 168) {
        upgradeTo168(db);
      }

      if (oldVersion < 169) {
        upgradeTo169(db);
      }

      if (oldVersion < 170) {
        upgradeTo170(db);
      }

      if (oldVersion < 171) {
        upgradeTo171(db);
      }

      if (oldVersion < 172) {
        db.execSQL(TRANSACTIONS_PARENT_ID_INDEX);
      }

      if (oldVersion < 173) {
        upgradeTo173(db);
        createOrRefreshTransactionTriggers(db);
        createOrRefreshViews(db);
      }

      if (oldVersion < 174) {
        db.execSQL(getPRIORITIZED_PRICES_CREATE());
      }

      if (oldVersion < 175) {
       upgradeTo175(db);
      }

      if (oldVersion < 176) {
        createOrRefreshTemplateViews(db);
      }

      if (oldVersion < 177) {
        upgradeTo177(db);
      }

      if (oldVersion < 178) {
        upgradeTo178(db);
      }

      if (oldVersion < 179) {
        upgradeTo179(db);
      }

      if (oldVersion < 180) {
        upgradeTo180(db);
      }

      if (oldVersion < 181) {
        createOrRefreshViews(db);
      }

      TransactionProvider.resumeChangeTrigger(db);
    } catch (SQLException e) {
      throw new SQLiteUpgradeFailedException(oldVersion, newVersion, e);
    }
  }

  public void repairTransferUuids(SupportSQLiteDatabase db) {
    try {
      repairWithSealedAccounts(db, () -> db.execSQL("update transactions set uuid = (select uuid from transactions peer where peer._id=transactions.transfer_peer) where uuid is null and transfer_peer is not null;"));
    } catch (SQLException e) {
      CrashHandler.report(e);
    }
  }

  public void repairSplitPartDates(SupportSQLiteDatabase db) {
    repairWithSealedAccounts(db, () -> db.execSQL("UPDATE transactions set date = (select date from transactions parents where _id = transactions.parent_id) where parent_id is not null"));
  }

  private void createOrRefreshTransactionTriggers(SupportSQLiteDatabase db) {
    createOrRefreshChangeLogTriggers(db);
    createOrRefreshTransactionSealedTriggers(db);
  }


  private void createOrRefreshTransactionSealedTriggers(SupportSQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_insert");
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_tranfer_update");
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_delete");
    db.execSQL(TRANSACTIONS_SEALED_INSERT_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
    db.execSQL(TRANSFER_SEALED_UPDATE_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_SEALED_DELETE_TRIGGER_CREATE);
  }

  private void createOrRefreshViews(SupportSQLiteDatabase db) {
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_COMMITTED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_UNCOMMITTED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_ALL);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_CHANGES_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_WITH_ACCOUNT);

    String viewExtended = buildViewDefinitionExtended(TABLE_TRANSACTIONS);
    String tagGroupBy = DbConstantsKt.tagGroupBy(TABLE_TRANSACTIONS);
    String viewDefinition = buildViewDefinition(TABLE_TRANSACTIONS);
    db.execSQL("CREATE VIEW " + VIEW_COMMITTED + viewDefinition + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED  + tagGroupBy + ";");
    db.execSQL("CREATE VIEW " + VIEW_UNCOMMITTED + viewDefinition + " WHERE " + KEY_STATUS + " = " + STATUS_UNCOMMITTED + tagGroupBy + ";");
    db.execSQL("CREATE VIEW " + VIEW_ALL + viewExtended);
    db.execSQL("CREATE VIEW " + VIEW_EXTENDED + viewExtended + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED);

    db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
    db.execSQL(VIEW_WITH_ACCOUNT_DEFINITION);

    createOrRefreshTemplateViews(db);
  }

  private void createOrRefreshTemplateViews(SupportSQLiteDatabase db) {
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_ALL);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_UNCOMMITTED);

    String viewTemplates = buildViewDefinition(TABLE_TEMPLATES);
    String viewTemplatesExtended = buildViewDefinitionExtended(TABLE_TEMPLATES);
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_UNCOMMITTED + viewTemplates + " WHERE " + KEY_STATUS + " = " + STATUS_UNCOMMITTED + tagGroupBy(TABLE_TEMPLATES) + ";");
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_ALL + viewTemplatesExtended);
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_EXTENDED + viewTemplatesExtended + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
  }

  @Override
  public final void onDowngrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {
    throw new SQLiteDowngradeFailedException(oldVersion, newVersion);
  }

  public static class SQLiteDowngradeFailedException extends SQLiteException {
    SQLiteDowngradeFailedException(int oldVersion, int newVersion) {
      super(String.format(Locale.ROOT, "Downgrade not supported %d -> %d", oldVersion, newVersion));
    }
  }

  public static class SQLiteUpgradeFailedException extends SQLiteException {
    SQLiteUpgradeFailedException(int oldVersion, int newVersion, SQLException e) {
      super(String.format(Locale.ROOT, "Upgrade failed  %d -> %d", oldVersion, newVersion), e);
    }
  }
}
