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

import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.Utils;

import java.util.Calendar;
import java.util.Locale;

/**
 * @author Michael Totschnig
 */
public class DatabaseConstants {
  private static boolean isLocalized = false;
  public static int weekStartsOn, monthStartsOn;
  private static String YEAR_OF_WEEK_START;
  private static String YEAR_OF_MONTH_START;
  private static String WEEK;
  private static String MONTH;
  private static String THIS_YEAR_OF_WEEK_START;
  private static String THIS_WEEK;
  private static String THIS_MONTH;
  private static String WEEK_START;
  private static String WEEK_END;
  private static String COUNT_FROM_WEEK_START_ZERO;
  private static String WEEK_START_JULIAN;

  //in sqlite julian days are calculated from noon, in order to make sure that the returned julian day matches the day we need, we set the time to noon.
  private static final String JULIAN_DAY_OFFSET = "'start of day','+12 hours'";

  private DatabaseConstants() {
  }

  public static void buildLocalized(Locale locale) {
    weekStartsOn = Utils.getFirstDayOfWeekFromPreferenceWithFallbackToLocale(locale);
    monthStartsOn = Integer.parseInt(PrefKey.GROUP_MONTH_STARTS.getString("1"));
    int monthDelta = monthStartsOn - 1;
    int nextWeekEndSqlite; //Sqlite starts with Sunday = 0
    int nextWeekStartsSqlite = weekStartsOn - 1;
    if (weekStartsOn == Calendar.SUNDAY) {
      //weekStartsOn Sunday
      nextWeekEndSqlite = 6;
    } else {
      //weekStartsOn Monday or Saturday
      nextWeekEndSqlite = weekStartsOn - 2;
    }
    YEAR_OF_WEEK_START = "CAST(strftime('%Y',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)";
    YEAR_OF_MONTH_START = "CAST(strftime('%Y',date,'unixepoch','localtime','-" + monthDelta + " day') AS integer)";
    WEEK_START = "strftime('%s',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day','utc')";
    THIS_YEAR_OF_WEEK_START = "CAST(strftime('%Y','now','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)";
    WEEK_END = "strftime('%s',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "','utc')";
    WEEK = "CAST(strftime('%W',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)"; //calculated for the beginning of the week
    MONTH = "CAST(strftime('%m',date,'unixepoch','localtime','-" + monthDelta + " day') AS integer) - 1";
    THIS_WEEK = "CAST(strftime('%W','now','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)";
    THIS_MONTH = "CAST(strftime('%m','now','localtime','-" + monthDelta + " day') AS integer) - 1";
    COUNT_FROM_WEEK_START_ZERO = "strftime('%%s','%d-01-01','weekday 1', 'weekday " + nextWeekStartsSqlite + "', '" +
        "-7 day" +
        "' ,'+%d day','utc')";
    WEEK_START_JULIAN = "julianday(date,'unixepoch','localtime'," + JULIAN_DAY_OFFSET + ",'weekday " + nextWeekEndSqlite + "', '-6 day')";
    isLocalized = true;
  }

  private static void ensureLocalized() {
    if (!isLocalized) {
      buildLocalized(Locale.getDefault());
    }
  }

