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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET;
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
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;
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
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_EXECUTION_ADVANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_DIRECTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST;
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
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES_TAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS_TAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_ALL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_CHANGES_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_ALL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_WITH_ACCOUNT;
import static org.totschnig.myexpenses.util.ColorUtils.MAIN_COLORS;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public class TransactionDatabase extends SQLiteOpenHelper {
  public static final int DATABASE_VERSION = 115;
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
          + KEY_CR_STATUS + " text not null check (" + KEY_CR_STATUS + " in (" + CrStatus.JOIN + ")) default '" + CrStatus.RECONCILED.name() + "',"
          + KEY_REFERENCE_NUMBER + " text, "
          + KEY_PICTURE_URI + " text, "
          + KEY_UUID + " text, "
          + KEY_ORIGINAL_AMOUNT + " integer, "
          + KEY_ORIGINAL_CURRENCY + " text, "
          + KEY_EQUIVALENT_AMOUNT + " integer);";

  private static final String TRANSACTIONS_UUID_INDEX_CREATE = "CREATE UNIQUE INDEX transactions_account_uuid_index ON "
      + TABLE_TRANSACTIONS + "(" + KEY_ACCOUNTID + "," + KEY_UUID + "," + KEY_STATUS + ")";

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

  private static String buildViewWithAccount() {
    return new StringBuilder()
        .append(" AS SELECT ").append(TABLE_TRANSACTIONS).append(".*").append(", ")
        .append(KEY_COLOR).append(", ")
        .append(KEY_CURRENCY).append(", ")
        .append(KEY_EXCLUDE_FROM_TOTALS).append(", ")
        .append(TABLE_ACCOUNTS).append(".").append(KEY_TYPE).append(" AS ").append(KEY_ACCOUNT_TYPE).append(", ")
        .append(TABLE_ACCOUNTS).append(".").append(KEY_LABEL).append(" AS ").append(KEY_ACCOUNT_LABEL)
        .append(" FROM ").append(TABLE_TRANSACTIONS).append(" LEFT JOIN ")
        .append(TABLE_ACCOUNTS).append(" ON ").append(KEY_ACCOUNTID)
        .append(" = ").append(TABLE_ACCOUNTS).append(".").append(KEY_ROWID).toString();
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
          .append(KEY_SEALED).append(", ")
          .append(KEY_EXCLUDE_FROM_TOTALS).append(", ")
          .append(TABLE_ACCOUNTS).append(".").append(KEY_TYPE).append(" AS ").append(KEY_ACCOUNT_TYPE).append(", ")
          .append(TABLE_ACCOUNTS).append(".").append(KEY_LABEL).append(" AS ").append(KEY_ACCOUNT_LABEL);
    }

    if (tableName.equals(TABLE_TRANSACTIONS)) {
      stringBuilder.append(", ").append(TABLE_PLAN_INSTANCE_STATUS).append(".").append(KEY_TEMPLATEID);
      stringBuilder.append(", group_concat(").append(TABLE_TAGS).append(".").append(KEY_LABEL).append(", ', ') AS ").append(KEY_TAGLIST);
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
          + KEY_CURRENCY + " text not null  references " + TABLE_CURRENCIES + "(" + KEY_CODE + "), "
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
          + KEY_CRITERION + " integer,"
          + KEY_HIDDEN + " boolean default 0,"
          + KEY_SEALED + " boolean default 0);";

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
          + KEY_ICON + " string, "
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
          + KEY_STATUS + " integer default 0,"
          + KEY_PLAN_EXECUTION_ADVANCE + " integer default 0,"
          + KEY_DEFAULT_ACTION + " text not null check (" + KEY_DEFAULT_ACTION + " in (" + Template.Action.JOIN + ")) default '" + Template.Action.SAVE.name() + "');";

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
          + " (" + KEY_ROWID + " integer primary key autoincrement, " +
          KEY_CODE + " text UNIQUE not null," +
          KEY_GROUPING + " text not null check (" + KEY_GROUPING + " in (" + Grouping.JOIN + ")) default '" + Grouping.NONE.name() + "'," +
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

  private static final String RAISE_UPDATE_SEALED_ACCOUNT =
      "SELECT RAISE (FAIL, 'attempt to update sealed account');";

  private static final String ACCOUNTS_SEALED_TRIGGER_CREATE =
      String.format(Locale.ROOT, "CREATE TRIGGER sealed_account_update BEFORE UPDATE OF %1$s,%2$s,%3$s,%4$s,%5$s,%6$s,%7$s ON %8$s WHEN old.%9$s = 1 ",
          KEY_LABEL, KEY_OPENING_BALANCE, KEY_DESCRIPTION, KEY_CURRENCY, KEY_TYPE, KEY_UUID, KEY_CRITERION, TABLE_ACCOUNTS, KEY_SEALED) +
          String.format(Locale.ROOT, "BEGIN %s END", RAISE_UPDATE_SEALED_ACCOUNT);

  private static final String TRANSACTIONS_SEALED_INSERT_TRIGGER_CREATE =
      "CREATE TRIGGER sealed_account_transaction_insert " +
          "BEFORE INSERT ON " + TABLE_TRANSACTIONS + " " +
          "WHEN (SELECT " + KEY_SEALED + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = new." + KEY_ACCOUNTID + ") = 1 " +
          String.format(Locale.ROOT, "BEGIN %s END", RAISE_UPDATE_SEALED_ACCOUNT);

  private static final String TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE =
      "CREATE TRIGGER sealed_account_transaction_update " +
          "BEFORE UPDATE ON " + TABLE_TRANSACTIONS + " " +
          "WHEN (SELECT max(" + KEY_SEALED + ") FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " IN (new." + KEY_ACCOUNTID + ",old." + KEY_ACCOUNTID + ")) = 1 " +
          String.format(Locale.ROOT, "BEGIN %s END", RAISE_UPDATE_SEALED_ACCOUNT);

  private static final String TRANSACTIONS_SEALED_DELETE_TRIGGER_CREATE =
      "CREATE TRIGGER sealed_account_transaction_delete " +
          "BEFORE DELETE ON " + TABLE_TRANSACTIONS + " " +
          "WHEN (SELECT " + KEY_SEALED + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = old." + KEY_ACCOUNTID + ") = 1 " +
          String.format(Locale.ROOT, "BEGIN %s END", RAISE_UPDATE_SEALED_ACCOUNT);

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
          + KEY_REFERENCE_NUMBER + " text, "
          + KEY_PICTURE_URI + " text);";

  private static final String BUDGETS_CREATE =
      "CREATE TABLE " + TABLE_BUDGETS + " ( "
          + KEY_ROWID + " integer primary key autoincrement, "
          + KEY_TITLE + " text not null default '', "
          + KEY_DESCRIPTION + " text not null, "
          + KEY_GROUPING + " text not null check (" + KEY_GROUPING + " in (" + Grouping.JOIN + ")), "
          + KEY_BUDGET + " integer not null, "
          + KEY_ACCOUNTID + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_CURRENCY + " text, "
          + KEY_START + " datetime, "
          + KEY_END + " datetime)";

  private static final String BUDGETS_CATEGORY_CREATE =
      "CREATE TABLE " + TABLE_BUDGET_CATEGORIES + " ( "
          + KEY_BUDGETID + " integer references " + TABLE_BUDGETS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_CATID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_BUDGET + " integer not null, "
          + "primary key (" + KEY_BUDGETID + "," + KEY_CATID + "));";


  private static final String SELECT_SEQUENCE_NUMBER_TEMPLATE = "(SELECT " + KEY_SYNC_SEQUENCE_LOCAL + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = %s." + KEY_ACCOUNTID + ")";
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
      + String.format(Locale.US, SELECT_SEQUENCE_NUMBER_TEMPLATE, "new") + ", "
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
      + String.format(Locale.US, SELECT_SEQUENCE_NUMBER_TEMPLATE, "old") + ", "
      + "old." + KEY_ACCOUNTID + ", "
      + "old." + KEY_UUID + ", "
      + String.format(Locale.US, SELECT_PARENT_UUID_TEMPLATE, "old") + "); END;";

  private static final String DELETE_TRIGGER_ACTION_AFTER_TRANSFER_UPDATE = " BEGIN INSERT INTO " + TABLE_CHANGES + "("
      + KEY_TYPE + ","
      + KEY_SYNC_SEQUENCE_LOCAL + ", "
      + KEY_ACCOUNTID + ","
      + KEY_UUID + ","
      + KEY_PARENT_UUID + ") VALUES ('" + TransactionChange.Type.deleted + "', "
      + String.format(Locale.US, SELECT_SEQUENCE_NUMBER_TEMPLATE, "old") + ", "
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
          + " AND new." + KEY_STATUS + " = " + " old." + KEY_STATUS //we ignore setting of exported flag
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
          + String.format(Locale.US, SELECT_SEQUENCE_NUMBER_TEMPLATE, "old") + ", "
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

  private static final String UPDATE_ACCOUNT_METADATA_TRIGGER = String.format(
      "CREATE TRIGGER update_account_metadata AFTER UPDATE OF %1$s,%2$s,%3$s,%4$s,%5$s,%6$s,%7$s,%8$s ON %9$s "
          + " WHEN new.%10$s IS NOT NULL AND new.%16$s > 0 AND NOT EXISTS (SELECT 1 FROM %11$s)"
          + " BEGIN INSERT INTO %12$s (%13$s, %14$s, %15$s, %16$s) VALUES ('metadata', '_ignored_', new.%17$s, new.%16$s); END;",
      KEY_LABEL, KEY_OPENING_BALANCE, KEY_DESCRIPTION, KEY_CURRENCY, KEY_TYPE, KEY_COLOR, KEY_EXCLUDE_FROM_TOTALS, KEY_CRITERION,
      TABLE_ACCOUNTS, KEY_SYNC_ACCOUNT_NAME, TABLE_SYNC_STATE,
      TABLE_CHANGES, KEY_TYPE, KEY_UUID, KEY_ACCOUNTID, KEY_SYNC_SEQUENCE_LOCAL, KEY_ROWID);

  private static final String UPDATE_ACCOUNT_EXCHANGE_RATE_TRIGGER = String.format(
      "CREATE TRIGGER update_account_exchange_rate AFTER UPDATE ON %1$s "
          + " WHEN %2$s"
          + " BEGIN INSERT INTO %3$s (%4$s, %5$s, %6$s, %7$s) VALUES ('metadata', '_ignored_', new.%6$s, %8$s); END;",
      TABLE_ACCOUNT_EXCHANGE_RATES,
      String.format(Locale.US, SHOULD_WRITE_CHANGE_TEMPLATE, "new"),
      TABLE_CHANGES, KEY_TYPE, KEY_UUID, KEY_ACCOUNTID, KEY_SYNC_SEQUENCE_LOCAL, String.format(SELECT_SEQUENCE_NUMBER_TEMPLATE, "old"));

  private static final String SETTINGS_CREATE =
      "CREATE TABLE " + TABLE_SETTINGS + " ("
          + KEY_KEY + " text unique not null, "
          + KEY_VALUE + " text);";

  private static final String TAGS_CREATE =
      "CREATE TABLE " + TABLE_TAGS
          + " (" + KEY_ROWID + " integer primary key autoincrement, " +
          KEY_LABEL + " text UNIQUE not null);";

  private static final String TRANSACTIONS_TAGS_CREATE =
      "CREATE TABLE " + TABLE_TRANSACTIONS_TAGS
          + " ( " + KEY_TAGID + " integer references " + TABLE_TAGS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + KEY_TRANSACTIONID + " integer references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + ") ON DELETE CASCADE, "
          + "primary key (" + KEY_TAGID + "," + KEY_TRANSACTIONID + "));";

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

  public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
  public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

  TransactionDatabase(Context context, String databaseName) {
    super(context, databaseName, null, DATABASE_VERSION);
    mCtx = context;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      setWriteAheadLoggingEnabled(false);
    }
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);
    if (!db.isReadOnly()) {
      db.execSQL("PRAGMA legacy_alter_table=ON;");
    }
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
      String uncommitedSelect = String.format(Locale.ROOT, "(SELECT %s from %s where %s = %d)",
          KEY_ROWID, TABLE_TRANSACTIONS, KEY_STATUS, STATUS_UNCOMMITTED);
      String uncommitedParentSelect = String.format(Locale.ROOT, "%s IN %s", KEY_PARENTID, uncommitedSelect);
      final String whereClause = String.format(Locale.ROOT,
          "%1$s IN %2$s OR %3$s OR %4$s IN (SELECT %5$s FROM %6$s WHERE %3$s)",
          KEY_ROWID, uncommitedSelect, uncommitedParentSelect, KEY_TRANSFER_PEER, KEY_ROWID, TABLE_TRANSACTIONS);
      Timber.d(whereClause);
      db.delete(TABLE_TRANSACTIONS, whereClause, null);
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
    db.execSQL("CREATE INDEX transactions_payee_id_index on " + TABLE_TRANSACTIONS + "(" + KEY_PAYEEID + ")");
    db.execSQL("CREATE INDEX templates_payee_id_index on " + TABLE_TEMPLATES + "(" + KEY_PAYEEID + ")");

    // Triggers
    createOrRefreshTransactionTriggers(db);
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
    createOrRefreshAccountMetadataTrigger(db);
    db.execSQL(BUDGETS_CREATE);
    db.execSQL(BUDGETS_CATEGORY_CREATE);
    db.execSQL("CREATE INDEX budget_categories_cat_id_index on " + TABLE_BUDGET_CATEGORIES + "(" + KEY_CATID + ")");

    db.execSQL(TAGS_CREATE);
    db.execSQL(TRANSACTIONS_TAGS_CREATE);
    createOrRefreshTransferTagsTriggers(db);
    db.execSQL(TEMPLATES_TAGS_CREATE);

    //Views
    createOrRefreshViews(db);
    //Run on ForTest build type
    //insertTestData(db, 50, 50);
    PrefKey.FIRST_INSTALL_DB_SCHEMA_VERSION.putInt(DATABASE_VERSION);
  }

  public void createOrRefreshTransferTagsTriggers(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS insert_transfer_tags");
    db.execSQL("DROP TRIGGER IF EXISTS delete_transfer_tags");
    db.execSQL(INSERT_TRANSFER_TAGS_TRIGGER);
    db.execSQL(DELETE_TRANSFER_TAGS_TRIGGER);
  }

