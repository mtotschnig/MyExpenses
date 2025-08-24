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

import static org.totschnig.myexpenses.db2.RepositoryPaymentMethodKt.localizedLabelForPaymentMethod;
import static org.totschnig.myexpenses.provider.DbConstantsKt.TRANSFER_ACCOUNT_LABEL;

import android.content.Context;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.preference.PrefHandler;

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
  private static String THIS_YEAR_OF_MONTH_START;
  private static String THIS_WEEK;
  private static String THIS_MONTH;
  private static String WEEK_START;
  private static String COUNT_FROM_WEEK_START_ZERO;
  private static String WEEK_START_JULIAN;
  private static String WEEK_MAX;

  //in sqlite julian days are calculated from noon, in order to make sure that the returned julian day matches the day we need, we set the time to noon.
  private static final String JULIAN_DAY_OFFSET = "'start of day','+12 hours'";

  private static String[] PROJECTION_BASE, PROJECTION_EXTENDED;

  private DatabaseConstants() {
  }

  public static void buildLocalized(Locale locale, MyApplication myApplication) {
    AppComponent appComponent = myApplication.getAppComponent();
    buildLocalized(
            locale,
            myApplication,
            appComponent.prefHandler()
    );
  }

  public static void buildLocalized(Locale locale, Context context, PrefHandler prefHandler) {
    weekStartsOn = prefHandler.weekStartWithFallback(locale);
    monthStartsOn = prefHandler.getMonthStart();
    int monthDelta = monthStartsOn - 1;
    int nextWeekEndSqlite;
    int nextWeekStartsSqlite = weekStartsOn - 1; //Sqlite starts with Sunday = 0
    if (weekStartsOn == Calendar.SUNDAY) {
      //weekStartsOn Sunday
      nextWeekEndSqlite = 6;
    } else {
      //weekStartsOn Monday or Saturday
      nextWeekEndSqlite = weekStartsOn - 2;
    }
    YEAR_OF_WEEK_START = "CAST(strftime('%Y',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)";
    YEAR_OF_MONTH_START = "CAST(strftime('%Y',date,'unixepoch','localtime','-" + monthDelta + " day') AS integer)";
    WEEK_START = "date(date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day')";
    THIS_YEAR_OF_WEEK_START = "CAST(strftime('%Y','now','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)";
    WEEK = "CAST((strftime('%j',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') - 1) / 7 + 1 AS integer)"; //calculated for the beginning of the week
    MONTH = "CAST(strftime('%m',date,'unixepoch','localtime','-" + monthDelta + " day') AS integer) - 1"; //convert to 0 based
    THIS_WEEK = "CAST((strftime('%j','now','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') - 1) / 7 + 1 AS integer)";
    THIS_MONTH = "CAST(strftime('%m','now','localtime','-" + monthDelta + " day') AS integer) - 1";
    THIS_YEAR_OF_MONTH_START =  "CAST(strftime('%Y','now','localtime','-" + monthDelta + " day') AS integer)";
    COUNT_FROM_WEEK_START_ZERO = "date('%d-01-01','weekday " + nextWeekStartsSqlite + "', '" +
        "-7 day" +
        "' ,'+%d day')";
    WEEK_START_JULIAN = "julianday(date,'unixepoch','localtime'," + JULIAN_DAY_OFFSET + ",'weekday " + nextWeekEndSqlite + "', '-6 day')";
    WEEK_MAX= "CAST((strftime('%%j','%d-12-31','weekday " + nextWeekEndSqlite + "', '-6 day') - 1) / 7 + 1 AS integer)";
    buildProjection(context);
    isLocalized = true;
  }

  public static void buildProjection(Context context) {
    PROJECTION_BASE = new String[]{
            KEY_ROWID,
            KEY_ACCOUNTID,
            KEY_DATE,
            KEY_VALUE_DATE,
            KEY_AMOUNT + " AS " + KEY_DISPLAY_AMOUNT,
            KEY_COMMENT,
            KEY_CATID,
            KEY_PATH,
            KEY_PAYEEID,
            KEY_PAYEE_NAME,
            KEY_TRANSFER_PEER,
            KEY_TRANSFER_ACCOUNT,
            TRANSFER_ACCOUNT_LABEL,
            KEY_METHODID,
            localizedLabelForPaymentMethod(context, KEY_METHOD_LABEL) + " AS " + KEY_METHOD_LABEL,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            YEAR_OF_WEEK_START + " AS " + KEY_YEAR_OF_WEEK_START,
            YEAR_OF_MONTH_START + " AS " + KEY_YEAR_OF_MONTH_START,
            YEAR + " AS " + KEY_YEAR,
            MONTH + " AS " + KEY_MONTH,
            WEEK + " AS " + KEY_WEEK,
            DAY + " AS " + KEY_DAY,
            THIS_YEAR_OF_WEEK_START + " AS " + KEY_THIS_YEAR_OF_WEEK_START,
            THIS_YEAR_OF_MONTH_START + " AS " + KEY_THIS_YEAR_OF_MONTH_START,
            THIS_YEAR + " AS " + KEY_THIS_YEAR,
            THIS_WEEK + " AS " + KEY_THIS_WEEK,
            THIS_DAY + " AS " + KEY_THIS_DAY,
            WEEK_START + " AS " + KEY_WEEK_START
    };

    //extended
    int baseLength = PROJECTION_BASE.length;
    PROJECTION_EXTENDED = new String[baseLength + 7];
    System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
    PROJECTION_EXTENDED[baseLength] = KEY_COLOR;
    PROJECTION_EXTENDED[baseLength + 1] = KEY_TRANSFER_PEER_IS_PART;
    PROJECTION_EXTENDED[baseLength + 2] = KEY_STATUS;
    PROJECTION_EXTENDED[baseLength + 3] = KEY_ACCOUNT_LABEL;
    PROJECTION_EXTENDED[baseLength + 4] = KEY_ACCOUNT_TYPE;
    PROJECTION_EXTENDED[baseLength + 5] = KEY_TAGLIST;
    PROJECTION_EXTENDED[baseLength + 6] = KEY_PARENTID;

  }


  private static void ensureLocalized() {
    if (!isLocalized) {
      buildLocalized(Locale.getDefault(), MyApplication.Companion.getInstance());
    }
  }

  //if we do not cast the result to integer, we would need to do the conversion in Java
  public static final String YEAR = "CAST(strftime('%Y',date,'unixepoch','localtime') AS integer)";
  public static final String MONTH_PLAIN= "CAST(strftime('%m',date,'unixepoch','localtime') AS integer) - 1"; //convert to 0 based
  public static final String THIS_DAY = "CAST(strftime('%j','now','localtime') AS integer)";
  public static final String DAY = "CAST(strftime('%j',date,'unixepoch','localtime') AS integer)";
  public static final String THIS_YEAR = "CAST(strftime('%Y','now','localtime') AS integer)";
  public static final String DAY_START_JULIAN = "julianday(date,'unixepoch','localtime'," + JULIAN_DAY_OFFSET + ")";
  public static final String KEY_DATE = "date";
  public static final String KEY_VALUE_DATE = "value_date";
  public static final String KEY_AMOUNT = "amount";
  /**
   * alias that we need in order to have a common column name both for
   * 1) home aggregate,
   * 2) all other accounts
   */
  public static final String KEY_DISPLAY_AMOUNT = "display_amount";
  public static final String KEY_COMMENT = "comment";
  public static final String KEY_ROWID = "_id";
  public static final String KEY_CATID = "cat_id";
  public static final String KEY_ACCOUNTID = "account_id";
  public static final String KEY_PAYEEID = "payee_id";
  public static final String KEY_TRANSFER_PEER = "transfer_peer";
  public static final String KEY_METHODID = "method_id";
  public static final String KEY_TITLE = "title";
  public static final String KEY_LABEL = "label";
  public static final String KEY_PATH = "path";
  public static final String KEY_MATCHES_FILTER = "matches";
  public static final String KEY_LEVEL = "level";
  public static final String KEY_COLOR = "color";
  public static final String KEY_TYPE = "type";
  public static final String KEY_FLAG = "flag";
  public static final String KEY_CURRENCY = "currency";
  public static final String KEY_DESCRIPTION = "description";
  public static final String KEY_OPENING_BALANCE = "opening_balance";
  public static final String KEY_EQUIVALENT_OPENING_BALANCE = "equivalent_opening_balance";
  public static final String KEY_USAGES = "usages";
  public static final String KEY_PARENTID = "parent_id";
  public static final String KEY_TRANSFER_ACCOUNT = "transfer_account";
  public static final String KEY_TRANSFER_ACCOUNT_LABEL = "transfer_account_label";
  public static final String KEY_STATUS = "status";
  public static final String KEY_PAYEE_NAME = "name";
  public static final String KEY_SHORT_NAME = "short_name";
  public static final String KEY_METHOD_LABEL = "method_label";
  public static final String KEY_METHOD_ICON = "method_icon";
  public static final String KEY_PAYEE_NAME_NORMALIZED = "name_normalized";
  public static final String KEY_TRANSACTIONID = "transaction_id";
  public static final String KEY_GROUPING = "grouping";
  public static final String KEY_CR_STATUS = "cr_status";
  public static final String KEY_REFERENCE_NUMBER = "number";
  public static final String KEY_IS_NUMBERED = "is_numbered";
  public static final String KEY_PLANID = "plan_id";
  public static final String KEY_PLAN_EXECUTION = "plan_execution";
  public static final String KEY_PLAN_EXECUTION_ADVANCE = "plan_execution_advance";
  public static final String KEY_DEFAULT_ACTION = "default_action";
  public static final String KEY_IS_DEFAULT = "is_default";
  public static final String KEY_TEMPLATEID = "template_id";
  public static final String KEY_INSTANCEID = "instance_id";
  public static final String KEY_CODE = "code";
  public static final String KEY_WEEK_START = "week_start";
  public static final String KEY_GROUP_START = "group_start";
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
  public static final String KEY_THIS_YEAR_OF_MONTH_START = "this_year_of_month_start";
  public static final String KEY_MAX_VALUE = "max_value";
  public static final String KEY_CURRENT_BALANCE = "current_balance";
  public static final String KEY_EQUIVALENT_CURRENT_BALANCE = "equivalent_current_balance";
  public static final String KEY_TOTAL = "total";
  public static final String KEY_EQUIVALENT_TOTAL = "equivalent_total";
  public static final String KEY_CURRENT = "current";
  public static final String KEY_CLEARED_TOTAL = "cleared_total";
  public static final String KEY_RECONCILED_TOTAL = "reconciled_total";
  public static final String KEY_SUM_EXPENSES = "sum_expenses";
  public static final String KEY_SUM_INCOME = "sum_income";
  public static final String KEY_SUM_TRANSFERS = "sum_transfers";
  public static final String KEY_EQUIVALENT_EXPENSES = "equivalent_expenses";
  public static final String KEY_EQUIVALENT_INCOME = "equivalent_income";
  public static final String KEY_EQUIVALENT_TRANSFERS = "equivalent_transfers";
  public static final String KEY_MAPPED_CATEGORIES = "mapped_categories";
  public static final String KEY_MAPPED_PAYEES = "mapped_payees";
  public static final String KEY_MAPPED_METHODS = "mapped_methods";
  public static final String KEY_MAPPED_TEMPLATES = "mapped_templates";
  public static final String KEY_MAPPED_TRANSACTIONS = "mapped_transactions";
  public static final String KEY_MAPPED_BUDGETS = "mapped_budgets";
  public static final String KEY_HAS_CLEARED = "has_cleared";
  public static final String KEY_IS_AGGREGATE = "is_aggregate";
  public static final String KEY_HAS_FUTURE = "has_future"; //has the accounts transactions stored for future dates
  public static final String KEY_SUM = "sum";
  public static final String KEY_SORT_KEY = "sort_key";
  public static final String KEY_EXCLUDE_FROM_TOTALS = "exclude_from_totals";
  public static final String KEY_PREDEFINED_METHOD_NAME = "predefined";
  public static final String KEY_UUID = "uuid";
  public static final String KEY_URI = "uri";

  public static final String KEY_URI_LIST = "uri_list";
  public static final String KEY_ATTACHMENT_COUNT= "attachment_count";
  public static final String KEY_SYNC_ACCOUNT_NAME = "sync_account_name";
  public static final String KEY_TRANSFER_AMOUNT = "transfer_amount";
  public static final String KEY_LABEL_NORMALIZED = "label_normalized";
  public static final String KEY_LAST_USED = "last_used";
  public static final String KEY_HAS_TRANSFERS = "has_transfers";
  public static final String KEY_MAPPED_TAGS = "mapped_tags";
  public static final String KEY_PLAN_INFO = "plan_info";
  public static final String KEY_PARENT_UUID = "parent_uuid";
  public static final String KEY_SYNC_SEQUENCE_LOCAL = "sync_sequence_local";
  public static final String KEY_ACCOUNT_LABEL = "account_label";
  public static final String KEY_ACCOUNT_TYPE = "account_type";
  public static final String KEY_ACCOUNT_UUID = "account_uuid";
  public static final String KEY_IS_SAME_CURRENCY = "is_same_currency";
  public static final String KEY_TIMESTAMP = "timestamp";
  public static final String KEY_KEY = "key";
  public static final String KEY_VALUE = "value";
  public static final String KEY_SORT_DIRECTION = "sort_direction";
  public static final String KEY_SORT_BY = "sort_by";
  public static final String KEY_CURRENCY_SELF = "currency_self";
  public static final String KEY_CURRENCY_OTHER= "currency_other";
  public static final String KEY_EXCHANGE_RATE = "exchange_rate";
  public static final String KEY_ORIGINAL_AMOUNT = "original_amount";
  public static final String KEY_ORIGINAL_CURRENCY = "original_currency";
  public static final String KEY_EQUIVALENT_AMOUNT = "equivalent_amount";
  /*
  true if the transfer peer is either part of a split transaction or part of an archive
   */
  public static final String KEY_TRANSFER_PEER_IS_PART = "transfer_peer_is_part";

  /*
  true if the transfer peer is archived, i.e. part of an archive
   */
  public static final String KEY_TRANSFER_PEER_IS_ARCHIVED = "transfer_peer_is_archived";
  public static final String KEY_BUDGETID = "budget_id";
  public static final String KEY_START = "start";
  public static final String KEY_END = "end";
  public static final String KEY_TAGID = "tag_id";
  public static final String KEY_TRANSFER_CURRENCY = "transfer_currency";
  public static final String KEY_COUNT = "count";
  public static final String KEY_TAGLIST = "tag_list";
  public static final String KEY_DEBT_ID = "debt_id";
  public static final String KEY_MAPPED_DEBTS = "mapped_debts";
  /**
   * If this field is part of a projection for a query to the Methods URI, only payment methods
   * mapped to account types will be returned
   */
  public static final String KEY_ACCOUNT_TYPE_LIST = "account_type_list";

  /**
   * Used for both saving goal and credit limit on accounts
   */
  public static final String KEY_CRITERION = "criterion";

  /**
   * column alias for the second group (month or week)
   */
  public static final String KEY_SECOND_GROUP = "second";

  /**
   * Budget set for the grouping type that is active on an account
   */
  public static final String KEY_BUDGET = "budget";

  /**
   * For each budget allocation, we also store a potential rollover from the previous period
   */
  public static final String KEY_BUDGET_ROLLOVER_PREVIOUS = "rollOverPrevious";

  /**
   * Rollover amounts are stored redundantly, both for the period with the leftover, and for the next
   * period where it rolls to. Thus we can display this information for both periods, without
   * needing to calculate or lookup
   */
  public static final String KEY_BUDGET_ROLLOVER_NEXT = "rollOverNext";

  public static final String KEY_VISIBLE = "visible";
  public static final String KEY_FLAG_LABEL = "flag_label";
  public static final String KEY_FLAG_SORT_KEY = "flag_sort_key";
  public static final String KEY_FLAG_ICON = "flag_icon";
  public static final String METHOD_FLAG_SORT = "flagSort";
  public static final String KEY_SORTED_IDS = "sortedIds";

  /**
   * boolean flag for accounts: A sealed account can no longer be edited
   */
  public static final String KEY_SEALED = "sealed";
  public static final String KEY_HAS_SEALED_DEBT = "hasSealedDebt";
  public static final String KEY_HAS_SEALED_ACCOUNT = "hasSealedAccount";
  public static final String KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER = "hasSealedAccountWithTransfer";
  public static final String KEY_AMOUNT_HOME_EQUIVALENT = "amountHomeEquivalent";

  /**
   * if of a drawable resource representing a category
   */
  public static final String KEY_ICON = "icon";

  public static final String KEY_HAS_DESCENDANTS = "hasDescendants";

  /**
   * flag for budget amounts that only apply to one period
   */
  public static final String KEY_ONE_TIME = "oneTime";

  public static final String KEY_EQUIVALENT_SUM = "equivalentSum";

  /**
   * Bankleitzahl
   */
  public static final String KEY_BLZ = "blz";

  /**
   * Business Identifier Code
   */
  public static final String KEY_BIC = "bic";
  public static final String KEY_BANK_NAME = "name";
  public static final String KEY_USER_ID = "user_id";
  public static final String KEY_BANK_ID = "bank_id";
  public static final String KEY_IBAN = "iban";
  public static final String KEY_ATTRIBUTE_NAME = "attribute_name";
  public static final String KEY_CONTEXT = "context";
  public static final String KEY_ATTRIBUTE_ID = "attribute_id";

  public static final String KEY_ATTACHMENT_ID = "attachment_id";

  public static final String KEY_VERSION = "version";

  // Prices
  public static final String KEY_COMMODITY = "commodity";
  public static final String KEY_SOURCE = "source";

  /**
   * flag for accounts with dynamic exchange rates
   */
  public static final String KEY_DYNAMIC = "dynamic";
  public static final String KEY_LATEST_EXCHANGE_RATE = "latest_exchange_rate";
  public static final String KEY_LATEST_EXCHANGE_RATE_DATE = "latest_exchange_rate_date";


  public static final String KEY_ACCOUNT_TYPE_LABEL = "account_type_label";
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
   * with EXPORT_HANDLE_DELETED_CREATE_HELPER
   */
  public static final int STATUS_HELPER = 3;

  /**
   * Status for the parent archive transaction
   */
  public static final int STATUS_ARCHIVE = 4;

  /**
   * Status for the transactions contained in the archive
   */
  public static final int STATUS_ARCHIVED = 5;

  public static final String TABLE_TRANSACTIONS = "transactions";
  public static final String TABLE_ACCOUNTS = "accounts";
  static final String TABLE_SYNC_STATE = "_sync_state";
  public static final String TABLE_CATEGORIES = "categories";
  public static final String TREE_CATEGORIES = "Tree";
  public static final String TABLE_METHODS = "paymentmethods";
  static final String TABLE_ACCOUNTTYES_METHODS = "accounttype_paymentmethod";
  public static final String TABLE_TEMPLATES = "templates";
  public static final String TABLE_PAYEES = "payee";
  public static final String TABLE_CURRENCIES = "currency";
  public static final String VIEW_COMMITTED = "transactions_committed";
  public static final String VIEW_WITH_ACCOUNT = "transactions_with_account";
  public static final String VIEW_UNCOMMITTED = "transactions_uncommitted";
  public static final String VIEW_ALL = "transactions_all";
  static final String VIEW_TEMPLATES_ALL = "templates_all";
  public static final String VIEW_TEMPLATES_UNCOMMITTED = "templates_uncommitted";
  public static final String VIEW_EXTENDED = "transactions_extended";
  static final String VIEW_CHANGES_EXTENDED = "changes_extended";
  static final String VIEW_TEMPLATES_EXTENDED = "templates_extended";
  public static final String TABLE_PLAN_INSTANCE_STATUS = "planinstance_transaction";
  static final String TABLE_CHANGES = "changes";
  static final String TABLE_SETTINGS = "settings";
  static final String TABLE_ACCOUNT_EXCHANGE_RATES = "account_exchangerates";
  public static final String TABLE_TAGS = "tags";
  public static final String TABLE_TRANSACTIONS_TAGS = "transactions_tags";
  public static final String TABLE_ACCOUNTS_TAGS = "accounts_tags";
  public static final String TABLE_TEMPLATES_TAGS = "templates_tags";
  /**
   * used on backup and restore
   */
  public static final String TABLE_EVENT_CACHE = "event_cache";

  public static final String TABLE_BUDGETS = "budgets";
  public static final String TABLE_BUDGET_ALLOCATIONS = "budget_allocations";

  public static final String TABLE_DEBTS = "debts";

  public static final String TABLE_BANKS = "banks";

  public static final String TABLE_ATTRIBUTES = "attributes";

  public static final String TABLE_ATTACHMENTS = "attachments";

  public static final String TABLE_TRANSACTION_ATTACHMENTS = "transaction_attachments";

  public static final String TABLE_TRANSACTION_ATTRIBUTES = "transaction_attributes";

  public static final String TABLE_ACCOUNT_ATTRIBUTES = "account_attributes";

  public static final String TABLE_EQUIVALENT_AMOUNTS = "equivalent_amounts";

  public static final String TABLE_PRICES = "prices";

  public static final String VIEW_PRIORITIZED_PRICES = "prioritized_prices";

  public static final String CAT_AS_LABEL = DbConstantsKt.fullCatCase(null) + " AS " + KEY_LABEL;

  public static final String TRANSFER_ACCOUNT_UUID = "(SELECT " + KEY_UUID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ") AS " + KEY_TRANSFER_ACCOUNT;

  public static final String TRANSFER_CURRENCY = String.format("(select %1$s from %2$s where %3$s=%4$s) AS %5$s", KEY_CURRENCY, TABLE_ACCOUNTS, KEY_ROWID, KEY_TRANSFER_ACCOUNT, KEY_TRANSFER_CURRENCY);

  public static final String CATEGORY_ICON =
      "CASE WHEN " +
          "  " + KEY_CATID + " " +
          " THEN " +
          "  (SELECT " + KEY_ICON + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
          " ELSE null" +
          " END AS " + KEY_ICON;

  public static final String TABLE_ACCOUNT_TYPES = "account_types";
  public static final String KEY_IS_ASSET = "isAsset";
  public static final String KEY_TYPE_SORT_KEY = "type_sort_key";
  public static final String KEY_SUPPORTS_RECONCILIATION = "supportsReconciliation";
  public static final String METHOD_TYPE_SORT = "typeSort";

  public static final String TABLE_ACCOUNT_FLAGS = "account_flags";

  public static final Long SPLIT_CATID = 0L;
  public static final long NULL_ROW_ID = 0L;
  public static final String NULL_CHANGE_INDICATOR = "__NULL__";
  public static final String WHERE_NOT_SPLIT =
      KEY_CATID + " IS NOT " + SPLIT_CATID;
  public static final String WHERE_NOT_SPLIT_PART =
      KEY_PARENTID + " IS null";
  public static final String WHERE_NOT_VOID =
      KEY_CR_STATUS + " != '" + CrStatus.VOID.name() + "'";
  public static final String WHERE_NOT_ARCHIVED =
    KEY_STATUS + " != " + STATUS_ARCHIVED;
  public static final String WHERE_NOT_ARCHIVE =
          KEY_STATUS + " != " + STATUS_ARCHIVE;

  public static final String WHERE_DEPENDENT = KEY_PARENTID + " = ? OR " + KEY_ROWID + " IN "
      + "(SELECT " + KEY_TRANSFER_PEER + " FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_PARENTID + "= ?)";

  public static final String WHERE_SELF_OR_PEER = KEY_TRANSFER_PEER + " = ? OR " + KEY_ROWID + " = ?";

  public static final String WHERE_SELF_OR_RELATED = WHERE_SELF_OR_PEER + " OR " + WHERE_DEPENDENT;

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


  public static String getThisYearOfMonthStart() {
    ensureLocalized();
    return THIS_YEAR_OF_MONTH_START;
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

  /**
   * we want to find out the week range when we are given a week number
   * we find out the first day in the year, that is the firstdayofweek of the locale and is
   * one week behind the first day with week number 1
   * add (weekNumber)*7 days to get at the beginning of the week
   */
  static String getCountFromWeekStartZero() {
    ensureLocalized();
    return COUNT_FROM_WEEK_START_ZERO;
  }

  static String getWeekMax() {
    ensureLocalized();
    return WEEK_MAX;
  }

  public static String[] getProjectionBase() {
    ensureLocalized();
    return PROJECTION_BASE;
  }

  public static String[] getProjectionExtended() {
    ensureLocalized();
    return PROJECTION_EXTENDED;
  }
}