  //if we do not cast the result to integer, we would need to do the conversion in Java
  public static final String YEAR = "CAST(strftime('%Y',date,'unixepoch','localtime') AS integer)";
  public static final String THIS_DAY = "CAST(strftime('%j','now','localtime') AS integer)";
  public static final String DAY = "CAST(strftime('%j',date,'unixepoch','localtime') AS integer)";
  public static final String THIS_YEAR = "CAST(strftime('%Y','now','localtime') AS integer)";
  public static final String DAY_START_JULIAN = "julianday(date,'unixepoch','localtime'," + JULIAN_DAY_OFFSET + ")";
  public static final String KEY_DATE = "date";
  public static final String KEY_VALUE_DATE = "value_date";
  public static final String KEY_AMOUNT = "amount";
  public static final String KEY_COMMENT = "comment";
  public static final String KEY_ROWID = "_id";
  public static final String KEY_CATID = "cat_id";
  public static final String KEY_ACCOUNTID = "account_id";
  public static final String KEY_PAYEEID = "payee_id";
  public static final String KEY_TRANSFER_PEER = "transfer_peer";
  public static final String KEY_METHODID = "method_id";
  public static final String KEY_TITLE = "title";
  public static final String KEY_LABEL_MAIN = "label_main";
  public static final String KEY_LABEL_SUB = "label_sub";
  public static final String KEY_LABEL = "label";
  public static final String KEY_COLOR = "color";
  public static final String KEY_TYPE = "type";
  public static final String KEY_CURRENCY = "currency";
  public static final String KEY_DESCRIPTION = "description";
  public static final String KEY_OPENING_BALANCE = "opening_balance";
  public static final String KEY_USAGES = "usages";
  public static final String KEY_PARENTID = "parent_id";
  public static final String KEY_TRANSFER_ACCOUNT = "transfer_account";
  public static final String KEY_STATUS = "status";
  public static final String KEY_PAYEE_NAME = "name";
  public static final String KEY_METHOD_LABEL = "method_label";
  public static final String KEY_PAYEE_NAME_NORMALIZED = "name_normalized";
  public static final String KEY_TRANSACTIONID = "transaction_id";
  public static final String KEY_GROUPING = "grouping";
  public static final String KEY_CR_STATUS = "cr_status";
  public static final String KEY_REFERENCE_NUMBER = "number";
  public static final String KEY_IS_NUMBERED = "is_numbered";
  public static final String KEY_PLANID = "plan_id";
  public static final String KEY_PLAN_EXECUTION = "plan_execution";
  public static final String KEY_TEMPLATEID = "template_id";
  public static final String KEY_INSTANCEID = "instance_id";
  public static final String KEY_CODE = "code";
  public static final String KEY_WEEK_START = "week_start";
  public static final String KEY_GROUP_START = "group_start";
  public static final String KEY_WEEK_END = "week_end";
  public static final String KEY_DAY = "day";
  public static final String KEY_WEEK = "week";
  public static final String KEY_MONTH = "month";
  public static final String KEY_YEAR = "year";
  public static final String KEY_YEAR_OF_WEEK_START = "year_of_week_start";
  public static final String KEY_YEAR_OF_MONTH_START = "year_of_month_start";
  public static final String KEY_THIS_DAY = "this_day";
  public static final String KEY_THIS_WEEK = "this_week";
  public static final String KEY_THIS_MONTH = "this_month";
  public static final String KEY_THIS_YEAR = "this_year";
  public static final String KEY_THIS_YEAR_OF_WEEK_START = "this_year_of_week_start";
  public static final String KEY_MAX_VALUE = "max_value";
  public static final String KEY_CURRENT_BALANCE = "current_balance";
  public static final String KEY_TOTAL = "total";
  public static final String KEY_CLEARED_TOTAL = "cleared_total";
  public static final String KEY_RECONCILED_TOTAL = "reconciled_total";
  public static final String KEY_SUM_EXPENSES = "sum_expenses";
  public static final String KEY_SUM_INCOME = "sum_income";
  public static final String KEY_SUM_TRANSFERS = "sum_transfers";
  public static final String KEY_MAPPED_CATEGORIES = "mapped_categories";
  public static final String KEY_MAPPED_PAYEES = "mapped_payees";
  public static final String KEY_MAPPED_METHODS = "mapped_methods";
  public static final String KEY_MAPPED_TEMPLATES = "mapped_templates";
  public static final String KEY_MAPPED_TRANSACTIONS = "mapped_transactions";
  public static final String KEY_HAS_CLEARED = "has_cleared";
  public static final String KEY_HAS_EXPORTED = "has_exported";
  public static final String KEY_IS_AGGREGATE = "is_aggregate";
  public static final String KEY_HAS_FUTURE = "has_future"; //has the accounts transactions stored for future dates
  public static final String KEY_SUM = "sum";
  public static final String KEY_SORT_KEY = "sort_key";
  public static final String KEY_SORT_KEY_TYPE = "sort_key_type";
  public static final String KEY_EXCLUDE_FROM_TOTALS = "exclude_from_totals";
  public static final String KEY_PREDEFINED_METHOD_NAME = "predefined";
  public static final String KEY_UUID = "uuid";
  public static final String KEY_PICTURE_URI = "picture_id";//historical reasons
  public static final String KEY_SYNC_ACCOUNT_NAME = "sync_account_name";
  public static final String KEY_TRANSFER_AMOUNT = "transfer_amount";
  public static final String KEY_LABEL_NORMALIZED = "label_normalized";
  public static final String KEY_LAST_USED = "last_used";
  public static final String KEY_HAS_TRANSFERS = "has_transfers";
  public static final String KEY_PLAN_INFO = "plan_info";
  public static final String KEY_PARENT_UUID = "parent_uuid";
  public static final String KEY_SYNC_SEQUENCE_LOCAL = "sync_sequence_local";
  public static final String KEY_ACCOUNT_LABEL = "account_label";
  public static final String KEY_IS_SAME_CURRENCY = "is_same_currency";
  public static final String KEY_TIMESTAMP = "timestamp";
  public static final String KEY_KEY = "key";
  public static final String KEY_VALUE = "value";
  public static final String KEY_SORT_DIRECTION = "sort_direction";
  public static final String KEY_CURRENCY_SELF = "currency_self";
  public static final String KEY_CURRENCY_OTHER= "currency_other";
  public static final String KEY_EXCHANGE_RATE = "exchange_rate";
  public static final String KEY_ORIGINAL_AMOUNT = "original_amount";
  public static final String KEY_ORIGINAL_CURRENCY = "original_currency";
  public static final String KEY_EQUIVALENT_AMOUNT = "equivalent_amount";
  public static final String KEY_TRANSFER_PEER_PARENT = "transfer_peer_parent";
  public static final String KEY_BUDGETID = "budget_id";
  /**
   * Used for both saving goal and credit limit on accounts
   */
  public static final String KEY_CRITERION = "criterion";