/*  private void insertTestData(SQLiteDatabase db, int countGroup, int countChild) {
    long date = System.currentTimeMillis() / 1000;
    for (int i = 1; i <= countGroup; i++) {
      AccountInfo testAccount = new AccountInfo("Test account " + i, AccountType.CASH, 0);
      long testAccountId = db.insertOrThrow(DatabaseConstants.TABLE_ACCOUNTS, null, testAccount.getContentValues());
      for (int j = 1; j <= countChild; j++) {
        long payeeId = db.insertOrThrow(DatabaseConstants.TABLE_PAYEES, null, new PayeeInfo("Payee " + i + "_" + j).getContentValues());
        date -= 60 * 60 * 24;
        TransactionInfo transactionInfo = new TransactionInfo("Transaction " + j, date, 0, testAccountId, payeeId);
        db.insertOrThrow(
            DatabaseConstants.TABLE_TRANSACTIONS,
            null,
            transactionInfo.getContentValues()
        );
      }
    }
  }*/

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
              CurrencyContext currencyContext = MyApplication.getInstance().getAppComponent().currencyContext();
              currencyContext.ensureFractionDigitsAreCached(currencyContext.get(c.getString(0)));
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
      TransactionProvider.pauseChangeTrigger(db);

      if (oldVersion < 64) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("code", CurrencyEnum.BYN.name());
        //will log SQLiteConstraintException if value already exists in table
        db.insert("currency", null, initialValues);
      }

      if (oldVersion < 65) {
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
      }

      if (oldVersion < 74) {
        db.execSQL("DROP TRIGGER IF EXISTS insert_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS insert_after_update_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS delete_after_update_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS delete_change_log");
        db.execSQL("DROP TRIGGER IF EXISTS update_change_log");
        db.execSQL("update transactions set transfer_peer = (select _id from transactions peer where peer.transfer_peer = transactions._id) where transfer_peer is null;");
        createOrRefreshTransactionTriggers(db);
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
        Cursor c = db.rawQuery("SELECT distinct currency from accounts", null);
        if (c != null) {
          if (c.moveToFirst()) {
            String GROUPING_PREF_PREFIX = "AGGREGATE_GROUPING_";
            final SharedPreferences settings = MyApplication.getInstance().getSettings();
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
                  db.update("currency", initialValues, "code = ?", new String[]{currency});
                  editor.remove(key);
                  updated = true;
                } catch (Exception e) {
                  //since this setting is not critical, we can live with failure of migration
                  Timber.e(e);
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

      if (oldVersion < 82) {
        createOrRefreshAccountTriggers(db);
      }

      if (oldVersion < 83) {
        final String auto_backup_cloud = MyApplication.getInstance().getSettings().getString("auto_backup_cloud", null);
        if (auto_backup_cloud != null) {
          ContentValues values = new ContentValues(2);
          values.put("key", "auto_backup_cloud");
          values.put("value", auto_backup_cloud);
          db.insert("settings", null, values);
        }
      }

      if (oldVersion < 84) {
        try {
          db.execSQL("CREATE UNIQUE INDEX budgets_type_account ON budgets(grouping,account_id)");
          db.execSQL("CREATE UNIQUE INDEX budgets_type_currency ON budgets(grouping,currency);");
        } catch (SQLException e) {
          // We got one report where this failed, because there were already multiple budgets for
          // account /grouping pairs. At the moment, we silently live without the index.
          Timber.e(e);
        }
      }

      if (oldVersion < 85) {
        db.execSQL("ALTER TABLE accounts add column hidden boolean default 0");
        db.execSQL("ALTER TABLE accounts add column sealed boolean default 0");
        createOrRefreshAccountSealedTrigger(db);
        createOrRefreshTransactionSealedTriggers(db);
      }

      if (oldVersion < 86) {
        db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
        db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
      }

      if (oldVersion < 87) {
        createOrRefreshTemplateViews(db);
      }

      if (oldVersion < 88) {
        db.execSQL("ALTER TABLE categories add column icon string");
      }

      if (oldVersion < 89) {
        //createOrRefreshViews(db);
      }

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
        Cursor c = db.query("budgets", new String[]{"_id",
                String.format(Locale.ROOT, "coalesce(%1$s, -(select %2$s from %3$s where %4$s = %5$s), %6$d) AS %1$s",
                    "account_id", "_id", "currency", "code", "budgets.currency", AggregateAccount.HOME_AGGREGATE_ID), "grouping"},
            null, null, null, null, null);
        if (c != null) {
          if (c.moveToFirst()) {
            final SharedPreferences settings = MyApplication.getInstance().getSettings();
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

      if (oldVersion < 93) {
        //on very recent versions of Sqlite renaming tables like done in upgrade to 92 breaks views AND triggers
        createOrRefreshTransactionTriggers(db);
      }

      if (oldVersion < 94) {
        createOrRefreshAccountTriggers(db);
      }

/*      if (oldVersion < 95) {
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_EXTENDED);
        db.execSQL("CREATE VIEW " + VIEW_EXTENDED + buildViewDefinitionExtended(TABLE_TRANSACTIONS) + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
      }*/
      if (oldVersion < 96) {
        db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
        db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
      }
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
        db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
        createOrRefreshTransactionTriggers(db);
        createOrRefreshAccountTriggers(db);
        createOrRefreshAccountMetadataTrigger(db);
      }
      if (oldVersion < 100) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("code", CurrencyEnum.VEB.name());
        //will log SQLiteConstraintException if value already exists in table
        db.insert("currency", null, initialValues);
      }
      if (oldVersion < 102) {
        db.execSQL("CREATE TABLE tags (_id integer primary key autoincrement, label text UNIQUE not null)");
        db.execSQL("CREATE TABLE transactions_tags ( tag_id integer references tags(_id) ON DELETE CASCADE, transaction_id integer references transactions(_id) ON DELETE CASCADE, primary key (tag_id,transaction_id))");
        createOrRefreshTransferTagsTriggers(db);
        db.execSQL("CREATE TABLE templates_tags ( tag_id integer references tags(_id) ON DELETE CASCADE, template_id integer references templates(_id) ON DELETE CASCADE, primary key (tag_id,template_id));");
        createOrRefreshViews(db);
      }
      if (oldVersion < 103) {
        createOrRefreshTransferTagsTriggers(db);
      }
      if (oldVersion < 104) {
        db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
        db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
        //repair uuids that got lost by bug
        repairTransferUuids(db);
      }
      if (oldVersion < 105) {
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_WITH_ACCOUNT);
        db.execSQL("CREATE VIEW " + VIEW_WITH_ACCOUNT + buildViewWithAccount() + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
      }
      if (oldVersion < 106) {
        db.execSQL("DROP TRIGGER IF EXISTS update_change_log");
        db.execSQL(TRANSACTIONS_UPDATE_TRIGGER_CREATE);
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
      if (oldVersion < 110) {
        createOrRefreshTemplateViews(db);
      }
      if (oldVersion < 111) {
        repairSplitPartDates(db);
      }
      if (oldVersion < 112) {
        String templateDefaultAction = PrefKey.TEMPLATE_CLICK_DEFAULT.getString("SAVE");
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
        db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
        createOrRefreshTransactionTriggers(db);
        createOrRefreshAccountTriggers(db);
        createOrRefreshAccountMetadataTrigger(db);
      }
      TransactionProvider.resumeChangeTrigger(db);
    } catch (SQLException e) {
      throw new SQLiteUpgradeFailedException(oldVersion, newVersion, e);
    }
  }

  public void repairTransferUuids(SQLiteDatabase db) {
    try {
      repairWithSealedAccounts(db, () -> db.execSQL("update transactions set uuid = (select uuid from transactions peer where peer._id=transactions.transfer_peer) where uuid is null and transfer_peer is not null;"));
    } catch (SQLException e) {
      Timber.e(e);
    }
  }

  private void repairWithSealedAccounts(SQLiteDatabase db, Runnable run) {
    db.execSQL("update accounts set sealed = -1 where sealed = 1");
    run.run();
    db.execSQL("update accounts set sealed = 1 where sealed = -1");
  }

  public void repairSplitPartDates(SQLiteDatabase db) {
    repairWithSealedAccounts(db, () -> db.execSQL("UPDATE transactions set date = (select date from transactions parents where _id = transactions.parent_id) where parent_id is not null"));
  }

  private void createOrRefreshAccountTriggers(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS update_account_sync_null");
    db.execSQL("DROP TRIGGER IF EXISTS sort_key_default");
    db.execSQL(UPDATE_ACCOUNT_SYNC_NULL_TRIGGER);
    db.execSQL(ACCOUNTS_TRIGGER_CREATE);
    createOrRefreshAccountSealedTrigger(db);
  }

  private void createOrRefreshAccountSealedTrigger(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_update");
    db.execSQL(ACCOUNTS_SEALED_TRIGGER_CREATE);
  }

  private void createOrRefreshAccountMetadataTrigger(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS update_account_metadata");
    db.execSQL("DROP TRIGGER IF EXISTS update_account_exchange_rate");
    db.execSQL(UPDATE_ACCOUNT_METADATA_TRIGGER);
    db.execSQL(UPDATE_ACCOUNT_EXCHANGE_RATE_TRIGGER);
  }

  private void createOrRefreshTransactionTriggers(SQLiteDatabase db) {
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

    createOrRefreshTransactionSealedTriggers(db);
  }

  private void createOrRefreshTransactionSealedTriggers(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_insert");
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_update");
    db.execSQL("DROP TRIGGER IF EXISTS sealed_account_transaction_delete");
    db.execSQL(TRANSACTIONS_SEALED_INSERT_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE);
    db.execSQL(TRANSACTIONS_SEALED_DELETE_TRIGGER_CREATE);
  }

  private void createOrRefreshViews(SQLiteDatabase db) {
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_COMMITTED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_UNCOMMITTED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_ALL);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_CHANGES_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_WITH_ACCOUNT);

    String viewTransactions = buildViewDefinition(TABLE_TRANSACTIONS);
    String viewExtended = buildViewDefinitionExtended(TABLE_TRANSACTIONS);
    db.execSQL("CREATE VIEW " + VIEW_COMMITTED + viewTransactions + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
    final String tagJoin = String.format(Locale.ROOT, " LEFT JOIN %1$s ON %1$s.%2$s = %3$s.%4$s LEFT JOIN %5$s ON %6$s= %5$s.%4$s",
        TABLE_TRANSACTIONS_TAGS, KEY_TRANSACTIONID, TABLE_TRANSACTIONS, KEY_ROWID, TABLE_TAGS, KEY_TAGID);
    final String tagGroupBy = String.format(Locale.ROOT, " GROUP BY %1$s.%2$s", TABLE_TRANSACTIONS, KEY_ROWID);
    db.execSQL("CREATE VIEW " + VIEW_UNCOMMITTED + viewTransactions + " WHERE " + KEY_STATUS + " = " + STATUS_UNCOMMITTED + ";");
    db.execSQL("CREATE VIEW " + VIEW_ALL + viewExtended + tagJoin + tagGroupBy);
    db.execSQL("CREATE VIEW " + VIEW_EXTENDED + viewExtended + tagJoin + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED +
        tagGroupBy + ";");

    db.execSQL("CREATE VIEW " + VIEW_CHANGES_EXTENDED + buildViewDefinitionExtended(TABLE_CHANGES));
    db.execSQL("CREATE VIEW " + VIEW_WITH_ACCOUNT + buildViewWithAccount() + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");

    createOrRefreshTemplateViews(db);
  }

  private void createOrRefreshTemplateViews(SQLiteDatabase db) {
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_ALL);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_EXTENDED);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_TEMPLATES_UNCOMMITTED);

    String viewTemplates = buildViewDefinition(TABLE_TEMPLATES);
    String viewTemplatesExtended = buildViewDefinitionExtended(TABLE_TEMPLATES);
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_UNCOMMITTED + viewTemplates + " WHERE " + KEY_STATUS + " = " + STATUS_UNCOMMITTED + ";");
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_ALL + viewTemplatesExtended);
    db.execSQL("CREATE VIEW " + VIEW_TEMPLATES_EXTENDED + viewTemplatesExtended + " WHERE " + KEY_STATUS + " != " + STATUS_UNCOMMITTED + ";");
  }

  @Override
  public final void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
