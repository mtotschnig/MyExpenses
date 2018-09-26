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

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.android.calendar.CalendarContractCompat.Events;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.BudgetType;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY_OTHER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY_SELF;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_NORMALIZED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENT_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_EXECUTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_DIRECTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TIMESTAMP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTTYES_METHODS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_EXCHANGE_RATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGETS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGET_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CHANGES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_EVENT_CACHE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_METHODS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PLAN_INSTANCE_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_SETTINGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_STALE_URIS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_SYNC_STATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_ALL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_CHANGES_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_ALL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_UNCOMMITTED;
import static org.totschnig.myexpenses.util.ColorUtils.MAIN_COLORS;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public class TransactionDatabase extends SQLiteOpenHelper {
  public static final int DATABASE_VERSION = 78;
  private static final String DATABASE_NAME = "data";
  private Context mCtx;

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
          + KEY_CR_STATUS + " text not null check (" + KEY_CR_STATUS + " in (" + Transaction.CrStatus.JOIN + ")) default '" + Transaction.CrStatus.RECONCILED.name() + "',"
          + KEY_REFERENCE_NUMBER + " text, "
          + KEY_PICTURE_URI + " text, "
          + KEY_UUID + " text, "
          + KEY_ORIGINAL_AMOUNT + " integer, "
          + KEY_ORIGINAL_CURRENCY + " text, "
          + KEY_EQUIVALENT_AMOUNT + " integer);";

  private static final String TRANSACTIONS_UUID_INDEX_CREATE = "CREATE UNIQUE INDEX transactions_account_uuid_index ON "
      + TABLE_TRANSACTIONS + "(" + KEY_UUID + "," + KEY_ACCOUNTID + "," + KEY_STATUS + ")";

  private static String buildViewDefinition(String tableName) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(" AS SELECT ").append(tableName).append(".*, ").append(TABLE_PAYEES)
        .append(".").append(KEY_PAYEE_NAME).append(", ")
        .append(TABLE_METHODS).append(".").append(KEY_LABEL).append(" AS ").append(KEY_METHOD_LABEL);

    if (tableName.equals(TABLE_TRANSACTIONS)) {
      stringBuilder.append(", ").append(TABLE_PLAN_INSTANCE_STATUS).append(".").append(KEY_TEMPLATEID);
    }

    stringBuilder.append(" FROM ").append(tableName).append(" LEFT JOIN ").append(TABLE_PAYEES).append(" ON ")
        .append(KEY_PAYEEID).append(" = ").append(TABLE_PAYEES).append(".").append(KEY_ROWID).append(" LEFT JOIN ")
        .append(TABLE_METHODS).append(" ON ").append(KEY_METHODID).append(" = ").append(TABLE_METHODS)
        .append(".").append(KEY_ROWID);

    if (tableName.equals(TABLE_TRANSACTIONS)) {
      stringBuilder.append(" LEFT JOIN ").append(TABLE_PLAN_INSTANCE_STATUS)
          .append(" ON ").append(tableName).append(".").append(KEY_ROWID).append(" = ")
          .append(TABLE_PLAN_INSTANCE_STATUS).append(".").append(KEY_TRANSACTIONID);
    }
    return stringBuilder.toString();
  }

  private static String buildViewDefinitionExtended(String tableName) {
    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append(" AS SELECT ").append(tableName).append(".*, ").append(TABLE_PAYEES)
        .append(".").append(KEY_PAYEE_NAME).append(", ")
        .append(TABLE_METHODS).append(".").append(KEY_LABEL).append(" AS ").append(KEY_METHOD_LABEL);

    if (!tableName.equals(TABLE_CHANGES)) {
      stringBuilder.append(", ")
          .append(KEY_COLOR).append(", ")
          .append(KEY_CURRENCY).append(", ")
          .append(KEY_EXCLUDE_FROM_TOTALS).append(", ")
          .append(TABLE_ACCOUNTS).append(".").append(KEY_LABEL).append(" AS ").append(KEY_ACCOUNT_LABEL);
    }

    if (tableName.equals(TABLE_TRANSACTIONS)) {
      stringBuilder.append(", ").append(TABLE_PLAN_INSTANCE_STATUS).append(".").append(KEY_TEMPLATEID);
    }

    stringBuilder.append(" FROM ").append(tableName).append(" LEFT JOIN ").append(TABLE_PAYEES).append(" ON ")
        .append(KEY_PAYEEID).append(" = ").append(TABLE_PAYEES).append(".").append(KEY_ROWID)
        .append(" LEFT JOIN ")
        .append(TABLE_METHODS).append(" ON ").append(KEY_METHODID).append(" = ").append(TABLE_METHODS)
        .append(".").append(KEY_ROWID);

    if (!tableName.equals(TABLE_CHANGES)) {
      stringBuilder.append(" LEFT JOIN ").append(TABLE_ACCOUNTS).append(" ON ").append(KEY_ACCOUNTID)
          .append(" = ").append(TABLE_ACCOUNTS).append(".").append(KEY_ROWID);
    }

    if (tableName.equals(TABLE_TRANSACTIONS)) {
      stringBuilder.append(" LEFT JOIN ").append(TABLE_PLAN_INSTANCE_STATUS)
          .append(" ON ").append(tableName).append(".").append(KEY_ROWID).append(" = ")
          .append(TABLE_PLAN_INSTANCE_STATUS).append(".").append(KEY_TRANSACTIONID);
    }

    return stringBuilder.toString();
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
          + KEY_CURRENCY + " text not null, "
          + KEY_TYPE + " text not null check (" + KEY_TYPE + " in (" + AccountType.JOIN + ")) default '" + AccountType.CASH.name() + "', "
          + KEY_COLOR + " integer default -3355444, "
          + KEY_GROUPING + " text not null check (" + KEY_GROUPING + " in (" + Grouping.JOIN + ")) default '" + Grouping.NONE.name() + "', "
          + KEY_USAGES + " integer default 0,"
          + KEY_LAST_USED + " datetime, "
          + KEY_SORT_KEY + " integer, "
          + KEY_SYNC_ACCOUNT_NAME + " text, "
          + KEY_SYNC_SEQUENCE_LOCAL + " integer default 0,"
          + KEY_EXCLUDE_FROM_TOTALS + " boolean default 0, "
          + KEY_UUID + " text, "
          + KEY_SORT_DIRECTION + " text not null check (" + KEY_SORT_DIRECTION + " in ('ASC','DESC')) default 'DESC',"
          + KEY_CRITERION + " integer);";

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
          + KEY_PARENTID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + "), "
          + KEY_USAGES + " integer default 0, "
          + KEY_LAST_USED + " datetime, "
          + KEY_COLOR + " integer, "
          + "UNIQUE (" + KEY_LABEL + "," + KEY_PARENTID + "));";

  private static final String PAYMENT_METHODS_CREATE =
      "CREATE TABLE " + TABLE_METHODS + " ("
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_LABEL + " text not null, "
          + KEY_IS_NUMBERED + " boolean default 0, "
          + KEY_TYPE + " integer " +
          "check (" + KEY_TYPE + " in ("
          + PaymentMethod.EXPENSE + ","
          + PaymentMethod.NEUTRAL + ","
          + PaymentMethod.INCOME + ")) default 0);";

  private static final String ACCOUNTTYE_METHOD_CREATE =
      "CREATE TABLE " + TABLE_ACCOUNTTYES_METHODS + " ("
          + KEY_TYPE + " text not null check (" + KEY_TYPE + " in (" + AccountType.JOIN + ")), "
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
          + KEY_LAST_USED + " datetime,"
          + KEY_PARENTID + " integer references " + TABLE_TEMPLATES + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_STATUS + " integer default 0);";

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


  /**
   * stores payees and payers
   * this table is used for populating the autocompleting text field,
   */
  private static final String PAYEE_CREATE =
      "CREATE TABLE " + TABLE_PAYEES
          + " (" + KEY_ROWID + " integer primary key autoincrement, " +
          KEY_PAYEE_NAME + " text UNIQUE not null," +
          KEY_PAYEE_NAME_NORMALIZED + " text);";

  private static final String CURRENCY_CREATE =
      "CREATE TABLE " + TABLE_CURRENCIES
          + " (" + KEY_ROWID + " integer primary key autoincrement, " + KEY_CODE
          + " text UNIQUE not null);";

  /**
   * in this table we store links between plan instances and transactions,
   * thus allowing us to track if an instance has been applied, and to allow editing or cancellation of
   * transactions added from plan instances
   */
  private static final String PLAN_INSTANCE_STATUS_CREATE =
      "CREATE TABLE " + TABLE_PLAN_INSTANCE_STATUS
          + " ( " + KEY_TEMPLATEID + " integer references " + TABLE_TEMPLATES + "(" + KEY_ROWID + ") ON DELETE CASCADE," +
          KEY_INSTANCEID + " integer," + // NO LONGER references Instances._ID in calendar content provider; instanceId is calculated from day
          KEY_TRANSACTIONID + " integer UNIQUE references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + ") ON DELETE CASCADE);";

  private static final String STALE_URIS_CREATE =
      "CREATE TABLE " + TABLE_STALE_URIS
          + " ( " + KEY_PICTURE_URI + " text);";

  private static final String STALE_URI_TRIGGER_CREATE =
      "CREATE TRIGGER cache_stale_uri " +
          "AFTER DELETE ON " + TABLE_TRANSACTIONS + " " +
          "WHEN old." + KEY_PICTURE_URI + " NOT NULL " +
          "AND NOT EXISTS " +
          "(SELECT 1 FROM " + TABLE_TRANSACTIONS + " " +
          "WHERE " + KEY_PICTURE_URI + " = old." + KEY_PICTURE_URI + ") " +
          "BEGIN INSERT INTO " + TABLE_STALE_URIS + " VALUES (old." + KEY_PICTURE_URI + "); END";

  private static final String ACCOUNTS_TRIGGER_CREATE =
      "CREATE TRIGGER sort_key_default " +
          "AFTER INSERT ON " + TABLE_ACCOUNTS + " " +
          "BEGIN UPDATE " + TABLE_ACCOUNTS + " SET " + KEY_SORT_KEY +
          " = (SELECT coalesce(max(" + KEY_SORT_KEY + "),0) FROM " + TABLE_ACCOUNTS + ") + 1 WHERE " +
          KEY_ROWID + " = NEW." + KEY_ROWID + "; END";

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
          + KEY_METHODID + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + "),"
          + KEY_CR_STATUS + " text check (" + KEY_CR_STATUS + " in (" + Transaction.CrStatus.JOIN + ")),"
          + KEY_REFERENCE_NUMBER + " text, "
          + KEY_PICTURE_URI + " text);";

  private static final String BUDGETS_CREATE =
      "CREATE TABLE " + TABLE_BUDGETS + " ( "
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_LABEL + " text not null, "
          + KEY_TYPE + " text not null check (" + KEY_TYPE + " in (" + BudgetType.JOIN + ")), "
          + KEY_AMOUNT + " integer not null, "
          + KEY_ACCOUNTID + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_CURRENCY + " text not null)";

  private static final String BUDGETS_CATEGORY_CREATE =
      "CREATE TABLE " + TABLE_BUDGET_CATEGORIES + " ( "
          + KEY_BUDGETID + " integer references " + TABLE_BUDGETS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_CATID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + "), "
          + KEY_AMOUNT + " integer not null)";


  private static final String SELECT_SEQUCENE_NUMBER_TEMLATE = "(SELECT " + KEY_SYNC_SEQUENCE_LOCAL + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = %s." + KEY_ACCOUNTID + ")";
  private static final String SELECT_PARENT_UUID_TEMPLATE = "CASE WHEN %1$s." + KEY_PARENTID + " IS NULL THEN NULL ELSE (SELECT " + KEY_UUID + " from " + TABLE_TRANSACTIONS + " where " + KEY_ROWID + " = %1$s." + KEY_PARENTID + ") END";

  private static final String INSERT_TRIGGER_ACTION = " BEGIN INSERT INTO " + TABLE_CHANGES + "("
      + KEY_TYPE + ","
      + KEY_SYNC_SEQUENCE_LOCAL + ", "
      + KEY_UUID + ", "
      + KEY_PARENT_UUID + ", "
      + KEY_COMMENT + ", "
      + KEY_DATE + ", "
      + KEY_VALUE_DATE + ", "
      + KEY_AMOUNT + ", "
      + KEY_ORIGINAL_AMOUNT + ", "
      + KEY_ORIGINAL_CURRENCY + ", "
      + KEY_EQUIVALENT_AMOUNT + ", "
      + KEY_CATID + ", "
      + KEY_ACCOUNTID + ","
      + KEY_PAYEEID + ", "
      + KEY_TRANSFER_ACCOUNT + ", "
      + KEY_METHODID + ","
      + KEY_CR_STATUS + ", "
      + KEY_REFERENCE_NUMBER + ", "
      + KEY_PICTURE_URI + ") VALUES ('" + TransactionChange.Type.created + "', "
      + String.format(Locale.US, SELECT_SEQUCENE_NUMBER_TEMLATE, "new") + ", "
      + "new." + KEY_UUID + ", "
      + String.format(Locale.US, SELECT_PARENT_UUID_TEMPLATE, "new") + ", "
      + "new." + KEY_COMMENT + ", "
      + "new." + KEY_DATE + ", "
      + "new." + KEY_VALUE_DATE + ", "
      + "new." + KEY_AMOUNT + ", "
      + "new." + KEY_ORIGINAL_AMOUNT + ", "
      + "new." + KEY_ORIGINAL_CURRENCY + ", "
      + "new." + KEY_EQUIVALENT_AMOUNT + ", "
      + "new." + KEY_CATID + ", "
      + "new." + KEY_ACCOUNTID + ", "
      + "new." + KEY_PAYEEID + ", "
      + "new." + KEY_TRANSFER_ACCOUNT + ", "
      + "new." + KEY_METHODID + ", "
      + "new." + KEY_CR_STATUS + ", "
      + "new." + KEY_REFERENCE_NUMBER + ", "
      + "new." + KEY_PICTURE_URI + "); END;";

  private static final String DELETE_TRIGGER_ACTION = " BEGIN INSERT INTO " + TABLE_CHANGES + "("
      + KEY_TYPE + ","
      + KEY_SYNC_SEQUENCE_LOCAL + ", "
      + KEY_ACCOUNTID + ","
      + KEY_UUID + ","
      + KEY_PARENT_UUID + ") VALUES ('" + TransactionChange.Type.deleted + "', "
      + String.format(Locale.US, SELECT_SEQUCENE_NUMBER_TEMLATE, "old") + ", "
      + "old." + KEY_ACCOUNTID + ", "
      + "old." + KEY_UUID + ", "
      + String.format(Locale.US, SELECT_PARENT_UUID_TEMPLATE, "old") + "); END;";

  private static final String DELETE_TRIGGER_ACTION_AFTER_TRANSFER_UPDATE = " BEGIN INSERT INTO " + TABLE_CHANGES + "("
      + KEY_TYPE + ","
      + KEY_SYNC_SEQUENCE_LOCAL + ", "
      + KEY_ACCOUNTID + ","
      + KEY_UUID + ","
      + KEY_PARENT_UUID + ") VALUES ('" + TransactionChange.Type.deleted + "', "
      + String.format(Locale.US, SELECT_SEQUCENE_NUMBER_TEMLATE, "old") + ", "
      + "old." + KEY_ACCOUNTID + ", "
      + "new." + KEY_UUID + ", "
      + String.format(Locale.US, SELECT_PARENT_UUID_TEMPLATE, "old") + "); END;";

  private static final String SHOULD_WRITE_CHANGE_TEMPLATE = " EXISTS (SELECT 1 FROM " + TABLE_ACCOUNTS
      + " WHERE " + KEY_ROWID + " = %s." + KEY_ACCOUNTID + " AND " + KEY_SYNC_ACCOUNT_NAME + " IS NOT NULL AND "
      + KEY_SYNC_SEQUENCE_LOCAL + " > 0) AND NOT EXISTS (SELECT 1 FROM " + TABLE_SYNC_STATE + ")";

  private static final String TRANSACTIONS_INSERT_TRIGGER_CREATE =
      "CREATE TRIGGER insert_change_log "
          + "AFTER INSERT ON " + TABLE_TRANSACTIONS
          + " WHEN " + String.format(Locale.US, SHOULD_WRITE_CHANGE_TEMPLATE, "new")
          + " AND new." + KEY_STATUS + " != " + STATUS_UNCOMMITTED
          + INSERT_TRIGGER_ACTION;

  private static final String TRANSACTIONS_INSERT_AFTER_UPDATE_TRIGGER_CREATE =
      "CREATE TRIGGER insert_after_update_change_log "
          + "AFTER UPDATE ON " + TABLE_TRANSACTIONS
          + " WHEN " + String.format(Locale.US, SHOULD_WRITE_CHANGE_TEMPLATE, "new")
          + " AND ((old." + KEY_STATUS + " = " + STATUS_UNCOMMITTED + " AND new." + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ")"
          + " OR (old." + KEY_ACCOUNTID + " != new." + KEY_ACCOUNTID + " AND new." + KEY_STATUS + " != " + STATUS_UNCOMMITTED + "))"
          + INSERT_TRIGGER_ACTION;

  private static final String TRANSACTIONS_DELETE_AFTER_UPDATE_TRIGGER_CREATE =
      "CREATE TRIGGER delete_after_update_change_log "
          + "AFTER UPDATE ON " + TABLE_TRANSACTIONS
          + " WHEN " + String.format(Locale.US, SHOULD_WRITE_CHANGE_TEMPLATE, "old")
          + " AND old." + KEY_ACCOUNTID + " != new." + KEY_ACCOUNTID + " AND new." + KEY_STATUS + " != " + STATUS_UNCOMMITTED
          + DELETE_TRIGGER_ACTION_AFTER_TRANSFER_UPDATE;

  private static final String TRANSACTIONS_DELETE_TRIGGER_CREATE =
      "CREATE TRIGGER delete_change_log "
          + "AFTER DELETE ON " + TABLE_TRANSACTIONS
          + " WHEN " + String.format(Locale.US, SHOULD_WRITE_CHANGE_TEMPLATE, "old")
          + " AND old." + KEY_STATUS + " != " + STATUS_UNCOMMITTED + " AND EXISTS (SELECT 1 FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = old." + KEY_ACCOUNTID + ")"
          + DELETE_TRIGGER_ACTION;

  private static String buildChangeTriggerDefinitionForColumn(String column) {
    return "CASE WHEN old." + column + " = new." + column + " THEN NULL ELSE new." + column + " END";
  }

  private static final String TRANSACTIONS_UPDATE_TRIGGER_CREATE =
      "CREATE TRIGGER update_change_log "
          + "AFTER UPDATE ON " + TABLE_TRANSACTIONS
          + " WHEN " + String.format(Locale.US, SHOULD_WRITE_CHANGE_TEMPLATE, "old")
          + " AND old." + KEY_STATUS + " != " + STATUS_UNCOMMITTED
          + " AND new." + KEY_STATUS + " != " + STATUS_UNCOMMITTED
          + " AND new." + KEY_ACCOUNTID + " = old." + KEY_ACCOUNTID //if account is changed, we need to delete transaction from one account, and add it to the other
          + " AND new." + KEY_TRANSFER_PEER + " IS old." + KEY_TRANSFER_PEER //if a new transfer is inserted, the first peer is updated, after second one is added, and we can skip this update here
          + " AND new." + KEY_UUID + " IS NOT NULL "  //during transfer update, uuid is temporarily set to null, we need to skip this change here, otherwise we run into SQLiteConstraintException
          + " BEGIN INSERT INTO " + TABLE_CHANGES + "("
          + KEY_TYPE + ","
          + KEY_SYNC_SEQUENCE_LOCAL + ", "
          + KEY_UUID + ", "
          + KEY_ACCOUNTID + ", "
          + KEY_PARENT_UUID + ", "
          + KEY_COMMENT + ", "
          + KEY_DATE + ", "
          + KEY_VALUE_DATE + ", "
          + KEY_AMOUNT + ", "
          + KEY_ORIGINAL_AMOUNT + ", "
          + KEY_ORIGINAL_CURRENCY + ", "
          + KEY_EQUIVALENT_AMOUNT + ", "
          + KEY_CATID + ", "
          + KEY_PAYEEID + ", "
          + KEY_TRANSFER_ACCOUNT + ", "
          + KEY_METHODID + ", "
          + KEY_CR_STATUS + ", "
          + KEY_REFERENCE_NUMBER + ", "
          + KEY_PICTURE_URI + ") VALUES ('" + TransactionChange.Type.updated + "', "
          + String.format(Locale.US, SELECT_SEQUCENE_NUMBER_TEMLATE, "old") + ", "
          + "new." + KEY_UUID + ", "
          + "new." + KEY_ACCOUNTID + ", "
          + String.format(Locale.US, SELECT_PARENT_UUID_TEMPLATE, "new") + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_COMMENT) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_DATE) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_VALUE_DATE) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_AMOUNT) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_ORIGINAL_AMOUNT) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_ORIGINAL_CURRENCY) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_EQUIVALENT_AMOUNT) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_CATID) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_PAYEEID) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_TRANSFER_ACCOUNT) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_METHODID) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_CR_STATUS) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_REFERENCE_NUMBER) + ", "
          + buildChangeTriggerDefinitionForColumn(KEY_PICTURE_URI) + "); END;";


  private static final String INCREASE_CATEGORY_USAGE_ACTION = " BEGIN UPDATE " + TABLE_CATEGORIES + " SET " + KEY_USAGES + " = " +
      KEY_USAGES + " + 1, " + KEY_LAST_USED + " = strftime('%s', 'now')  WHERE " + KEY_ROWID +
      " IN (new." + KEY_CATID + " , (SELECT " + KEY_PARENTID +
      " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = new." + KEY_CATID + ")); END;";

  private static final String INCREASE_CATEGORY_USAGE_INSERT_TRIGGER = "CREATE TRIGGER insert_increase_category_usage "
      + "AFTER INSERT ON " + TABLE_TRANSACTIONS
      + " WHEN new." + KEY_CATID + " IS NOT NULL AND new." + KEY_CATID + " != " + SPLIT_CATID + ""
      + INCREASE_CATEGORY_USAGE_ACTION;


  private static final String INCREASE_CATEGORY_USAGE_UPDATE_TRIGGER = "CREATE TRIGGER update_increase_category_usage "
      + "AFTER UPDATE ON " + TABLE_TRANSACTIONS
      + " WHEN new." + KEY_CATID + " IS NOT NULL AND (old." + KEY_CATID + " IS NULL OR new." + KEY_CATID + " != old." + KEY_CATID + ")"
      + INCREASE_CATEGORY_USAGE_ACTION;

  private static final String INCREASE_ACCOUNT_USAGE_ACTION = " BEGIN UPDATE " + TABLE_ACCOUNTS + " SET " + KEY_USAGES + " = " +
      KEY_USAGES + " + 1, " + KEY_LAST_USED + " = strftime('%s', 'now')  WHERE " + KEY_ROWID +
      " = new." + KEY_ACCOUNTID + "; END;";

  private static final String INCREASE_ACCOUNT_USAGE_INSERT_TRIGGER = "CREATE TRIGGER insert_increase_account_usage "
      + "AFTER INSERT ON " + TABLE_TRANSACTIONS
      + " WHEN new." + KEY_PARENTID + " IS NULL"
      + INCREASE_ACCOUNT_USAGE_ACTION;

  private static final String INCREASE_ACCOUNT_USAGE_UPDATE_TRIGGER = "CREATE TRIGGER update_increase_account_usage "
      + "AFTER UPDATE ON " + TABLE_TRANSACTIONS
      + " WHEN new." + KEY_PARENTID + " IS NULL AND new." + KEY_ACCOUNTID + " != old." + KEY_ACCOUNTID + " AND (old." + KEY_TRANSFER_ACCOUNT + " IS NULL OR new." + KEY_ACCOUNTID + " != old." + KEY_TRANSFER_ACCOUNT + ")"
      + INCREASE_ACCOUNT_USAGE_ACTION;

  private static final String UPDATE_ACCOUNT_SYNC_NULL_TRIGGER = "CREATE TRIGGER update_account_sync_null "
      + "AFTER UPDATE ON " + TABLE_ACCOUNTS
      + " WHEN new." + KEY_SYNC_ACCOUNT_NAME + " IS NULL AND old." + KEY_SYNC_ACCOUNT_NAME + " IS NOT NULL "
      + "BEGIN "
      + "UPDATE " + TABLE_ACCOUNTS + " SET " + KEY_SYNC_SEQUENCE_LOCAL + " = 0 WHERE " + KEY_ROWID + " = old." + KEY_ROWID + "; "
      + "DELETE FROM " + TABLE_CHANGES + " WHERE " + KEY_ACCOUNTID + " = old." + KEY_ROWID + "; "
      + "END;";

  private static final String SETTINGS_CREATE =
      "CREATE TABLE " + TABLE_SETTINGS + " ("
          + KEY_KEY + " text unique not null, "
          + KEY_VALUE + " text);";

  public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
  public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

  TransactionDatabase(Context context) {
    super(context, getDbName(), null, DATABASE_VERSION);
    mCtx = context;
    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      setWriteAheadLoggingEnabled(true);
    }*/
  }

  public static String getDbName() {
    return MyApplication.isInstrumentationTest() ? MyApplication.getTestId() : DATABASE_NAME;
  }

  @Override
  public void onOpen(SQLiteDatabase db) {
    super.onOpen(db);
    //since API 16 we could use onConfigure to enable foreign keys
    //which is run before onUpgrade
    //but this makes upgrades more difficult, since then you have to maintain the constraint in
    //each step of a multi statement upgrade with table rename
    //we stick to doing upgrades with foreign keys disabled which forces us
    //to take care of ensuring consistency during upgrades
    if (!db.isReadOnly()) {
      db.execSQL("PRAGMA foreign_keys=ON;");
    }
    try {
      db.delete(TABLE_TRANSACTIONS, KEY_STATUS + " = " + STATUS_UNCOMMITTED, null);
    } catch (SQLiteException e) {
      CrashHandler.report(e);
    }
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(DATABASE_CREATE);
    db.execSQL(TRANSACTIONS_UUID_INDEX_CREATE);
    db.execSQL(PAYEE_CREATE);
    db.execSQL(PAYMENT_METHODS_CREATE);
    db.execSQL(TEMPLATE_CREATE);
    db.execSQL(PLAN_INSTANCE_STATUS_CREATE);
    db.execSQL(CATEGORIES_CREATE);
    db.execSQL(ACCOUNTS_CREATE);
    db.execSQL(ACCOUNTS_UUID_INDEX_CREATE);
    db.execSQL(SYNC_STATE_CREATE);
    db.execSQL(ACCOUNTTYE_METHOD_CREATE);
    insertDefaultPaymentMethods(db);
    db.execSQL(CURRENCY_CREATE);
    //category for splits needed to honour foreign constraint
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_ROWID, SPLIT_CATID);
    initialValues.put(KEY_PARENTID, SPLIT_CATID);
    initialValues.put(KEY_LABEL, "__SPLIT_TRANSACTION__");
    db.insertOrThrow(TABLE_CATEGORIES, null, initialValues);
    insertCurrencies(db);
    db.execSQL(EVENT_CACHE_CREATE);
    db.execSQL(STALE_URIS_CREATE);
    db.execSQL(STALE_URI_TRIGGER_CREATE);
    db.execSQL(CHANGES_CREATE);

    //Index
    db.execSQL("CREATE INDEX transactions_cat_id_index on " + TABLE_TRANSACTIONS + "(" + KEY_CATID + ")");
    db.execSQL("CREATE INDEX templates_cat_id_index on " + TABLE_TEMPLATES + "(" + KEY_CATID + ")");

    //Views
    createOrRefreshViews(db);

    // Triggers
    createOrRefreshChangelogTriggers(db);
    db.execSQL(INCREASE_CATEGORY_USAGE_INSERT_TRIGGER);
    db.execSQL(INCREASE_CATEGORY_USAGE_UPDATE_TRIGGER);
    db.execSQL(INCREASE_ACCOUNT_USAGE_INSERT_TRIGGER);
    db.execSQL(INCREASE_ACCOUNT_USAGE_UPDATE_TRIGGER);
    createOrRefreshAccountTriggers(db);
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
  }

  private void insertCurrencies(SQLiteDatabase db) {
    ContentValues initialValues = new ContentValues();
    for (CurrencyEnum currency : CurrencyEnum.values()) {
      initialValues.put(KEY_CODE, currency.name());
      db.insert(TABLE_CURRENCIES, null, initialValues);
    }
  }

  /**
   * @param db insert the predefined payment methods in the database, all of them are valid only for bank accounts
   */
  private void insertDefaultPaymentMethods(SQLiteDatabase db) {
    ContentValues initialValues;
    long _id;
    for (PaymentMethod.PreDefined pm : PaymentMethod.PreDefined.values()) {
      initialValues = new ContentValues();
      initialValues.put(KEY_LABEL, pm.name());
      initialValues.put(KEY_TYPE, pm.paymentType);
      initialValues.put(KEY_IS_NUMBERED, pm.isNumbered);
      _id = db.insert(TABLE_METHODS, null, initialValues);
      initialValues = new ContentValues();
      initialValues.put(KEY_METHODID, _id);
      initialValues.put(KEY_TYPE, "BANK");
      db.insert(TABLE_ACCOUNTTYES_METHODS, null, initialValues);
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
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
        for (PaymentMethod.PreDefined pm : PaymentMethod.PreDefined.values()) {
          initialValues = new ContentValues();
          initialValues.put("label", pm.name());
          initialValues.put("type", pm.paymentType);
          _id = db.insert("paymentmethods", null, initialValues);
          initialValues = new ContentValues();
          initialValues.put("method_id", _id);
          initialValues.put("type", "BANK");
          db.insert("accounttype_paymentmethod", null, initialValues);
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
        db.insert("categories", null, initialValues);
      }

      if (oldVersion < 31) {
        //in an alpha version distributed on Google Play, we had SPLIT_CATID as -1
        ContentValues initialValues = new ContentValues();
        initialValues.put("_id", 0);
        initialValues.put("parent_id", 0);
        db.update("categories", initialValues, "_id=-1", null);
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
        db.update("paymentmethods", initialValues, "label = ?", new String[]{"CHEQUE"});
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
        Cursor c = db.query("payee", new String[]{"_id", "name"}, null, null, null, null, null);
        if (c != null) {
          if (c.moveToFirst()) {
            ContentValues v = new ContentValues();
            while (c.getPosition() < c.getCount()) {
              v.put("name_normalized", Utils.normalize(c.getString(1)));
              db.update("payee", v, "_id = " + c.getLong(0), null);
              c.moveToNext();
            }
          }
          c.close();
        }
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
          SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
          qb.setTables("templates LEFT JOIN payee ON payee_id = payee._id" +
              " LEFT JOIN accounts ON account_id = accounts._id" +
              " LEFT JOIN paymentmethods ON method_id = paymentmethods._id");
          Cursor c = qb.query(db, projection, null, null, null, null, null);
          if (c != null) {
            if (c.moveToFirst()) {
              ContentValues templateValues = new ContentValues();
              while (c.getPosition() < c.getCount()) {
                templateValues.put("uuid", Model.generateUuid());
                long templateId = c.getLong(c.getColumnIndex("_id"));
                db.update("templates", templateValues, "_id = " + templateId, null);
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
        File pictureDir = PictureDirHelper.getPictureDir(false);
        //fallback if not mounted
        if (pictureDir == null) {
          pictureDir = new File(
              Environment.getExternalStorageDirectory().getPath() +
                  "/Android/data/" + MyApplication.getInstance().getPackageName() + "/files",
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
        Cursor c = db.query("categories", new String[]{"_id", "label"}, null, null, null, null, null);
        if (c != null) {
          if (c.moveToFirst()) {
            ContentValues v = new ContentValues();
            while (c.getPosition() < c.getCount()) {
              v.put("label_normalized", Utils.normalize(c.getString(1)));
              db.update("categories", v, "_id = " + c.getLong(0), null);
              c.moveToNext();
            }
          }
          c.close();
        }
      }

      if (oldVersion < 56) {
        db.execSQL("ALTER TABLE templates add column last_used datetime");
        db.execSQL("ALTER TABLE categories add column last_used datetime");
        db.execSQL("ALTER TABLE accounts add column last_used datetime");
//        db.execSQL("CREATE TRIGGER sort_key_default AFTER INSERT ON accounts " +
//            "BEGIN UPDATE accounts SET sort_key = (SELECT coalesce(max(sort_key),0) FROM accounts) + 1 " +
//            "WHERE _id = NEW._id; END");
        //The sort key could be set by user in previous versions, now it is handled internally
        Cursor c = db.query("accounts", new String[]{"_id", "sort_key"}, null, null, null, null, "sort_key ASC");
        boolean hasAccountSortKeySet = false;
        if (c != null) {
          if (c.moveToFirst()) {
            ContentValues v = new ContentValues();
            while (c.getPosition() < c.getCount()) {
              v.put("sort_key", c.getPosition() + 1);
              db.update("accounts", v, "_id = ?", new String[]{c.getString(0)});
              if (c.getInt(1) != 0) hasAccountSortKeySet = true;
              c.moveToNext();
            }
          }
          c.close();
        }
        String legacy = PrefKey.SORT_ORDER_LEGACY.getString("USAGES");
        PrefKey.SORT_ORDER_TEMPLATES.putString(legacy);
        PrefKey.SORT_ORDER_CATEGORIES.putString(legacy);
        PrefKey.SORT_ORDER_ACCOUNTS.putString(hasAccountSortKeySet ? "CUSTOM" : legacy);
        PrefKey.SORT_ORDER_LEGACY.remove();
      }

      if (oldVersion < 57) {
        //fix custom app uris
        try {
          if (CALENDAR.hasPermission(mCtx)) {
            Cursor c = db.query("templates", new String[]{"_id", "plan_id"}, "plan_id IS NOT null", null, null, null, null);
            if (c != null) {
              if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                  Plan.updateCustomAppUri(c.getLong(1), Template.buildCustomAppUri(c.getLong(0)));
                  c.moveToNext();
                }
              }
              c.close();
            }
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
        Cursor c = db.rawQuery("SELECT distinct currency from accounts", null);
        if (c != null) {
          if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
              Money.ensureFractionDigitsAreCached(Utils.getSaveInstance(c.getString(0)));
              c.moveToNext();
            }
          }
          c.close();
        }
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

      if (oldVersion < 62) {
        //refreshViewsExtended(db);
      }

      if (oldVersion < 63) {
        db.execSQL("CREATE TABLE _sync_state (status integer)");
        //createOrRefreshChangelogTriggers(db);
      }

      if (oldVersion < 64) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("code", CurrencyEnum.BYN.name());
        //will log SQLiteConstraintException if value already exists in table
        db.insert("currency", null, initialValues);
      }

      if (oldVersion < 65) {
        if (DistribHelper.shouldUseAndroidPlatformCalendar()) {
          //unfortunately we have to drop information about canceled instances
          db.delete("planinstance_transaction", "transaction_id is null", null);
          //we update instance_id to negative numbers, in order to prevent Conflict, which would araise
          //in the rare case where an existing instance_id equals a newly calculated one
          db.execSQL("update planinstance_transaction set instance_id = - rowid");
          Cursor c = db.rawQuery("SELECT rowid, (SELECT date from transactions where _id = transaction_id) FROM planinstance_transaction", null);
          if (c != null) {
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
                  db.update("planinstance_transaction", v, whereClause, whereArgs);
                } catch (Exception e) {
                  CrashHandler.report(e);
                }
                c.moveToNext();
              }
            }
            c.close();
          }
        }
      }

      if (oldVersion < 66) {
        db.execSQL(String.format("CREATE TABLE %s (%s text unique not null, %s text unique not null);", "settings", "key", "value"));
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
        createOrRefreshViews(db);

        db.execSQL("CREATE TRIGGER protect_split_transaction BEFORE DELETE ON categories " +
            " WHEN (OLD._id = 0)" +
            " BEGIN SELECT RAISE (FAIL, 'split category can not be deleted'); " +
            " END;");
      }
      if (oldVersion < 69) {
        //repair missed trigger recreation
        createOrRefreshAccountTriggers(db);
        //while trigger was not set new accounts were added without sort key leading to crash
        //https://github.com/mtotschnig/MyExpenses/issues/420
        //we now set sort_key again for all accounts trying to preserve existing order
        Cursor c = db.query("accounts", new String[]{"_id"}, null, null, null, null, "sort_key ASC");
        if (c != null) {
          if (c.moveToFirst()) {
            ContentValues v = new ContentValues();
            while (c.getPosition() < c.getCount()) {
              v.put("sort_key", c.getPosition() + 1);
              db.update("accounts", v, "_id = ?", new String[]{c.getString(0)});
              c.moveToNext();
            }
          }
          c.close();
        }
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
        createOrRefreshChangelogTriggers(db);
      }
      if (oldVersion < 74) {
        //repair transfers that have not been synced correctly
        db.execSQL("update transactions set transfer_peer = (select _id from transactions peer where peer.transfer_peer = transactions._id) where transfer_peer is null;");
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
        Cursor c = db.query("categories", new String[]{"_id"}, "parent_id is null", null, null, null, KEY_USAGES);
        if (c != null) {
          if (c.moveToFirst()) {
            ContentValues v = new ContentValues();
            int count = 0;
            while (c.getPosition() < c.getCount()) {
              v.put(KEY_COLOR, MAIN_COLORS[count % MAIN_COLORS.length]);
              db.update("categories", v, "_id = " + c.getLong(0), null);
              c.moveToNext();
              count++;
            }
          }
          c.close();
        }
      }
    } catch (SQLException e) {
      throw Utils.hasApiLevel(Build.VERSION_CODES.JELLY_BEAN) ?
          new SQLiteUpgradeFailedException("Database upgrade failed", e) :
          e;
    }
  }

  private void createOrRefreshAccountTriggers(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS update_account_sync_null");
    db.execSQL("DROP TRIGGER IF EXISTS sort_key_default");
    db.execSQL(UPDATE_ACCOUNT_SYNC_NULL_TRIGGER);
    db.execSQL(ACCOUNTS_TRIGGER_CREATE);
  }

  private void createOrRefreshChangelogTriggers(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS insert_change_log");
    db.execSQL("DROP TRIGGER IF EXISTS insert_after_update_change_log");
    db.execSQL("DROP TRIGGER IF EXISTS delete_after_update_change_log");
    db.execSQL("DROP TRIGGER IF EXISTS delete_change_log");
    db.execSQL("DROP TRIGGER IF EXISTS update_change_log");

    db.execSQL(TRANSACTIONS_INSERT_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_INSERT_AFTER_UPDATE_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_DELETE_AFTER_UPDATE_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_DELETE_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_UPDATE_TRIGGER_CREATE);
  }

  private void createOrRefreshViews(SQLiteDatabase db) {
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_COMMITTED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_UNCOMMITTED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_ALL);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_ALL);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_UNCOMMITTED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_CHANGES_EXTENDED);


    String viewTransactions = buildViewDefinition(TABLE_TRANSACTIONS);
    String viewExtended = buildViewDefinitionExtended(TABLE_TRANSACTIONS);
    db.execSQL("CREATE VIEW " + VIEW_COMMITTED + viewTransactions + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
    db.execSQL("CREATE VIEW " + VIEW_UNCOMMITTED + viewTransactions + " WHERE " + KEY_STATUS + " = " + STATUS_UNCOMMITTED + ";");
    db.execSQL("CREATE VIEW " + VIEW_ALL + viewExtended);
    db.execSQL("CREATE VIEW " + VIEW_EXTENDED + viewExtended + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");

    String viewTemplates = buildViewDefinition(TABLE_TEMPLATES);
    String viewTemplatesExtended = buildViewDefinitionExtended(TABLE_TEMPLATES);
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_UNCOMMITTED + viewTemplates + " WHERE " + KEY_STATUS + " = " + STATUS_UNCOMMITTED + ";");
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_ALL + viewTemplatesExtended);
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_EXTENDED + viewTemplatesExtended + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");

    db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
  }

  @Override
  public final void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new SQLiteDowngradeFailedException();
  }

  public static class SQLiteDowngradeFailedException extends SQLiteException {
  }

  public static class SQLiteUpgradeFailedException extends SQLiteException {
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public SQLiteUpgradeFailedException(String error, Throwable cause) {
      super(error, cause);
    }
  }

}