  /**
   * column alias for the second group (month or week)
   */
  public static final String KEY_SECOND_GROUP = "second";

  /**
   * No special status
   */
  public static final int STATUS_NONE = 0;

  /**
   * transaction that already has been exported
   */
  public static final int STATUS_EXPORTED = 1;
  /**
   * split transaction (and its parts) that are currently edited
   */
  public static final int STATUS_UNCOMMITTED = 2;

  /**
   * a transaction that has been created as a result of an export
   * with {@link Account#EXPORT_HANDLE_DELETED_CREATE_HELPER}
   */
  public static final int STATUS_HELPER = 3;

  public static final String TABLE_TRANSACTIONS = "transactions";
  public static final String TABLE_ACCOUNTS = "accounts";
  public static final String TABLE_SYNC_STATE = "_sync_state";
  public static final String TABLE_CATEGORIES = "categories";
  public static final String TABLE_METHODS = "paymentmethods";
  public static final String TABLE_ACCOUNTTYES_METHODS = "accounttype_paymentmethod";
  public static final String TABLE_TEMPLATES = "templates";
  public static final String TABLE_PAYEES = "payee";
  public static final String TABLE_CURRENCIES = "currency";
  public static final String VIEW_COMMITTED = "transactions_committed";
  public static final String VIEW_UNCOMMITTED = "transactions_uncommitted";
  public static final String VIEW_ALL = "transactions_all";
  public static final String VIEW_TEMPLATES_ALL = "templates_all";
  public static final String VIEW_TEMPLATES_UNCOMMITTED = "templates_uncommitted";
  public static final String VIEW_EXTENDED = "transactions_extended";
  public static final String VIEW_CHANGES_EXTENDED = "changes_extended";
  public static final String VIEW_TEMPLATES_EXTENDED = "templates_extended";
  public static final String TABLE_PLAN_INSTANCE_STATUS = "planinstance_transaction";
  public static final String TABLE_STALE_URIS = "stale_uris";
  public static final String TABLE_CHANGES = "changes";
  public static final String TABLE_SETTINGS = "settings";
  public static final String TABLE_ACCOUNT_EXCHANGE_RATES = "account_exchangerates";
  /**
   * used on backup and restore
   */
  public static final String TABLE_EVENT_CACHE = "event_cache";

  static final String TABLE_BUDGETS = "budgets";
  static final String TABLE_BUDGET_CATEGORIES = "budget_categories";

  /**
   * an SQL CASE expression for transactions
   * that gives either the category for normal transactions
   * or the account for transfers
   */
  public static final String LABEL_MAIN =
      "CASE WHEN " +
          KEY_TRANSFER_ACCOUNT +
          " THEN " +
          "  (SELECT " + KEY_LABEL + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ") " +
          "WHEN " +
          KEY_CATID +
          " THEN " +
          "  CASE WHEN " +
          "    (SELECT " + KEY_PARENTID + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          "  THEN " +
          "    (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES
          + " WHERE  " + KEY_ROWID + " = (SELECT " + KEY_PARENTID + " FROM " + TABLE_CATEGORIES
          + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ")) " +
          "  ELSE " +
          "    (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          "  END " +
          "END AS " + KEY_LABEL_MAIN;

  public static final String LABEL_SUB =
      "CASE WHEN " +
          "  " + KEY_TRANSFER_PEER + " is null AND " + KEY_CATID + " AND (SELECT " + KEY_PARENTID + " FROM " + TABLE_CATEGORIES
          + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          "THEN " +
          "  (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          "END AS " + KEY_LABEL_SUB;

  /**
   * //different from Transaction, since transfer_peer is treated as boolean here
   */
  public static final String LABEL_SUB_TEMPLATE =
      "CASE WHEN " +
          "  " + KEY_CATID + " AND (SELECT " + KEY_PARENTID + " FROM " + TABLE_CATEGORIES
          + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          "THEN " +
          "  (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          "END AS " + KEY_LABEL_SUB;

  private static final String FULL_CAT_CASE =
      " CASE WHEN " +
          " (SELECT " + KEY_PARENTID + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          " THEN " +
          " (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " +
          " (SELECT " + KEY_PARENTID + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ")) " +
          " || '" + TransactionList.CATEGORY_SEPARATOR +
          "' ELSE '' END || " +
          " (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ")";

  public static final String CAT_AS_LABEL = FULL_CAT_CASE + " AS " + KEY_LABEL;

  public static final String TRANSFER_ACCOUNT_UUUID = "(SELECT " + KEY_UUID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ") AS " + KEY_TRANSFER_ACCOUNT;

  /**
   * if transaction is linked to a subcategory
   * main and category label are concatenated
   */
  public static final String FULL_LABEL =
      "CASE WHEN " +
          "  " + KEY_TRANSFER_ACCOUNT + " " +
          " THEN " +
          "  (SELECT " + KEY_LABEL + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ") " +
          " ELSE " +
          FULL_CAT_CASE +
          " END AS  " + KEY_LABEL;


  public static final String TRANSFER_PEER_PARENT =
      "(SELECT " + KEY_PARENTID
          + " FROM " + TABLE_TRANSACTIONS + " peer WHERE peer." + KEY_ROWID
          + " = " + VIEW_EXTENDED + "." + KEY_TRANSFER_PEER + ")";

  /**
   * Can only be used when fetching single transaction from DB, because the inner select is linked
   * to VIEW_ALL used as outer table
   */
  public static final String TRANSFER_AMOUNT =
      "CASE WHEN " +
          "  " + KEY_TRANSFER_PEER + " " +
          " THEN " +
          "  (SELECT " + KEY_AMOUNT + " FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_ROWID + " = " + VIEW_ALL + "." + KEY_TRANSFER_PEER + ") " +
          " ELSE null" +
          " END AS " + KEY_TRANSFER_AMOUNT;

  public static final Long SPLIT_CATID = 0L;

  public static final String WHERE_NOT_SPLIT =
      "(" + KEY_CATID + " IS null OR " + KEY_CATID + " != " + SPLIT_CATID + ")";
  public static final String WHERE_NOT_SPLIT_PART =
      KEY_PARENTID + " IS null";
  public static final String WHERE_IN_PAST = KEY_DATE + " <= strftime('%s','now')";
  public static final String WHERE_NOT_VOID =
      KEY_CR_STATUS + " != '" + Transaction.CrStatus.VOID.name() + "'";
  public static final String WHERE_TRANSACTION =
      WHERE_NOT_SPLIT + " AND " + WHERE_NOT_VOID + " AND " + KEY_TRANSFER_PEER + " is null";
  public static final String WHERE_INCOME = KEY_AMOUNT + ">0 AND " + WHERE_TRANSACTION;
  public static final String WHERE_EXPENSE = KEY_AMOUNT + "<0 AND " + WHERE_TRANSACTION;
  public static final String WHERE_IN = KEY_AMOUNT + ">0 AND " + WHERE_NOT_SPLIT + " AND " + WHERE_NOT_VOID;
  public static final String WHERE_OUT = KEY_AMOUNT + "<0 AND " + WHERE_NOT_SPLIT + " AND " + WHERE_NOT_VOID;
  public static final String WHERE_TRANSFER =
      WHERE_NOT_SPLIT + " AND " + WHERE_NOT_VOID + " AND " + KEY_TRANSFER_PEER + " is not null";

  public static final String TRANSFER_SUM =
      "sum(CASE WHEN " + WHERE_TRANSFER + " THEN " + KEY_AMOUNT + " ELSE 0 END)";
  public static final String HAS_CLEARED =
      "(SELECT EXISTS(SELECT 1 FROM " + TABLE_TRANSACTIONS + " WHERE "
          + KEY_ACCOUNTID + " = " + TABLE_ACCOUNTS + "." + KEY_ROWID + " AND " + KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "' LIMIT 1)) AS " + KEY_HAS_CLEARED;
  public static final String HAS_EXPORTED =
      "(SELECT EXISTS(SELECT 1 FROM " + TABLE_TRANSACTIONS + " WHERE "
          + KEY_ACCOUNTID + " = " + TABLE_ACCOUNTS + "." + KEY_ROWID + " AND " + KEY_STATUS + " = " + STATUS_EXPORTED + " LIMIT 1)) AS " + KEY_HAS_EXPORTED;
  public static final String HAS_FUTURE =
      "(SELECT EXISTS(SELECT 1 FROM " + TABLE_TRANSACTIONS + " WHERE "
          + KEY_ACCOUNTID + " = " + TABLE_ACCOUNTS + "." + KEY_ROWID + " AND " + KEY_DATE + " > strftime('%s','now')  LIMIT 1)) AS " + KEY_HAS_FUTURE;
  public static final String SELECT_AMOUNT_SUM = "SELECT coalesce(sum(" + KEY_AMOUNT + "),0) FROM "
      + VIEW_COMMITTED
      + " WHERE " + KEY_ACCOUNTID + " = " + TABLE_ACCOUNTS + "." + KEY_ROWID
      + " AND " + WHERE_NOT_VOID;
  //exclude split_catid
  public static final String MAPPED_CATEGORIES =
      "count(CASE WHEN  " + KEY_CATID + ">0 AND " + WHERE_NOT_VOID + " THEN 1 ELSE null END) as " + KEY_MAPPED_CATEGORIES;
  public static final String MAPPED_PAYEES =
      "count(CASE WHEN  " + KEY_PAYEEID + ">0 AND " + WHERE_NOT_VOID + "  THEN 1 ELSE null END) as " + KEY_MAPPED_PAYEES;
  public static final String MAPPED_METHODS =
      "count(CASE WHEN  " + KEY_METHODID + ">0 AND " + WHERE_NOT_VOID + "  THEN 1 ELSE null END) as " + KEY_MAPPED_METHODS;
  public static final String HAS_TRANSFERS =
      "count(CASE WHEN  " + KEY_TRANSFER_ACCOUNT + ">0 AND " + WHERE_NOT_VOID + "  THEN 1 ELSE null END) as " + KEY_HAS_TRANSFERS;

  public static final String WHERE_DEPENDENT = KEY_PARENTID + " = ? OR " + KEY_ROWID + " IN "
      + "(SELECT " + KEY_TRANSFER_PEER + " FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_PARENTID + "= ?)";
  ;

  public static final String WHERE_RELATED = KEY_TRANSFER_PEER + " = ? OR " + WHERE_DEPENDENT;

  public static final String WHERE_SELF_OR_PEER = KEY_TRANSFER_PEER + " = ? OR " + KEY_ROWID + " = ?";

  public static final String WHERE_SELF_OR_DEPENDENT = KEY_ROWID + " = ? OR " + WHERE_DEPENDENT;

  public static final String IS_SAME_CURRENCY = KEY_CURRENCY + " = (SELECT " + KEY_CURRENCY + " from " +
      TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ")";

  public static String getYearOfWeekStart() {
    ensureLocalized();
    return YEAR_OF_WEEK_START;
  }

  public static String getYearOfMonthStart() {
    ensureLocalized();
    return YEAR_OF_MONTH_START;
  }

  public static String getWeek() {
    ensureLocalized();
    return WEEK;
  }

  public static String getMonth() {
    ensureLocalized();
    return MONTH;
  }

  public static String getThisYearOfWeekStart() {
    ensureLocalized();
    return THIS_YEAR_OF_WEEK_START;
  }

  public static String getWeekStartJulian() {
    ensureLocalized();
    return WEEK_START_JULIAN;
  }

  public static String getThisWeek() {
    ensureLocalized();
    return THIS_WEEK;
  }

  public static String getThisMonth() {
    ensureLocalized();
    return THIS_MONTH;
  }

  public static String getWeekStart() {
    ensureLocalized();
    return WEEK_START;
  }

  public static String getWeekEnd() {
    ensureLocalized();
    return WEEK_END;
  }

  /**
   * we want to find out the week range when we are given a week number
   * we find out the first day in the year, that is the firstdayofweek of the locale and is
   * one week behind the first day with week number 1
   * add (weekNumber-1)*7 days to get at the beginning of the week
   */
  public static String getCountFromWeekStartZero() {
    ensureLocalized();
    return COUNT_FROM_WEEK_START_ZERO;
  }

  public static String getAmountHomeEquivalent() {
    return getAmountHomeEquivalent(VIEW_EXTENDED);
  }

  public static String getAmountHomeEquivalent(String forTable) {
    return "coalesce(" + calcEquivalentAmountForSplitParts(forTable) + "," +
        getExchangeRate(forTable + "." +  KEY_ACCOUNTID) + " * " + KEY_AMOUNT + ")";
  }

  private static String calcEquivalentAmountForSplitParts(String forTable) {
    return "CASE WHEN " + KEY_PARENTID
        + " THEN " +
        "(SELECT 1.0 * " + KEY_EQUIVALENT_AMOUNT + " / " + KEY_AMOUNT + " FROM " + TABLE_TRANSACTIONS + " WHERE " +
        KEY_ROWID + " = " + forTable + "." + KEY_PARENTID + ") * " + KEY_AMOUNT +
        " ELSE "
        + KEY_EQUIVALENT_AMOUNT + " END";
  }

  public static String getExchangeRate(String accountReference) {
    return "coalesce((SELECT " + KEY_EXCHANGE_RATE + " FROM " + TABLE_ACCOUNT_EXCHANGE_RATES + " WHERE " + KEY_ACCOUNTID + " = " + accountReference +
        " AND " + KEY_CURRENCY_SELF + "=" + KEY_CURRENCY + " AND " + KEY_CURRENCY_OTHER + "='" + PrefKey.HOME_CURRENCY.getString(null) + "'), 1)";
  }

  private static String getAmountCalculation(boolean forHome) {
    return forHome ? getAmountHomeEquivalent() : KEY_AMOUNT;
  }

  static String getInSum(boolean forHome) {
    return "sum(CASE WHEN " + WHERE_IN + " THEN " + getAmountCalculation(forHome) + " ELSE 0 END) AS " + KEY_SUM_INCOME;
  }

  static String getIncomeSum(boolean forHome) {
    return "sum(CASE WHEN " + WHERE_INCOME + " THEN " + getAmountCalculation(forHome) + " ELSE 0 END) AS " + KEY_SUM_INCOME;
  }

  static String getOutSum(boolean forHome) {
    return "sum(CASE WHEN " + WHERE_OUT + " THEN " + getAmountCalculation(forHome) + " ELSE 0 END) AS " + KEY_SUM_EXPENSES;
  }

  static String getExpenseSum(boolean forHome) {
    return "sum(CASE WHEN " + WHERE_EXPENSE + " THEN " + getAmountCalculation(forHome) + " ELSE 0 END) AS " + KEY_SUM_EXPENSES;
  }
}
