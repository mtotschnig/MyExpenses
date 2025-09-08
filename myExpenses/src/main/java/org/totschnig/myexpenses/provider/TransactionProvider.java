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
import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;
import static org.totschnig.myexpenses.provider.ArchiveKt.archive;
import static org.totschnig.myexpenses.provider.ArchiveKt.canBeArchived;
import static org.totschnig.myexpenses.provider.ArchiveKt.unarchive;
import static org.totschnig.myexpenses.provider.DataBaseAccount.HOME_AGGREGATE_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY_OTHER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY_SELF;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_BY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_DIRECTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI_LIST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.METHOD_FLAG_SORT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.METHOD_TYPE_SORT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.NULL_ROW_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS_TAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTTYES_METHODS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_EXCHANGE_RATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_FLAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_TYPES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTACHMENTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BANKS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGETS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGET_ALLOCATIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CHANGES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_EVENT_CACHE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_METHODS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PLAN_INSTANCE_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PRICES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_SETTINGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_SYNC_STATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES_TAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS_TAGS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTACHMENTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_ALL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_CHANGES_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_PRIORITIZED_PRICES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_ALL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_DEPENDENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_SELF_OR_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_SELF_OR_RELATED;
import static org.totschnig.myexpenses.provider.DbConstantsKt.CTE_SEARCH;
import static org.totschnig.myexpenses.provider.DbConstantsKt.accountWithTypeAndFlag;
import static org.totschnig.myexpenses.provider.DbConstantsKt.amountCteForDebts;
import static org.totschnig.myexpenses.provider.DbConstantsKt.budgetAllocation;
import static org.totschnig.myexpenses.provider.DbConstantsKt.buildSearchCte;
import static org.totschnig.myexpenses.provider.DbConstantsKt.categoryPathFromLeave;
import static org.totschnig.myexpenses.provider.DbConstantsKt.categoryTreeSelect;
import static org.totschnig.myexpenses.provider.DbConstantsKt.categoryTreeWithMappedObjects;
import static org.totschnig.myexpenses.provider.DbConstantsKt.categoryTreeWithSum;
import static org.totschnig.myexpenses.provider.DbConstantsKt.equivalentAmountJoin;
import static org.totschnig.myexpenses.provider.DbConstantsKt.exchangeRateJoin;
import static org.totschnig.myexpenses.provider.DbConstantsKt.getAccountSelector;
import static org.totschnig.myexpenses.provider.DbConstantsKt.getPayeeWithDuplicatesCTE;
import static org.totschnig.myexpenses.provider.DbConstantsKt.getTemplateQuerySelector;
import static org.totschnig.myexpenses.provider.DbConstantsKt.totalBudgetAllocation;
import static org.totschnig.myexpenses.provider.DbConstantsKt.transactionListAsCTE;
import static org.totschnig.myexpenses.provider.DbConstantsKt.transactionMappedObjectQuery;
import static org.totschnig.myexpenses.provider.DbConstantsKt.transactionQuerySelector;
import static org.totschnig.myexpenses.provider.DbConstantsKt.transactionSumQuery;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.computeWhere;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.dualQuery;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.groupByForPaymentMethodQuery;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.havingForPaymentMethodQuery;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.mapAccountProjection;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.mapPaymentMethodProjection;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.suggestNewCategoryColor;
import static org.totschnig.myexpenses.provider.MoreDbUtilsKt.tableForPaymentMethodQuery;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.db2.RepositoryPaymentMethodKt;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.Sort;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.filter.Operation;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.Preconditions;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.cursor.PlanInfoCursorWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TransactionProvider extends BaseTransactionProvider {

  public static final String AUTHORITY = BuildConfig.APPLICATION_ID;
  public static final Uri ACCOUNTS_URI =
      Uri.parse("content://" + AUTHORITY + "/accounts");
  //when we need the accounts cursor without the current balance
  //we do not want the cursor to be reloaded when a transaction is added
  //hence we access it through a different URI
  public static final Uri ACCOUNTS_BASE_URI =
      Uri.parse("content://" + AUTHORITY + "/accountsbase");
  public static final Uri ACCOUNTS_AGGREGATE_URI =
      Uri.parse("content://" + AUTHORITY + "/accounts/aggregates");
  //returns accounts with aggregate accounts, limited to id and label
  public static final Uri ACCOUNTS_MINIMAL_URI =
      Uri.parse("content://" + AUTHORITY + "/accountsMinimal");

  public static final Uri TRANSACTIONS_URI =
      Uri.parse("content://" + AUTHORITY + "/transactions");
  public static final Uri UNCOMMITTED_URI =
      Uri.parse("content://" + AUTHORITY + "/transactionsUncommitted");
  public static final Uri EXTENDED_URI =
          TRANSACTIONS_URI.buildUpon().appendQueryParameter(
                  TransactionProvider.QUERY_PARAMETER_EXTENDED, "1").build();
  public static final Uri TEMPLATES_URI =
      Uri.parse("content://" + AUTHORITY + "/templates");
  public static final Uri TEMPLATES_UNCOMMITTED_URI =
      Uri.parse("content://" + AUTHORITY + "/templatesUncommitted");
  public static final Uri CATEGORIES_URI =
      Uri.parse("content://" + AUTHORITY + "/categories");
  public static final Uri PAYEES_URI =
      Uri.parse("content://" + AUTHORITY + "/payees");
  public static final Uri METHODS_URI =
      Uri.parse("content://" + AUTHORITY + "/methods");
  public static final Uri MAPPED_METHODS_URI =
      Uri.parse("content://" + AUTHORITY + "/methods_transactions");
  public static final Uri ACCOUNTTYPES_METHODS_URI =
      Uri.parse("content://" + AUTHORITY + "/accounttypes_methods");
  public static final Uri SQLITE_SEQUENCE_TRANSACTIONS_URI =
      Uri.parse("content://" + AUTHORITY + "/sqlite_sequence/" + TABLE_TRANSACTIONS);
  public static final Uri PLAN_INSTANCE_STATUS_URI =
      Uri.parse("content://" + AUTHORITY + "/planinstance_transaction");

  public static Uri PLAN_INSTANCE_SINGLE_URI(long templateId, long instanceId) {
    return ContentUris.appendId(ContentUris.appendId(
        PLAN_INSTANCE_STATUS_URI.buildUpon(), templateId), instanceId)
        .build();
  }

  public static Uri TRANSACTION_ATTACHMENT_SINGLE_URI(long transactionId, long attachmentId) {
    return ContentUris.appendId(ContentUris.appendId(
                    TRANSACTIONS_ATTACHMENTS_URI.buildUpon(), transactionId), attachmentId)
            .build();
  }

  public static final String URI_SEGMENT_SORT = "sortBy";
  public static final String URI_SEGMENT_GROUPING = "accountGrouping";
  public static final String URI_SEGMENT_SUMS_FOR_ACCOUNTS = "sumsForAccounts";
  public static final String URI_SEGMENT_SUMS_FOR_ARCHIVE = "sumsForArchive";

  public static final Uri CURRENCIES_URI =
      Uri.parse("content://" + AUTHORITY + "/currencies");
  public static final Uri TRANSACTIONS_SUM_URI =
      Uri.parse("content://" + AUTHORITY + "/transactions/" + URI_SEGMENT_SUMS_FOR_ACCOUNTS);
  public static Uri ARCHIVE_SUMS_URI(long archiveId) {
    return Uri.parse("content://" + AUTHORITY + "/transactions/" + archiveId + "/"+ URI_SEGMENT_SUMS_FOR_ARCHIVE);
  }

  public static final Uri EVENT_CACHE_URI =
      Uri.parse("content://" + AUTHORITY + "/eventcache");
  public static final Uri DEBUG_SCHEMA_URI =
      Uri.parse("content://" + AUTHORITY + "/debug_schema");
  public static final Uri STALE_IMAGES_URI =
      Uri.parse("content://" + AUTHORITY + "/stale_images");
  public static final Uri MAPPED_TRANSFER_ACCOUNTS_URI =
      Uri.parse("content://" + AUTHORITY + "/transfer_account_transactions");
  public static final Uri CHANGES_URI = Uri.parse("content://" + AUTHORITY + "/changes");

  public static final Uri SETTINGS_URI = Uri.parse("content://" + AUTHORITY + "/settings");

  public static final Uri AUTOFILL_URI = Uri.parse("content://" + AUTHORITY + "/autofill");
  /**
   * select info from DB without table, e.g. CategoryList#DATEINFO_CURSOR
   * or set control flags like sync_state
   */
  public static final Uri DUAL_URI =
      Uri.parse("content://" + AUTHORITY + "/dual");

  public static final Uri ACCOUNT_EXCHANGE_RATE_URI =
      Uri.parse("content://" + AUTHORITY + "/account_exchangerates");


  public static final Uri SORT_URI =
          Uri.parse("content://" + AUTHORITY + "/" + URI_SEGMENT_SORT);
  public static final Uri ACCOUNT_GROUPINGS_URI =
      Uri.parse("content://" + AUTHORITY + "/" + URI_SEGMENT_GROUPING);

  public static final Uri BUDGETS_URI = Uri.parse("content://" + AUTHORITY + "/budgets");

  public static final Uri TAGS_URI = Uri.parse("content://" + AUTHORITY + "/tags");

  public static final Uri TRANSACTIONS_TAGS_URI = Uri.parse("content://" + AUTHORITY + "/transactions/tags");

  public static final Uri TEMPLATES_TAGS_URI = Uri.parse("content://" + AUTHORITY + "/templates/tags");

  public static final Uri ACCOUNTS_TAGS_URI = Uri.parse("content://" + AUTHORITY + "/accounts/tags");

  public static final Uri DEBTS_URI = Uri.parse("content://" + AUTHORITY + "/debts");

  public static final Uri BANKS_URI = Uri.parse("content://" + AUTHORITY + "/banks");

  public static final Uri TRANSACTIONS_ATTRIBUTES_URI = Uri.parse("content://" + AUTHORITY + "/transactions/attributes");

  public static final Uri ACCOUNTS_ATTRIBUTES_URI = Uri.parse("content://" + AUTHORITY + "/accounts/attributes");

  public static final Uri ATTRIBUTES_URI = Uri.parse("content://" + AUTHORITY + "/attributes");

  public static final Uri ATTACHMENTS_URI = Uri.parse("content://" + AUTHORITY + "/attachments");

  public static final Uri TRANSACTIONS_ATTACHMENTS_URI = Uri.parse("content://" + AUTHORITY + "/transactions/attachments");

  public static final Uri PRICES_URI = Uri.parse("content://" + AUTHORITY + "/prices");
  public static final Uri DYNAMIC_CURRENCIES_URI = Uri.parse("content://" + AUTHORITY + "/dynamicCurrencies");
  public static final Uri ACCOUNT_TYPES_URI = Uri.parse("content://" + AUTHORITY + "/accountTypes");
  public static final Uri ACCOUNT_FLAGS_URI = Uri.parse("content://" + AUTHORITY + "/accountFlags");

  public static final String URI_SEGMENT_MOVE = "move";
  public static final String URI_SEGMENT_TOGGLE_CRSTATUS = "toggleCrStatus";
  public static final String URI_SEGMENT_UNDELETE = "undelete";
  public static final String URI_SEGMENT_INCREASE_USAGE = "increaseUsage";
  public static final String URI_SEGMENT_CHANGE_FRACTION_DIGITS = "changeFractionDigits";
  public static final String URI_SEGMENT_TYPE_FILTER = "typeFilter";
  public static final String URI_SEGMENT_BUDGET_ALLOCATIONS = "allocations";
  public static final String URI_SEGMENT_DEFAULT_BUDGET_ALLOCATIONS = "defaultBudgetAllocations";
  public static final String URI_SEGMENT_UNSPLIT = "unsplit";
  public static final String URI_SEGMENT_LINK_TRANSFER = "link_transfer";
  public static final String URI_SEGMENT_UNLINK_TRANSFER = "unlink_transfer";
  public static final String URI_SEGMENT_TRANSFORM_TO_TRANSFER = "transform_to_transfer";
  public static final String URI_SEGMENT_UNARCHIVE = "unarchive";

  public static final Uri BUDGET_ALLOCATIONS_URI = Uri.parse("content://" + AUTHORITY + "/budgets/" + URI_SEGMENT_BUDGET_ALLOCATIONS);

  //"1" merge all currency aggregates, < 0 only return one specific aggregate
  public static final String QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES = "mergeCurrencyAggregates";
  public static final String QUERY_PARAMETER_FULL_PROJECTION_WITH_SUMS = "fullProjectionWithSums";
  public static final String QUERY_PARAMETER_BALANCE_SHEET = "balanceSheet";
  //uses full projection with sums for each account
  public static final Uri ACCOUNTS_FULL_URI = BaseTransactionProvider.Companion.balanceUri("now", false);
  public static final String QUERY_PARAMETER_EXTENDED = "extended";
  public static final String QUERY_PARAMETER_DISTINCT = "distinct";
  public static final String QUERY_PARAMETER_GROUP_BY = "groupBy";
  public static final String QUERY_PARAMETER_MARK_VOID = "markVoid";
  //"1" from production, "2" from test
  public static final String QUERY_PARAMETER_WITH_PLAN_INFO = "withPlanInfo";
  public static final String QUERY_PARAMETER_INIT = "init";
  public static final String QUERY_PARAMETER_CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";

  public static final String QUERY_PARAMETER_CALLER_IS_IN_BULK = "caller_is_in_bulk";
  private static final String QUERY_PARAMETER_SYNC_BEGIN = "syncBegin";
  private static final String QUERY_PARAMETER_SYNC_END = "syncEnd";
  public static final String QUERY_PARAMETER_WITH_JULIAN_START = "withJulianStart";
  public static final String QUERY_PARAMETER_WITH_COUNT = "count";
  public static final String QUERY_PARAMETER_WITH_INSTANCE = "withInstance";
  public static final String QUERY_PARAMETER_HIERARCHICAL = "hierarchical";
  public static final String QUERY_PARAMETER_CATEGORY_SEPARATOR = "categorySeparator";
  public static final String QUERY_PARAMETER_SHORTEN_COMMENT = "shortenComment";
  public static final String QUERY_PARAMETER_SEARCH = "search";
  public static final String QUERY_PARAMETER_COUNT_UNUSED = "countUnused";
  /**
   * do not honour EXCLUDE_FROM_TOTAL flag
   */
  public static final String QUERY_PARAMETER_INCLUDE_ALL = "includeAll";

  /**
   * 1 -> mapped objects for each row
   * 2 -> aggregate sums for all mapped objects
   */
  public static final String QUERY_PARAMETER_MAPPED_OBJECTS = "mappedObjects";

  /**
   * Transfers are included into in and out sums, instead of reported in extra field
   */
  public static final String QUERY_PARAMETER_INCLUDE_TRANSFERS = "includeTransfers";

  public static final String QUERY_PARAMETER_AGGREGATE_NEUTRAL = "aggregateNeutral";

  /**
   * Colon separated list of account types
   */
  public static final String QUERY_PARAMETER_ACCOUNT_TYPE_LIST = "accountTypeList";

  public static final String QUERY_PARAMETER_WITH_FILTER = "withFilter";

  public static final String QUERY_PARAMETER_TRANSACTION_ID_LIST = "transaction_id_list";
  @Deprecated
  public static final String METHOD_BULK_START = "bulkStart";
  @Deprecated
  public static final String METHOD_BULK_END = "bulkEnd";
  public static final String METHOD_SORT_ACCOUNTS = "sort_accounts";
  public static final String METHOD_SETUP_CATEGORIES = "setup_categories";
  public static final String METHOD_SETUP_CATEGORIES_DRY_RUN = "setup_categories_dry_run";
  public static final String METHOD_CHECK_CORRUPTED_DATA_987 = "checkCorruptedData";

  public static final String METHOD_DELETE_ATTACHMENTS = "deleteAttachments";

  public static final String METHOD_SAVE_CATEGORY = "saveCategory";
  public static final String KEY_CATEGORY = "category";
  public static final String METHOD_ENSURE_CATEGORY = "ensureCategory";
  public static final String KEY_CATEGORY_INFO = "categoryInfo";
  public static final String METHOD_ENSURE_CATEGORY_TREE = "ensureCategoryTree";
  public static final String KEY_CATEGORY_EXPORT = "categoryExport";
  public static final String METHOD_SAVE_TRANSACTION_TAGS = "saveTransactionTags";
  public static final String KEY_REPLACE = "replace";

  public static final String KEY_RESULT = "result";

  public static final String METHOD_MERGE_CATEGORIES = "mergeCategories";
  public static final String KEY_MERGE_SOURCE = "mergeSource";
  public static final String KEY_MERGE_TARGET = "mergeTarget";

  public static final String METHOD_ARCHIVE = "archive";
  public static final String METHOD_CAN_BE_ARCHIVED = "canBeArchived";

  public static final String METHOD_RECALCULATE_EQUIVALENT_AMOUNTS = "recalculateEquivalentAmounts";
  public static final String METHOD_RECALCULATE_EQUIVALENT_AMOUNTS_FOR_DATE = "recalculateEquivalentAmountsForDate";

  public static final String METHOD_CLEANUP_UNUSED_PAYEES = "cleanupUnusedPayees";

  private static final UriMatcher URI_MATCHER;

  @Override
  public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                      @Nullable String[] selectionArgs, @Nullable String sortOrder) {
    SupportSQLiteQueryBuilder qb;
    StringBuilder additionalWhere = new StringBuilder();
    SupportSQLiteDatabase db;
    db = getHelper().getReadableDatabase();

    Cursor c;

    String groupBy = uri.getQueryParameter(QUERY_PARAMETER_GROUP_BY);
    String having = null;
    String limit = null;
    String cte = null;
    Bundle extras = new Bundle();

    String aggregateFunction = getAggregateFunction();

    int uriMatch = URI_MATCHER.match(uri);
    //noinspection InlinedApi
    String queryParameterLimit = uri.getQueryParameter(ContentResolver.QUERY_ARG_LIMIT);
    if (queryParameterLimit != null) {
      //noinspection InlinedApi
      String queryParameterOffset = uri.getQueryParameter(ContentResolver.QUERY_ARG_OFFSET);
      limit = (queryParameterOffset == null) ? queryParameterLimit : queryParameterOffset + "," + queryParameterLimit;
      log("limit %s", limit);
    }
    switch (uriMatch) {
      case TRANSACTIONS: {
        if (uri.getBooleanQueryParameter(QUERY_PARAMETER_MAPPED_OBJECTS, false)) {
          if (selection != null) CrashHandler.throwOrReport("mapped object query ignores selection");
          String sql = transactionMappedObjectQuery(getAccountSelector(uri));
          c = measureAndLogQuery(db, uri, sql, selection, selectionArgs);
          return c;
        }
        boolean hasSearch = uri.getBooleanQueryParameter(QUERY_PARAMETER_SEARCH, false);
        String forCatId = uri.getQueryParameter(KEY_CATID);
        boolean extended = uri.getQueryParameter(QUERY_PARAMETER_EXTENDED) != null;
        String table = extended ? VIEW_EXTENDED : VIEW_COMMITTED;

        if (projection == null) {
          projection = extended ? DatabaseConstants.getProjectionExtended() : DatabaseConstants.getProjectionBase();
        }
        if (sortOrder == null) {
          sortOrder = KEY_DATE + " DESC";
        }
        boolean forHome = uri.getQueryParameter(KEY_ACCOUNTID) == null && uri.getQueryParameter(KEY_CURRENCY) == null && uri.getQueryParameter(KEY_PARENTID) == null;
        if (forCatId != null) {
          String selector = transactionQuerySelector(uri, CTE_SEARCH);
          selection = TextUtils.isEmpty(selection) ? selector : (TextUtils.isEmpty(selector) ? selection : (selection + " AND " + selector));
          projection = prepareProjectionForTransactions(projection, CTE_SEARCH, uri.getBooleanQueryParameter(QUERY_PARAMETER_SHORTEN_COMMENT, false), false);
          String sql = transactionListAsCTE(forCatId, forHome  ? getHomeCurrency() : null) + " " + SupportSQLiteQueryBuilder.builder(CTE_SEARCH).columns(projection)
                  .selection(computeWhere(selection, KEY_CATID + " IN (SELECT " + KEY_ROWID + " FROM Tree )"), selectionArgs).groupBy(groupBy)
                  .orderBy(sortOrder).create().getSql();
          c = measureAndLogQuery(db, uri, sql, selection, selectionArgs);
          return c;
        }
        String tableForQueryBuilder;
        if (hasSearch) {
            cte = buildSearchCte(table, getHomeCurrency(), forHome);
            table = CTE_SEARCH;
            tableForQueryBuilder = CTE_SEARCH;
        } else {
          tableForQueryBuilder = needsExtendedJoin(projection) ?
                  exchangeRateJoin(table, KEY_ACCOUNTID, getHomeCurrency(), table) + equivalentAmountJoin(getHomeCurrency()) : table;
        }
        String selector = transactionQuerySelector(uri, table);
        selection = TextUtils.isEmpty(selection) ? selector : (TextUtils.isEmpty(selector) ? selection : (selection + " AND " + selector));
        projection = prepareProjectionForTransactions(projection, table, uri.getBooleanQueryParameter(QUERY_PARAMETER_SHORTEN_COMMENT, false), !hasSearch);
        qb = SupportSQLiteQueryBuilder.builder(tableForQueryBuilder);
        if (uri.getQueryParameter(QUERY_PARAMETER_DISTINCT) != null) {
          qb.distinct();
        }
        break;
      }
      case UNCOMMITTED:
        qb = SupportSQLiteQueryBuilder.builder(VIEW_UNCOMMITTED);
        if (projection == null)
          projection = DatabaseConstants.getProjectionBase();
        break;
      case TRANSACTION_ID:
        qb = SupportSQLiteQueryBuilder.builder(VIEW_ALL  + equivalentAmountJoin(getHomeCurrency()));
        projection = prepareProjectionForTransactions(projection, VIEW_ALL, false, true);
        additionalWhere.append(KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        break;
      case TRANSACTIONS_SUMS: {
        // if type flag is passed in, then we only return one type, otherwise two rows for expense and income are returned
        String sql = transactionSumQuery(
                uri,
                projection,
                selection,
                getTypeWithFallBack(),
                aggregateFunction,
                getHomeCurrency()
        );
        c = measureAndLogQuery(db, uri, sql, selection, selectionArgs);
        return c;
      }
      case ARCHIVE_SUMS: {
        return archiveSumQuery(db, uri, getTypeWithFallBack());
      }
      case TRANSACTIONS_GROUPS: {
        return transactionGroupsQuery(db, uri, selection, selectionArgs);
      }
      case CATEGORIES: {
        String mappedObjects = uri.getQueryParameter(QUERY_PARAMETER_MAPPED_OBJECTS);
        if (mappedObjects != null) {
          String sql = categoryTreeWithMappedObjects(selection, projection, mappedObjects.equals("2"));
          return measureAndLogQuery(db, uri, sql, selection, selectionArgs);
        }
        if (uri.getBooleanQueryParameter(QUERY_PARAMETER_HIERARCHICAL, false)) {
          final boolean withSum = projection != null && Arrays.asList(projection).contains(KEY_SUM);

          String withType = uri.getQueryParameter(KEY_TYPE);
          String sql = withSum ?
                  categoryTreeWithSum(
                          aggregateFunction,
                          currencyContext.getHomeCurrencyString(),
                          sortOrder,
                          selection,
                          projection,
                          uri
                  ) :
                  categoryTreeSelect(
                          sortOrder,
                          selection,
                          projection,
                          null,
                          null,
                          uri.getQueryParameter(QUERY_PARAMETER_CATEGORY_SEPARATOR),
                          withType
                  );
          c = measureAndLogQuery(db, uri, sql, selection, selectionArgs);
          c.setNotificationUri(getContext().getContentResolver(), uri);
          return (withType != null && c.getCount() == 0) ? wrapWithResultCompat(c, hasCategories(db)) :  c;
        } else {
          qb = SupportSQLiteQueryBuilder.builder(TABLE_CATEGORIES);
          //split transaction: list Categories of split parts
          String transactionId = uri.getQueryParameter(KEY_TRANSACTIONID);
          if (transactionId != null) {
            selection = KEY_ROWID
                    + " IN (SELECT distinct "
                    + KEY_CATID
                    + " FROM "
                    + TABLE_TRANSACTIONS
                    + " WHERE "
                    + KEY_PARENTID
                    + " = ?)";
            selectionArgs = new String[]{transactionId};
          } else {
              additionalWhere.append(KEY_ROWID).append(" != ").append(SPLIT_CATID);
          }
          if (projection == null) {
            projection = new String[]{KEY_ROWID, KEY_LABEL, KEY_PARENTID};
          }
          break;
        }
      }
      case CATEGORY_ID:
        String rowId = uri.getPathSegments().get(1);
        if (uri.getBooleanQueryParameter(QUERY_PARAMETER_HIERARCHICAL, false)) {
          c = measureAndLogQuery(db, uri, categoryPathFromLeave(rowId), selection, selectionArgs);
          c.setNotificationUri(getContext().getContentResolver(), uri);
          return c;
        } else {
          qb = SupportSQLiteQueryBuilder.builder(TABLE_CATEGORIES);
          additionalWhere.append(KEY_ROWID + "=").append(rowId);
          break;
        }
      case ACCOUNTS:
      case ACCOUNTS_BASE:
      case ACCOUNTS_MINIMAL:
        final boolean minimal = uriMatch == ACCOUNTS_MINIMAL;
        final String sumsForDate = uri.getQueryParameter(QUERY_PARAMETER_FULL_PROJECTION_WITH_SUMS);
        final String mergeAggregate = uri.getQueryParameter(QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES);
        if (mergeAggregate != null || sumsForDate != null) {
          if (sortOrder == null) {
            sortOrder = minimal ? KEY_LABEL :
                    Sort.Companion.preferredOrderByForAccounts(PrefKey.SORT_ORDER_ACCOUNTS, prefHandler, Sort.LABEL, getCollate(), null);
            if (uri.getBooleanQueryParameter(QUERY_PARAMETER_BALANCE_SHEET, false)) {
              sortOrder = KEY_TYPE_SORT_KEY + " DESC, " + sortOrder;
            }
          }
          if (projection != null) {
            CrashHandler.throwOrReport(
                    "When calling accounts cursor with sums or aggregates, projection is ignored ", TAG
            );
          }
          String sql = buildAccountQuery(minimal, mergeAggregate, selection, sortOrder, sumsForDate);
          c = measureAndLogQuery(db, uri, sql, selection, selectionArgs);
          c.setNotificationUri(getContext().getContentResolver(), uri);
          return c;
        } else {
          if (sortOrder == null) {
            sortOrder = minimal ? (TABLE_ACCOUNTS + "." + KEY_LABEL) :
                    Sort.Companion.preferredOrderByForAccounts(PrefKey.SORT_ORDER_ACCOUNTS, prefHandler, Sort.LABEL, getCollate(), TABLE_ACCOUNTS);
            sortOrder = KEY_FLAG_SORT_KEY + " DESC, " + sortOrder;
          }
          qb = SupportSQLiteQueryBuilder.builder(minimal ? accountWithTypeAndFlag : getAccountsWithExchangeRate());
          if (projection == null) {
              projection =  org.totschnig.myexpenses.model2.Account.Companion.getProjection(minimal);
          }
          projection = mapAccountProjection(projection);
          break;
        }

      case AGGREGATE_ID:
        String currencyId = uri.getPathSegments().get(2);
        if (Integer.parseInt(currencyId) == HOME_AGGREGATE_ID) {
          qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNTS);
          projection = aggregateHomeProjection(projection);
        } else {
          qb = SupportSQLiteQueryBuilder.builder(TABLE_CURRENCIES);
          projection = aggregateProjection(projection);
          additionalWhere.append(TABLE_CURRENCIES + "." + KEY_ROWID + "= abs(").append(currencyId).append(")");
        }
        break;
      case ACCOUNT_ID:
        qb = SupportSQLiteQueryBuilder.builder(getAccountsWithExchangeRate());
        additionalWhere.append(TABLE_ACCOUNTS + "." + KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        projection = mapAccountProjection(projection);
        break;
      case PAYEES:
        if (uri.getBooleanQueryParameter(QUERY_PARAMETER_HIERARCHICAL, false)) {
          String sql = getPayeeWithDuplicatesCTE(selection, getCollate());
          c = measureAndLogQuery(db, uri, sql, selection, selectionArgs);
          c.setNotificationUri(getContext().getContentResolver(), uri);
          return c;
        }
        if (uri.getBooleanQueryParameter(QUERY_PARAMETER_COUNT_UNUSED, false)) {
          c = measureAndLogQuery(db, uri, COUNT_UNUSED_PAYEE_QUERY, selection, selectionArgs);
          c.setNotificationUri(getContext().getContentResolver(), uri);
          return c;
        }

        qb = SupportSQLiteQueryBuilder.builder(TABLE_PAYEES);
        if (sortOrder == null) {
          sortOrder = KEY_PAYEE_NAME + " COLLATE " + getCollate();
        }
        if (projection == null)
          projection = Companion.payeeProjection(TABLE_PAYEES);
        additionalWhere.append(KEY_ROWID + "!=" + NULL_ROW_ID);
        break;
      case PAYEE_ID:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_PAYEES);
        additionalWhere.append(KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        break;
      case MAPPED_TRANSFER_ACCOUNTS:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNTS + " JOIN " + TABLE_TRANSACTIONS + " ON (" + KEY_TRANSFER_ACCOUNT + " = " + TABLE_ACCOUNTS + "." + KEY_ROWID + ")");
        projection = new String[]{"DISTINCT " + TABLE_ACCOUNTS + "." + KEY_ROWID, KEY_LABEL};
        if (sortOrder == null) {
          sortOrder = KEY_LABEL;
        }
        break;
      case METHODS:
        qb = SupportSQLiteQueryBuilder.builder(tableForPaymentMethodQuery(projection));
        groupBy = groupByForPaymentMethodQuery(projection);
        having = havingForPaymentMethodQuery(projection);
        if (projection == null) {
          projection = RepositoryPaymentMethodKt.fullProjection(getWrappedContext());
        } else {
          projection = mapPaymentMethodProjection(projection, getWrappedContext());
        }
        if (sortOrder == null) {
          sortOrder = RepositoryPaymentMethodKt.localizedLabelForPaymentMethod(getWrappedContext(), KEY_LABEL) + " COLLATE " + getCollate();
        }
        additionalWhere.append(KEY_ROWID + "!=" + NULL_ROW_ID);
        break;
      case MAPPED_METHODS:
        String localizedLabel = RepositoryPaymentMethodKt.localizedLabelForPaymentMethod(getWrappedContext(), KEY_LABEL);
        qb = SupportSQLiteQueryBuilder.builder(TABLE_METHODS + " JOIN " + TABLE_TRANSACTIONS + " ON (" + KEY_METHODID + " = " + TABLE_METHODS + "." + KEY_ROWID + ")");
        projection = new String[]{"DISTINCT " + TABLE_METHODS + "." + KEY_ROWID, localizedLabel + " AS " + KEY_LABEL};
        if (sortOrder == null) {
          sortOrder = localizedLabel + " COLLATE " + getCollate();
        }
        break;
      case METHOD_ID:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_METHODS);
        if (projection == null)
          projection =  RepositoryPaymentMethodKt.basePaymentMethodProjection(getWrappedContext());
        additionalWhere.append(KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        break;
      case METHODS_FILTERED:
        localizedLabel = RepositoryPaymentMethodKt.localizedLabelForPaymentMethod(getWrappedContext(), KEY_LABEL);
        qb = SupportSQLiteQueryBuilder.builder(TABLE_METHODS + " JOIN " + TABLE_ACCOUNTTYES_METHODS + " ON (" + KEY_ROWID + " = " + KEY_METHODID + ")");
        projection = new String[]{KEY_ROWID, localizedLabel + " AS " + KEY_LABEL, KEY_IS_NUMBERED};
        String paymentType = uri.getPathSegments().get(2);
        String typeSelect;
        switch (paymentType) {
          case "1" -> typeSelect = "> -1";
          case "-1" -> typeSelect = "< 1";
          default -> typeSelect = "= 0";
        }
        selection = String.format("%s.%s %s", TABLE_METHODS, KEY_TYPE, typeSelect);
        String[] accountTypes = uri.getQueryParameter(QUERY_PARAMETER_ACCOUNT_TYPE_LIST).split(";");

        selection += " AND " + TABLE_ACCOUNTTYES_METHODS + ".type " + Operation.IN.getOp(accountTypes.length);
        selectionArgs = accountTypes;
        if (sortOrder == null) {
          sortOrder = localizedLabel + " COLLATE " + getCollate();
        }
        groupBy = KEY_ROWID;
        having = "count(*) = " + accountTypes.length;
        break;
      case ACCOUNTTYPES_METHODS:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNTTYES_METHODS);
        break;
      case TEMPLATES:
        String instanceId = uri.getQueryParameter(QUERY_PARAMETER_WITH_INSTANCE);
        if (instanceId == null) {
          String selector = getTemplateQuerySelector(uri);
          selection = TextUtils.isEmpty(selection) ? selector : (selection +
                  (TextUtils.isEmpty(selector) ? "" : " AND " + selector));
          qb = SupportSQLiteQueryBuilder.builder(VIEW_TEMPLATES_EXTENDED);
          if (projection == null) {
            projection = templateProjection(VIEW_TEMPLATES_EXTENDED);
          }
        } else {
          qb = SupportSQLiteQueryBuilder.builder(String.format(Locale.ROOT, "%1$s LEFT JOIN %2$s ON %1$s.%3$s = %4$s AND %5$s = %6$s LEFT JOIN %7$s ON %7$s.%3$s = %2$s.%8$s",
              VIEW_TEMPLATES_EXTENDED, TABLE_PLAN_INSTANCE_STATUS, KEY_ROWID, KEY_TEMPLATEID, KEY_INSTANCEID, instanceId,
              TABLE_TRANSACTIONS, KEY_TRANSACTIONID));
          if (projection != null) {
            report("When calling templates cursor with QUERY_PARAMETER_WITH_INSTANCE, projection is ignored ");
          }
          projection = new String[]{KEY_TITLE, KEY_INSTANCEID, KEY_TRANSACTIONID, KEY_COLOR, KEY_CURRENCY,
              String.format(Locale.ROOT, "coalesce(%1$s.%2$s, %3$s.%2$s) AS %2$s", TABLE_TRANSACTIONS, KEY_AMOUNT, VIEW_TEMPLATES_EXTENDED),
              VIEW_TEMPLATES_EXTENDED + "." + KEY_ROWID + " AS " + KEY_ROWID, KEY_SEALED};
        }

        break;
      case TEMPLATES_UNCOMMITTED:
        qb = SupportSQLiteQueryBuilder.builder(VIEW_TEMPLATES_UNCOMMITTED);
        if (projection == null)
          projection = templateProjection(null);
        break;
      case TEMPLATE_ID:
        qb = SupportSQLiteQueryBuilder.builder(VIEW_TEMPLATES_ALL);
        additionalWhere.append(KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        if (projection == null) {
          projection = templateProjection(VIEW_TEMPLATES_ALL);
        }
        break;
      case SQLITE_SEQUENCE_TABLE:
        qb = SupportSQLiteQueryBuilder.builder("SQLITE_SEQUENCE");
        projection = new String[]{"seq"};
        selection = "name = ?";
        selectionArgs = new String[]{uri.getPathSegments().get(1)};
        break;
      case PLANINSTANCE_TRANSACTION_STATUS:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_PLAN_INSTANCE_STATUS);
        break;
      case PLANINSTANCE_STATUS_SINGLE:
        qb = SupportSQLiteQueryBuilder.builder(String.format(Locale.ROOT, "%1$s LEFT JOIN %2$s ON %3$s = %4$s", TABLE_PLAN_INSTANCE_STATUS, TABLE_TRANSACTIONS, KEY_ROWID, KEY_TRANSACTIONID));
        additionalWhere.append(String.format(Locale.ROOT, "%s = %s AND %s = %s", KEY_TEMPLATEID,
            uri.getPathSegments().get(1), KEY_INSTANCEID, uri.getPathSegments().get(2)));
        projection = new String[]{KEY_TRANSACTIONID, KEY_AMOUNT};
        break;
      //only called from unit test
      case CURRENCIES:
        if (projection == null) {
          projection = new String[] {
              KEY_ROWID, KEY_CODE, KEY_GROUPING, KEY_LABEL, KEY_USAGES
          };
          qb = SupportSQLiteQueryBuilder.builder(CURRENCIES_USAGES_TABLE_EXPRESSION);
        } else {
          qb = SupportSQLiteQueryBuilder.builder(TABLE_CURRENCIES);
        }
        break;
      case DUAL:
        return dualQuery(db, projection);
      case EVENT_CACHE:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_EVENT_CACHE);
        break;
      case DEBUG_SCHEMA:
        return db.query(SupportSQLiteQueryBuilder.builder("sqlite_master").columns(new String[]{"name", "sql"}).selection("type = 'table'", new Object[]{}).create());
      case STALE_IMAGES:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ATTACHMENTS);
        selection = Companion.getSTALE_ATTACHMENT_SELECTION();
        if (projection == null)
          projection = new String[]{KEY_ROWID, KEY_URI};
        break;
      case STALE_IMAGES_ID:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ATTACHMENTS);
        selection = Companion.getSTALE_ATTACHMENT_SELECTION() + " AND " + KEY_ROWID + " = ?";
        selectionArgs = new String[] { uri.getPathSegments().get(1) };
        projection = new String[]{KEY_URI};
        break;
      case CHANGES:
        selection = KEY_ACCOUNTID + " = ? AND " + KEY_SYNC_SEQUENCE_LOCAL + " = ?";
        selectionArgs = new String[]{uri.getQueryParameter(KEY_ACCOUNTID), uri.getQueryParameter(KEY_SYNC_SEQUENCE_LOCAL)};
        qb = SupportSQLiteQueryBuilder.builder(VIEW_CHANGES_EXTENDED);
        if (projection == null) {
          projection = TransactionChange.PROJECTION;
        }
        break;
      case SETTINGS: {
        qb = SupportSQLiteQueryBuilder.builder(TABLE_SETTINGS);
        break;
      }
      case CURRENCIES_CODE: {
        qb = SupportSQLiteQueryBuilder.builder(TABLE_CURRENCIES);
        selection = KEY_CODE + " = ?";
        selectionArgs = new String[]{uri.getPathSegments().get(1)};
        break;
      }
      case AUTOFILL:
        qb = SupportSQLiteQueryBuilder.builder(VIEW_EXTENDED);
        selection = KEY_ROWID + "= (SELECT max(" + KEY_ROWID + ") FROM " + TABLE_TRANSACTIONS
            + " WHERE " + WHERE_NOT_SPLIT + " AND " + KEY_PAYEEID + " IN (?, (SELECT " + KEY_PARENTID  +" FROM " + TABLE_PAYEES +
                " WHERE " + KEY_ROWID + " = ?)))";
        String id = uri.getPathSegments().get(1);
        selectionArgs = new String[]{id,id};
        break;
      case ACCOUNT_EXCHANGE_RATE:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNT_EXCHANGE_RATES);
        additionalWhere.append(KEY_ACCOUNTID + "=").append(uri.getPathSegments().get(1))
                .append(" AND " + KEY_CURRENCY_SELF + "='").append(uri.getPathSegments().get(2)).append("'")
                .append(" AND " + KEY_CURRENCY_OTHER + "='").append(uri.getPathSegments().get(3)).append("'");
        projection = new String[]{KEY_EXCHANGE_RATE};
        break;
      case BUDGETS:
        qb = SupportSQLiteQueryBuilder.builder(getBudgetTableJoin());
        projection = projection == null ? getBudgetProjection() : projection;
        break;
      case BUDGET_ID:
        qb = SupportSQLiteQueryBuilder.builder(getBudgetTableJoin());
        projection = projection == null ? getBudgetProjection() : projection;
        additionalWhere.append(TABLE_BUDGETS + "." + KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        break;
      case ACCOUNT_DEFAULT_BUDGET_ALLOCATIONS: {
        qb = SupportSQLiteQueryBuilder.builder(TABLE_BUDGET_ALLOCATIONS);
        Long budgetId = budgetDefaultSelect(db, uri);
        if (budgetId == null) {
          return new MatrixCursor(projection, 0);
        }
        selection = KEY_CATID + " = 0 AND " + KEY_BUDGETID + " = ?";
        selectionArgs = new String[] { budgetId.toString() };
        extras.putLong(KEY_BUDGETID, budgetId);
        break;
      }
      case BUDGET_FOR_PERIOD: {
        String sql = (projection != null && projection.length == 1 && projection[0].equals(KEY_BUDGET)) ? totalBudgetAllocation(uri) : budgetAllocation(uri);
        c = measureAndLogQuery(db, uri, sql, null, null);
        return c;
      }
      case TAGS:
        boolean withCount = uri.getBooleanQueryParameter(QUERY_PARAMETER_WITH_COUNT, false);
        boolean withFilter = uri.getBooleanQueryParameter(QUERY_PARAMETER_WITH_FILTER, false);
        String tableName;
        if (withCount) {
          tableName = TABLE_TAGS + " LEFT JOIN " + TABLE_TRANSACTIONS_TAGS + " ON (" + KEY_ROWID + " = " + KEY_TAGID + ")";
          projection = new String[]{KEY_ROWID, KEY_LABEL, KEY_COLOR, String.format("count(%s) AS %s", KEY_TAGID, KEY_COUNT)};
          groupBy = KEY_ROWID;
        }
        else if (withFilter) {
          tableName = TABLE_TAGS + " LEFT JOIN " + TABLE_TRANSACTIONS_TAGS + " ON (" + TABLE_TAGS + "." + KEY_ROWID + " = " + KEY_TAGID + ") LEFT JOIN " +
                  TABLE_TRANSACTIONS + " ON (" + TABLE_TRANSACTIONS + "." + KEY_ROWID + " = " + KEY_TRANSACTIONID  + ")";
          projection = new String[]{TABLE_TAGS + "." + KEY_ROWID, KEY_LABEL, KEY_COLOR };
          groupBy = TABLE_TAGS + "." + KEY_ROWID;
        } else {
          tableName = TABLE_TAGS;
        }
        qb = SupportSQLiteQueryBuilder.builder(tableName);
        break;
      case TRANSACTIONS_TAGS:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_TRANSACTIONS_TAGS + " LEFT JOIN " + TABLE_TAGS + " ON (" + KEY_TAGID + " = " + KEY_ROWID + ")");
        break;
      case TEMPLATES_TAGS:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_TEMPLATES_TAGS + " LEFT JOIN " + TABLE_TAGS + " ON (" + KEY_TAGID + " = " + KEY_ROWID + ")");
        break;
      case ACCOUNTS_TAGS:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNTS_TAGS + " LEFT JOIN " + TABLE_TAGS + " ON (" + KEY_TAGID + " = " + KEY_ROWID + ")");
        break;
      case TAG_ID:
        qb = SupportSQLiteQueryBuilder.builder(TABLE_TAGS);
        additionalWhere.append(KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        break;
      case DEBTS: {
        String date = uri.getQueryParameter(KEY_DATE);
        String dateExpression = date != null ? "cast(strftime('%s', '" + date + "', 'localtime', 'start of day', '+1 day', '-1 second', 'utc') as integer)" : null;
        cte = amountCteForDebts(getHomeCurrency(), dateExpression);
        //for the moment only one of queryParameters KEY_TRANSACTIONID, KEY_DATE can be used
        String transactionId = uri.getQueryParameter(KEY_TRANSACTIONID);
        if (transactionId != null) {
          additionalWhere.append("not exists(SELECT 1 FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_DEBT_ID + " IS NOT NULL AND " + KEY_PARENTID + " = ")
                  .append(transactionId).append(")");
        } else if (dateExpression != null) {
          additionalWhere.append(KEY_DATE).append(" <= ").append(dateExpression);
        }
        if (projection == null) {
          projection = debtProjection(transactionId, true);
        }
        qb = SupportSQLiteQueryBuilder.builder(DEBT_PAYEE_JOIN);
        break;
      }
      case DEBT_ID: {
        if (projection == null) {
          projection = debtProjection(null, false);
        }
        qb = SupportSQLiteQueryBuilder.builder(DEBT_PAYEE_JOIN);
        additionalWhere.append(TABLE_DEBTS + "." + KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        break;
      }
      case BANKS: {
        projection = Companion.getBANK_PROJECTION();
        qb = SupportSQLiteQueryBuilder.builder(TABLE_BANKS);
        break;
      }
      case TRANSACTION_ATTRIBUTES: {
        qb = SupportSQLiteQueryBuilder.builder(TRANSACTION_ATTRIBUTES_JOIN);
        break;
      }
      case ACCOUNT_ATTRIBUTES: {
        qb = SupportSQLiteQueryBuilder.builder(ACCOUNT_ATTRIBUTES_JOIN);
        break;
      }
      case ATTACHMENTS: {
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ATTACHMENTS);
        if (selection == null) {
          String uuidSelection = uri.getQueryParameter(KEY_UUID);
          selection = Companion.LIVE_ATTACHMENT_SELECTION(uuidSelection != null);
          selectionArgs = uuidSelection != null ? new String[]{uuidSelection} : null;
        }
        break;
      }
      case TRANSACTION_ATTACHMENTS: {
        if (projection == null) {
          projection = new String[] { KEY_URI };
        }
        qb = SupportSQLiteQueryBuilder.builder(TABLE_TRANSACTION_ATTACHMENTS + " LEFT JOIN " + TABLE_ATTACHMENTS + " ON (" + KEY_ATTACHMENT_ID + " = " + KEY_ROWID + ")");
        break;
      }
      case BUDGET_ALLOCATIONS : {
        qb = SupportSQLiteQueryBuilder.builder(TABLE_BUDGET_ALLOCATIONS);
        break;
      }
      case PRICES: {
        qb = SupportSQLiteQueryBuilder.builder(VIEW_PRIORITIZED_PRICES);
        String commodity = uri.getQueryParameter(KEY_COMMODITY);
        if (commodity != null) {
          selection = KEY_CURRENCY + " = ? AND " + KEY_COMMODITY + "= ?";
          selectionArgs = new String[]{getHomeCurrency(), commodity};
          extras = oldestTransactionForCurrency(db, commodity);
        }
        break;
      }
      // This uses a separate Uri so that notify can be called on it and triggers reload with new home currency
      case DYNAMIC_CURRENCIES: {
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNTS);
        projection = new String[] { "distinct " + KEY_CURRENCY };
        selection = getDynamicCurrenciesSelection();
        selectionArgs = new String[] { getHomeCurrency() };
        break;
      }
      case ACCOUNT_TYPES: {
        projection = new String[]{"*", "(SELECT count(*) FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_TYPE + " = " + TABLE_ACCOUNT_TYPES + "." + KEY_ROWID + ") AS " + KEY_COUNT};
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNT_TYPES);
        break;
      }
      case ACCOUNT_TYPE_ID: {
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNT_TYPES);
        additionalWhere.append(KEY_ROWID + "=").append(uri.getPathSegments().get(1));
        break;
      }
      case ACCOUNT_FLAGS: {
        projection = new String[]{"*", "(SELECT count(*) FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_FLAG + " = " + TABLE_ACCOUNT_FLAGS + "." + KEY_ROWID + ") AS " + KEY_COUNT};
        qb = SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNT_FLAGS);
        break;
      }
      default:
        throw unknownUri(uri);
    }

    c = measureAndLogQuery(qb, uri, db, projection, computeWhere(selection, additionalWhere), selectionArgs, groupBy, having, sortOrder, limit, cte);

    c = wrapWithResultCompat(c, extras);

    final String withPlanInfo = uri.getQueryParameter(QUERY_PARAMETER_WITH_PLAN_INFO);
    if (uriMatch == TEMPLATES && withPlanInfo != null) {
      c = new PlanInfoCursorWrapper(getWrappedContext(), c, sortOrder == null, withPlanInfo.equals("2") || CALENDAR.hasPermission(getContext()));
    }
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  @Override
  public String getType(@NonNull Uri uri) {
    return null;
  }

  private IllegalArgumentException unknownUri(@NonNull Uri uri) {
    return new IllegalArgumentException("Unknown URL " + uri);
  }

  @Override
  public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
    log("INSERT Uri: %s, values: %s", uri, values);
    SupportSQLiteDatabase db = getHelper().getWritableDatabase();
    long id;
    Uri newUri;
    int uriMatch = URI_MATCHER.match(uri);
    maybeSetDirty(uriMatch);
    switch (uriMatch) {
      case TRANSACTIONS, UNCOMMITTED -> {
        Long equivalentAmount = values.getAsLong(KEY_EQUIVALENT_AMOUNT);
        values.remove(KEY_EQUIVALENT_AMOUNT);
        id = MoreDbUtilsKt.insert(db, TABLE_TRANSACTIONS, values);
        newUri = ContentUris.withAppendedId(TRANSACTIONS_URI, id);
        if (equivalentAmount != null) {
          insertEquivalentAmount(db, id, equivalentAmount);
        }
      }
      case ACCOUNTS -> {
        Preconditions.checkArgument(!values.containsKey(KEY_GROUPING));
        id = MoreDbUtilsKt.insert(db, TABLE_ACCOUNTS, values);
        newUri = ContentUris.withAppendedId(ACCOUNTS_URI, id);
      }
      case METHODS -> {
        id = MoreDbUtilsKt.insert(db, TABLE_METHODS, values);
        newUri = ContentUris.withAppendedId(METHODS_URI, id);
      }
      case ACCOUNTTYPES_METHODS -> {
        id = MoreDbUtilsKt.insert(db, TABLE_ACCOUNTTYES_METHODS, values);
        //we are not interested in accessing individual entries in this table, but have to return a uri
        newUri = ContentUris.withAppendedId(ACCOUNTTYPES_METHODS_URI, id);
      }
      case TEMPLATES -> {
        id = MoreDbUtilsKt.insert(db, TABLE_TEMPLATES, values);
        newUri = ContentUris.withAppendedId(TEMPLATES_URI, id);
      }
      case PAYEES -> {
        id = MoreDbUtilsKt.insert(db, TABLE_PAYEES, values);
        newUri = ContentUris.withAppendedId(PAYEES_URI, id);
      }
      case PLANINSTANCE_TRANSACTION_STATUS -> {
        long templateId = values.getAsLong(KEY_TEMPLATEID);
        long instanceId = values.getAsLong(KEY_INSTANCEID);
        db.insert(TABLE_PLAN_INSTANCE_STATUS, CONFLICT_REPLACE, values);
        Uri changeUri = Uri.parse(PLAN_INSTANCE_STATUS_URI + "/" + templateId + "/" + instanceId);
        notifyChange(changeUri, false);
        return changeUri;
      }
      case EVENT_CACHE -> {
        id = MoreDbUtilsKt.insert(db, TABLE_EVENT_CACHE, values);
        newUri = ContentUris.withAppendedId(EVENT_CACHE_URI, id);
      }
      case ACCOUNT_EXCHANGE_RATE -> {
        values.put(KEY_ACCOUNTID, uri.getPathSegments().get(1));
        values.put(KEY_CURRENCY_SELF, uri.getPathSegments().get(2));
        values.put(KEY_CURRENCY_OTHER, uri.getPathSegments().get(3));
        id = db.insert(TABLE_ACCOUNT_EXCHANGE_RATES, CONFLICT_REPLACE, values);
        newUri = uri;
      }
      case DUAL -> {
        if ("1".equals(uri.getQueryParameter(QUERY_PARAMETER_SYNC_BEGIN))) {
          id = pauseChangeTrigger(db);
          newUri = uri;
        } else {
          throw unknownUri(uri);
        }
      }
      case SETTINGS -> {
        id = db.insert(TABLE_SETTINGS, CONFLICT_REPLACE, values);
        newUri = ContentUris.withAppendedId(SETTINGS_URI, id);
      }
      case BUDGETS -> {
        Long budget = values.getAsLong(KEY_BUDGET);
        if (budget != null) {
          values.remove(KEY_BUDGET);
        }
        id = MoreDbUtilsKt.insert(db, TABLE_BUDGETS, values);
        if (budget != null) {
          ContentValues budgetInitialAmount = new ContentValues(2);
          budgetInitialAmount.put(KEY_BUDGETID, id);
          budgetInitialAmount.put(KEY_BUDGET, budget);
          budgetInitialAmount.put(KEY_CATID, 0);
          MoreDbUtilsKt.insert(db, TABLE_BUDGET_ALLOCATIONS, budgetInitialAmount);
        }
        newUri = BaseTransactionProvider.Companion.budgetUri(id);
      }
      case CURRENCIES -> {
        try {
          id = MoreDbUtilsKt.insert(db, TABLE_CURRENCIES, values);
        } catch (SQLiteConstraintException e) {
          return null;
        }
        newUri = ContentUris.withAppendedId(CURRENCIES_URI, id);
      }
      case TAGS -> {
        id = MoreDbUtilsKt.insert(db, TABLE_TAGS, values);
        newUri = ContentUris.withAppendedId(TAGS_URI, id);
      }
      case TRANSACTIONS_TAGS -> {
        if (callerIsNotSyncAdapter(uri)) throw new IllegalArgumentException("Can only be called from sync adapter");
        db.insert(TABLE_TRANSACTIONS_TAGS, CONFLICT_IGNORE, values);
        //the table does not have primary ids, we return the base uri
        notifyChange(uri, callerIsNotSyncAdapter(uri));
        return TRANSACTIONS_TAGS_URI;
      }
      case TEMPLATES_TAGS -> {
        db.insert(TABLE_TEMPLATES_TAGS, CONFLICT_IGNORE, values);
        //the table does not have primary ids, we return the base uri
        notifyChange(uri, false);
        return TEMPLATES_TAGS_URI;
      }
      case ACCOUNTS_TAGS -> {
        db.insert(TABLE_ACCOUNTS_TAGS, CONFLICT_IGNORE, values);
        //the table does not have primary ids, we return the base uri
        notifyChange(uri, false);
        return ACCOUNTS_TAGS_URI;
      }
      case DEBTS -> {
        id = MoreDbUtilsKt.insert(db, TABLE_DEBTS, values);
        newUri = ContentUris.withAppendedId(DEBTS_URI, id);
      }
      case BANKS -> {
        id = MoreDbUtilsKt.insert(db, TABLE_BANKS, values);
        newUri = ContentUris.withAppendedId(BANKS_URI, id);
      }
      // Currently not needed, until we implement Custom attributes
/*      case ATTRIBUTES -> {
        insertAttribute(db, values);
        return ATTRIBUTES_URI;
      }*/
      case TRANSACTION_ATTRIBUTES ->  {
        insertTransactionAttribute(db, values);
        return TRANSACTIONS_ATTRIBUTES_URI;
      }
      case ACCOUNT_ATTRIBUTES ->  {
        insertAccountAttribute(db, values);
        return ACCOUNTS_ATTRIBUTES_URI;
      }
      case ATTACHMENTS ->  {
        id = requireAttachment(db, values.getAsString(KEY_URI), values.getAsString(KEY_UUID));
        newUri = ContentUris.withAppendedId(ATTACHMENTS_URI, id);
      }
      case TRANSACTION_ATTACHMENTS -> {
        String uuid = values.getAsString(KEY_UUID);
        if (uuid != null) {
          Long attachmentByUuid = findAttachmentByUuid(db, uuid);
          if (attachmentByUuid == null) return null;
          values.remove(KEY_UUID);
          id = attachmentByUuid;
        } else {
          id = requireAttachment(db, values.getAsString(KEY_URI), null);
          values.remove(KEY_URI);
        }
        values.put(KEY_ATTACHMENT_ID, id);
        db.insert(TABLE_TRANSACTION_ATTACHMENTS, CONFLICT_IGNORE, values);
        newUri = ContentUris.withAppendedId(ATTACHMENTS_URI, id);
      }
      case TRANSACTION_TRANSFORM_TO_TRANSFER -> {
        id = MoreDbUtilsKt.transformToTransfer(db, uri, prefHandler.getDefaultTransferCategory());
        newUri = ContentUris.withAppendedId(TRANSACTIONS_URI, id);
      }
      case BUDGET_ALLOCATIONS -> {
        MoreDbUtilsKt.insert(db, TABLE_BUDGET_ALLOCATIONS, Objects.requireNonNull(values));
        return BUDGET_ALLOCATIONS_URI;
      }
      case PRICES -> {
        id = db.insert(TABLE_PRICES, CONFLICT_REPLACE, Objects.requireNonNull(values));
        newUri = ContentUris.withAppendedId(PRICES_URI, id);
      }
      case ACCOUNT_TYPES -> {
        id = MoreDbUtilsKt.insert(db, TABLE_ACCOUNT_TYPES, Objects.requireNonNull(values));
        newUri = ContentUris.withAppendedId(ACCOUNT_TYPES_URI, id);
      }
      case ACCOUNT_FLAGS -> {
        id = MoreDbUtilsKt.insert(db, TABLE_ACCOUNT_FLAGS, Objects.requireNonNull(values));
        newUri = ContentUris.withAppendedId(ACCOUNT_FLAGS_URI, id);
      }
      default -> throw unknownUri(uri);
    }
    notifyChange(uri, uriMatch == TRANSACTIONS && callerIsNotSyncAdapter(uri));
    //the accounts cursor contains aggregates about transactions
    //we need to notify it when transactions change
    if (uriMatch == TRANSACTIONS) {
      notifyChange(ACCOUNTS_URI, false);
      notifyChange(DEBTS_URI, false);
      //notifyChange(UNCOMMITTED_URI, false);
    } else if (uriMatch == ACCOUNTS) {
      notifyAccountChange();
      notifyChange(BANKS_URI, false);
    } else if (uriMatch == TEMPLATES) {
      notifyChange(TEMPLATES_UNCOMMITTED_URI, false);
    } else if (uriMatch == DEBTS) {
      notifyChange(PAYEES_URI, false);
    } else if (uriMatch == UNCOMMITTED) {
      notifyChange(DEBTS_URI, false);
    } else if (uriMatch == TRANSACTION_ATTACHMENTS) {
      notifyChange(TRANSACTIONS_URI, false);
    } else if (uriMatch == TRANSACTION_TRANSFORM_TO_TRANSFER) {
      notifyChange(TRANSACTIONS_URI, false);
      notifyChange(ACCOUNTS_URI, false);
      notifyChange(DEBTS_URI, false);
    } else if (uriMatch == PRICES) {
      notifyChange(ACCOUNTS_URI, false);
    }
    return id > 0 ? newUri : null;
  }

  @Override
  public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
    log("Delete for URL: %s", uri);
    SupportSQLiteDatabase db = getHelper().getWritableDatabase();
    int count;
    String segment;
    int uriMatch = URI_MATCHER.match(uri);
    maybeSetDirty(uriMatch);
    switch (uriMatch) {
      case TRANSACTIONS, UNCOMMITTED -> count = db.delete(TABLE_TRANSACTIONS, where, whereArgs);
      case TRANSACTION_ID -> {
        //maybe TODO ?: where and whereArgs are ignored
        segment = uri.getPathSegments().get(1);
        //when we are deleting a transfer whose peer is part of a split, we cannot delete the peer,
        //because the split would be left in an invalid state, hence we transform the peer to a normal split part
        //first we find out the account label
        db.beginTransaction();
        try {
          ContentValues args = new ContentValues();
          args.putNull(KEY_TRANSFER_ACCOUNT);
          args.putNull(KEY_TRANSFER_PEER);
          MoreDbUtilsKt.update(db, TABLE_TRANSACTIONS,
                  args,
                  KEY_TRANSFER_PEER + " = ? AND " + KEY_PARENTID + " IS NOT null",
                  new String[]{segment});
          //we delete the transaction, its children and its transfer peer, and transfer peers of its children
          if (uri.getQueryParameter(QUERY_PARAMETER_MARK_VOID) == null) {
            //we delete the parent separately, so that the changes trigger can correctly record the parent uuid
            count = db.delete(TABLE_TRANSACTIONS, WHERE_DEPENDENT, new String[]{segment, segment});
            count += db.delete(TABLE_TRANSACTIONS, WHERE_SELF_OR_PEER, new String[]{segment, segment});
          } else {
            ContentValues v = new ContentValues();
            v.put(KEY_CR_STATUS, CrStatus.VOID.name());
            count = MoreDbUtilsKt.update(db, TABLE_TRANSACTIONS, v, WHERE_SELF_OR_RELATED, new String[]{segment, segment, segment});
          }
          db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }
      }
      case TEMPLATES -> count = db.delete(TABLE_TEMPLATES, where, whereArgs);
      case TEMPLATE_ID -> count = db.delete(TABLE_TEMPLATES,
              KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      case ACCOUNTTYPES_METHODS -> count = db.delete(TABLE_ACCOUNTTYES_METHODS, where, whereArgs);
      case ACCOUNTS -> count = db.delete(TABLE_ACCOUNTS, where, whereArgs);
      case ACCOUNT_ID -> count = db.delete(TABLE_ACCOUNTS,
              KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);

      //update aggregate cursor
      //getContext().getContentResolver().notifyChange(AGGREGATES_URI, null);
      case CATEGORIES ->
              count = db.delete(TABLE_CATEGORIES, KEY_ROWID + " != " + SPLIT_CATID + prefixAnd(where),
                      whereArgs);
      case CATEGORY_ID -> {
        String lastPathSegment = uri.getLastPathSegment();
        if (Long.parseLong(lastPathSegment) == SPLIT_CATID)
          throw new IllegalArgumentException("split category can not be deleted");
        count = db.delete(TABLE_CATEGORIES,
                KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      }
      case PAYEE_ID -> count = db.delete(TABLE_PAYEES,
              KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      case METHOD_ID -> count = db.delete(TABLE_METHODS,
              KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      case PLANINSTANCE_TRANSACTION_STATUS ->
              count = db.delete(TABLE_PLAN_INSTANCE_STATUS, where, whereArgs);
      case PLANINSTANCE_STATUS_SINGLE -> {
        count = db.delete(TABLE_PLAN_INSTANCE_STATUS,
                String.format(Locale.ROOT, "%s = ? AND %s = ?", KEY_TEMPLATEID, KEY_INSTANCEID),
                new String[]{uri.getPathSegments().get(1), uri.getPathSegments().get(2)});
        notifyChange(uri, false);
        return count;
      }
      case EVENT_CACHE -> count = db.delete(TABLE_EVENT_CACHE, where, whereArgs);
      case STALE_IMAGES_ID -> {
        //will fail if attachment is not stale
        segment = uri.getPathSegments().get(1);
        count = db.delete(TABLE_ATTACHMENTS, KEY_ROWID + " = ?", new String[] { segment});
      }
      case STALE_IMAGES -> count = db.delete(TABLE_ATTACHMENTS, Companion.getSTALE_ATTACHMENT_SELECTION(), null);

      case CHANGES -> count = db.delete(TABLE_CHANGES, where, whereArgs);
      case SETTINGS -> count = db.delete(TABLE_SETTINGS, where, whereArgs);
      case BUDGET_ID ->
              count = db.delete(TABLE_BUDGETS, KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      case PAYEES -> count = db.delete(TABLE_PAYEES, where, whereArgs);
      case TAG_ID -> count = db.delete(TABLE_TAGS,
              KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      case DUAL -> {
        if ("1".equals(uri.getQueryParameter(QUERY_PARAMETER_SYNC_END))) {
          count = resumeChangeTrigger(db);
        } else {
          throw unknownUri(uri);
        }
      }
      case CURRENCIES_CODE -> {
        String currency = uri.getLastPathSegment();
        if (Utils.isKnownCurrency(currency)) {
          throw new IllegalArgumentException("Can only delete custom currencies");
        }
        try {
          count = db.delete(TABLE_CURRENCIES, String.format("%s = '%s'%s", KEY_CODE,
                  currency, prefixAnd(where)), whereArgs);
        } catch (SQLiteConstraintException e) {
          return 0;
        }
      }
      case TRANSACTIONS_TAGS -> {
        if (callerIsNotSyncAdapter(uri)) throw new IllegalArgumentException("Can only be called from sync adapter");
        count = db.delete(TABLE_TRANSACTIONS_TAGS, where, whereArgs);
      }
      case TEMPLATES_TAGS -> {
        count = db.delete(TABLE_TEMPLATES_TAGS, where, whereArgs);
      }
      case ACCOUNTS_TAGS -> {
        count = db.delete(TABLE_ACCOUNTS_TAGS, where, whereArgs);
      }
      case DEBT_ID -> {
        count = db.delete(TABLE_DEBTS,
                KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      }
      case BANK_ID -> {
        count = db.delete(TABLE_BANKS,
                KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      }
      case TRANSACTION_ID_ATTACHMENT_ID -> {
        String transactionId = uri.getPathSegments().get(2);
        String attachmentId = uri.getPathSegments().get(3);
        count = db.delete(TABLE_TRANSACTION_ATTACHMENTS,
                KEY_TRANSACTIONID + " = ? AND " + KEY_ATTACHMENT_ID + " = ?",
                new String[] { transactionId, attachmentId }
        );
        deleteAttachment(db, Long.parseLong(attachmentId), null);
      }
      case TAGS -> count = db.delete(TABLE_TAGS, where, whereArgs);
      case BUDGET_ALLOCATIONS -> count = db.delete(TABLE_BUDGET_ALLOCATIONS, where, whereArgs);
      case PRICES ->  count = db.delete(TABLE_PRICES, where, whereArgs);
      case ACCOUNT_TYPE_ID -> count = db.delete(TABLE_ACCOUNT_TYPES, KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      case ACCOUNT_FLAG_ID -> count = db.delete(TABLE_ACCOUNT_FLAGS, KEY_ROWID + " = " + uri.getLastPathSegment() + prefixAnd(where), whereArgs);
      default -> throw unknownUri(uri);
    }
    if (uriMatch == TRANSACTIONS || (uriMatch == TRANSACTION_ID && callerIsNotInBulkOperation(uri))) {
      notifyChange(TRANSACTIONS_URI, callerIsNotSyncAdapter(uri));
      notifyChange(ACCOUNTS_URI, false);
      notifyChange(DEBTS_URI, false);
      //notifyChange(UNCOMMITTED_URI, false);
    } else {
      if (uriMatch == ACCOUNTS || uriMatch == ACCOUNT_ID) {
        notifyAccountChange();
      }
      if (uriMatch == TEMPLATES || uriMatch == TEMPLATE_ID) {
        notifyChange(TEMPLATES_UNCOMMITTED_URI, false);
      }
      if (uriMatch == DEBT_ID) {
        notifyChange(PAYEES_URI, false);
      } else if (uriMatch == UNCOMMITTED) {
        notifyChange(DEBTS_URI, false);
      } else if (uriMatch == TRANSACTION_ID_ATTACHMENT_ID) {
        notifyChange(TRANSACTIONS_URI, false);
      } else if (uriMatch == BANK_ID) {
        notifyChange(ACCOUNTS_URI, false);
      } else if (uriMatch == PRICES) {
        notifyChange(ACCOUNTS_URI, false);
      }
      notifyChange(uri, uriMatch == TRANSACTION_ID);
    }
    return count;
  }

  private String prefixAnd(String where) {
    if (!TextUtils.isEmpty(where)) {
      return " AND (" + where + ')';
    } else {
      return "";
    }
  }

  @Override
  public int update(@NonNull Uri uri, ContentValues values, String where,
                    String[] whereArgs) {
    SupportSQLiteDatabase db = getHelper().getWritableDatabase();
    String segment; // contains rowId
    int count;
    int uriMatch = URI_MATCHER.match(uri);
    maybeSetDirty(uriMatch);
    log("UPDATE Uri: %s, values: %s", uri, values);
    final String lastPathSegment = uri.getLastPathSegment();
    switch (uriMatch) {
      case TRANSACTIONS, UNCOMMITTED ->
              count = MoreDbUtilsKt.update(db, TABLE_TRANSACTIONS, values, where, whereArgs);
      case TRANSACTION_ID, UNCOMMITTED_ID -> {
        Long equivalentAmount = values.getAsLong(KEY_EQUIVALENT_AMOUNT);
        values.remove(KEY_EQUIVALENT_AMOUNT);
        count = MoreDbUtilsKt.update(db, TABLE_TRANSACTIONS, values,
              KEY_ROWID + " = " + lastPathSegment + prefixAnd(where),
              whereArgs);
        if (equivalentAmount != null) {
          insertOrReplaceEquivalentAmount(db, Long.parseLong(lastPathSegment), equivalentAmount);
        }
    }
      case TRANSACTION_UNDELETE -> {
        segment = uri.getPathSegments().get(1);
        whereArgs = new String[]{segment, segment, segment};
        ContentValues v = new ContentValues();
        v.put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name());
        count = MoreDbUtilsKt.update(db, TABLE_TRANSACTIONS, v, "(" + WHERE_SELF_OR_RELATED + ") AND " + KEY_CR_STATUS + "='" + CrStatus.VOID.name() + "'", whereArgs);
      }
      case ACCOUNTS -> count = MoreDbUtilsKt.update(db, TABLE_ACCOUNTS, values, where, whereArgs);
      case ACCOUNT_ID -> count = MoreDbUtilsKt.update(db, TABLE_ACCOUNTS, values,
              KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      case TEMPLATES -> count = MoreDbUtilsKt.update(db, TABLE_TEMPLATES, values, where, whereArgs);
      case TEMPLATE_ID -> count = MoreDbUtilsKt.update(db, TABLE_TEMPLATES, values,
              KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      case PAYEE_ID -> {
        count = MoreDbUtilsKt.update(db, TABLE_PAYEES, values,
                KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
        notifyChange(TRANSACTIONS_URI, false);
      }
      case CATEGORIES ->
              throw new UnsupportedOperationException("Bulk update of categories is not supported");
      case CATEGORY_ID -> {
        //called from RepositoryCategory.moveCategory
        if (values.containsKey(KEY_PARENTID)) {
          Long parentId = values.getAsLong(KEY_PARENTID);
          if (parentId == null && !values.containsKey(KEY_COLOR)) {
            values.put(KEY_COLOR, suggestNewCategoryColor(db));
          }
          if (parentId != null) {
            values.putNull(KEY_COLOR);
          }
        }
        segment = lastPathSegment;
        count = MoreDbUtilsKt.update(db, TABLE_CATEGORIES, values, KEY_ROWID + " = " + segment + prefixAnd(where),
                whereArgs);
      }
      case METHOD_ID ->
              count = MoreDbUtilsKt.update(db, TABLE_METHODS, values, KEY_ROWID + " = " + lastPathSegment + prefixAnd(where),
                      whereArgs);
      case TEMPLATES_INCREASE_USAGE -> {
        db.execSQL("UPDATE " + TABLE_TEMPLATES + " SET " + KEY_USAGES + " = " + KEY_USAGES + " + 1, " +
                KEY_LAST_USED + " = strftime('%s', 'now') WHERE " + KEY_ROWID + " = " + uri.getPathSegments().get(1));
        count = 1;
      }
      //   when we move a transaction to a new target we apply two checks
      //1) we do not move a transfer to its own transfer_account
      //2) we check if the transactions method_id is also available in the target account, if not we set it to null
      case TRANSACTION_MOVE -> {
        segment = uri.getPathSegments().get(1);
        String target = uri.getPathSegments().get(3);
        db.execSQL("UPDATE " + TABLE_TRANSACTIONS +
                        " SET " +
                        KEY_ACCOUNTID + " = ?, " +
                        KEY_METHODID + " = " +
                        " CASE " +
                        " WHEN exists " +
                        " (SELECT 1 FROM " + TABLE_ACCOUNTTYES_METHODS +
                        " WHERE " + KEY_TYPE + " = " +
                        " (SELECT " + KEY_TYPE + " FROM " + TABLE_ACCOUNTS +
                        " WHERE " + DatabaseConstants.KEY_ROWID + " = ?) " +
                        " AND " + KEY_METHODID + " = " + TABLE_TRANSACTIONS + "." + KEY_METHODID + ")" +
                        " THEN " + KEY_METHODID +
                        " ELSE null " +
                        " END " +
                        " WHERE " + DatabaseConstants.KEY_ROWID + " = ? " +
                        " AND ( " + KEY_TRANSFER_ACCOUNT + " IS NULL OR " + KEY_TRANSFER_ACCOUNT + "  != ? )",
                new String[]{target, target, segment, target});
        count = 1;
      }
      case TRANSACTION_TOGGLE_CRSTATUS -> {
        db.execSQL("UPDATE " + TABLE_TRANSACTIONS +
                        " SET " + KEY_CR_STATUS +
                        " = CASE " + KEY_CR_STATUS +
                        " WHEN '" + "CLEARED" + "'" +
                        " THEN '" + "UNRECONCILED" + "'" +
                        " WHEN '" + "UNRECONCILED" + "'" +
                        " THEN '" + "CLEARED" + "'" +
                        " ELSE " + KEY_CR_STATUS +
                        " END" +
                        " WHERE " + DatabaseConstants.KEY_ROWID + " = ? ",
                new String[]{uri.getPathSegments().get(1)});
        count = 1;
      }
      case CURRENCIES_CHANGE_FRACTION_DIGITS -> {
        List<String> segments = uri.getPathSegments();
        count = updateFractionDigits(db, segments.get(2), Integer.parseInt(segments.get(3)));
      }
      case CHANGES -> {
        if (uri.getBooleanQueryParameter(QUERY_PARAMETER_INIT, false)) {
          initChangeLog(db, uri.getQueryParameter(KEY_ACCOUNTID));
          count = 1;
        } else {
          count = MoreDbUtilsKt.update(db, TABLE_CHANGES, values, where, whereArgs);
        }
      }
      case ACCOUNT_ID_GROUPING -> count = handleAccountProperty(db, uri, KEY_GROUPING);
      case ACCOUNT_ID_SORT -> count = handleAccountProperty(db, uri, KEY_SORT_BY, KEY_SORT_DIRECTION);
      case UNSPLIT -> count = unsplit(db, values, callerIsNotSyncAdapter(uri));
      case UNARCHIVE -> count = unarchive(db, values, callerIsNotSyncAdapter(uri));
      case BUDGET_ID -> count = MoreDbUtilsKt.update(db, TABLE_BUDGETS, values,
              KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      case BANK_ID -> count = MoreDbUtilsKt.update(db, TABLE_BANKS, values,
              KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      case BUDGET_CATEGORY -> count = budgetCategoryUpsert(db, uri, values);
      case BUDGET_ALLOCATIONS -> count = MoreDbUtilsKt.update(db, TABLE_BUDGET_ALLOCATIONS, values, where, whereArgs);
      case CURRENCIES_CODE -> {
        final String currency = lastPathSegment;
        count = MoreDbUtilsKt.update(db, TABLE_CURRENCIES, values, String.format("%s = '%s'%s", KEY_CODE,
                currency, prefixAnd(where)), whereArgs);
      }
      case TAG_ID -> count = MoreDbUtilsKt.update(db, TABLE_TAGS, values,
              KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      case TRANSACTION_LINK_TRANSFER -> count = MoreDbUtilsKt.linkTransfers(db, uri.getPathSegments().get(2), values.getAsString(KEY_UUID), callerIsNotSyncAdapter(uri));
      case TRANSACTION_UNLINK_TRANSFER -> count = MoreDbUtilsKt.unlinkTransfers(db, Objects.requireNonNull(lastPathSegment));
      case DEBTS -> count = MoreDbUtilsKt.update(db, TABLE_DEBTS, values, where, whereArgs);
      case DEBT_ID -> count = MoreDbUtilsKt.update(db, TABLE_DEBTS, values,
              KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      case PLANINSTANCE_TRANSACTION_STATUS -> count = MoreDbUtilsKt.update(db, TABLE_PLAN_INSTANCE_STATUS, values, where, whereArgs);

      //used during restore
      case ATTACHMENTS -> count = MoreDbUtilsKt.update(db, TABLE_ATTACHMENTS, values, where, whereArgs);
      case ACCOUNT_TYPE_ID -> count = MoreDbUtilsKt.update(db, TABLE_ACCOUNT_TYPES, values, KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      case ACCOUNT_FLAG_ID -> count = MoreDbUtilsKt.update(db, TABLE_ACCOUNT_FLAGS, values, KEY_ROWID + " = " + lastPathSegment + prefixAnd(where), whereArgs);
      default -> throw unknownUri(uri);
    }
    if (uriMatch == TRANSACTIONS || uriMatch == TRANSACTION_ID || uriMatch == ACCOUNTS || uriMatch == ACCOUNT_ID ||
        uriMatch == CURRENCIES_CHANGE_FRACTION_DIGITS || uriMatch == TRANSACTION_UNDELETE ||
        uriMatch == TRANSACTION_MOVE || uriMatch == TRANSACTION_TOGGLE_CRSTATUS ||
        uriMatch == TRANSACTION_LINK_TRANSFER  || uriMatch == TRANSACTION_UNLINK_TRANSFER || uriMatch == UNARCHIVE) {
      notifyChange(TRANSACTIONS_URI, callerIsNotSyncAdapter(uri));
      notifyChange(ACCOUNTS_URI, false);
      notifyChange(DEBTS_URI, false);
      //notifyChange(UNCOMMITTED_URI, false);
      notifyChange(CATEGORIES_URI, false);
    } else if (
      //we do not need to refresh cursors on the usage counters
        uriMatch != TEMPLATES_INCREASE_USAGE) {
      notifyChange(uri, false);
    }
    if (uriMatch == ACCOUNT_ID_GROUPING || uriMatch == ACCOUNT_ID_SORT) {
      notifyChange(ACCOUNTS_URI, false);
    }
    if (uriMatch == CURRENCIES_CHANGE_FRACTION_DIGITS || uriMatch == TEMPLATES_INCREASE_USAGE) {
      notifyChange(TEMPLATES_URI, false);
    }
    if (uriMatch == TEMPLATES || uriMatch == TEMPLATE_ID) {
      notifyChange(TEMPLATES_UNCOMMITTED_URI, false);
    }
    if (uriMatch == BUDGET_CATEGORY) {
      notifyChange(CATEGORIES_URI, false);
      notifyChange(BUDGETS_URI, false);
    }
    if (uriMatch == BUDGET_ALLOCATIONS) {
      notifyChange(CATEGORIES_URI, false);
      notifyChange(BUDGETS_URI, false);
    }
    if (uriMatch == ACCOUNTS || uriMatch == ACCOUNT_ID) {
      notifyAccountChange();
    }
    if (uriMatch == UNCOMMITTED_ID || uriMatch == UNCOMMITTED) {
      notifyChange(UNCOMMITTED_URI, false);
    }
    if (uriMatch == CATEGORY_ID || uriMatch == METHOD_ID) {
      notifyChange(TRANSACTIONS_URI, false);
    }
    if (uriMatch == ACCOUNT_TYPE_ID) {
      notifyChange(ACCOUNTS_URI, false);
    }
    if (uriMatch == ACCOUNT_FLAG_ID) {
      notifyChange(ACCOUNTS_URI, false);
    }
    return count;
  }

  /**
   * Apply the given set of {@link ContentProviderOperation}, executing inside
   * a transaction. All changes will be rolled back if
   * any single one fails.
   */
  @NonNull
  @Override
  public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
      throws OperationApplicationException {
    final SupportSQLiteDatabase db = getHelper().getWritableDatabase();
    final boolean alreadyInBulk = getBulkInProgress();
    if (!alreadyInBulk) {
      setBulkInProgress(true);
    }
    db.beginTransaction();
    try {
      final int numOperations = operations.size();
      final ContentProviderResult[] results = new ContentProviderResult[numOperations];
      for (int i = 0; i < numOperations; i++) {
        final ContentProviderOperation contentProviderOperation = operations.get(i);
        try {
          results[i] = contentProviderOperation.apply(this, results, i);
        } catch (Exception e) {
          Map<String, String> customData = new HashMap<>();
          customData.put("i", String.valueOf(i));
          customData.put("operation", contentProviderOperation.toString());
          CrashHandler.report(e, customData, TAG);
          throw e;
        }
      }
      db.setTransactionSuccessful();
      return results;
    } finally {
      db.endTransaction();
      if (!alreadyInBulk) {
        setBulkInProgress(false);
        notifyBulk();
      }
    }
  }

  @Nullable
  @Override
  public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
    log("Call method: %s, arg: %s, extras: %s", method, arg, extras);
    switch (method) {
      case METHOD_BULK_START -> {
        setBulkInProgress(true);
      }
      case METHOD_BULK_END -> {
        setBulkInProgress(false);
        notifyBulk();
      }
      case METHOD_SORT_ACCOUNTS -> {
        final SupportSQLiteDatabase db = getHelper().getWritableDatabase();
        if (extras != null) {
          long[] sortedIds = extras.getLongArray(KEY_SORT_KEY);
          if (sortedIds != null) {
            ContentValues values = new ContentValues(1);
            for (int i = 0; i < sortedIds.length; i++) {
              values.put(KEY_SORT_KEY, i);
              MoreDbUtilsKt.update(db, TABLE_ACCOUNTS, values, KEY_ROWID + " = ?", new String[]{String.valueOf(sortedIds[i])});
            }
            notifyChange(ACCOUNTS_URI, false);
          }
        }
      }
      case METHOD_SETUP_CATEGORIES -> {
        Bundle result = new Bundle(1);
        result.putSerializable(KEY_RESULT, MoreDbUtilsKt.setupDefaultCategories(getHelper().getWritableDatabase(), getWrappedContext().getResources()));
        notifyChange(CATEGORIES_URI, false);
        return result;
      }
      case METHOD_SETUP_CATEGORIES_DRY_RUN ->  {
        Bundle result = new Bundle(1);
        result.putParcelable(KEY_RESULT, MoreDbUtilsKt.getImportableCategories(getHelper().getWritableDatabase(), getWrappedContext().getResources()));
        return result;
      }
      case METHOD_CHECK_CORRUPTED_DATA_987 -> {
        return checkCorruptedData987();
      }
      case METHOD_DELETE_ATTACHMENTS ->  {
        Bundle result = new Bundle(1);
        result.putBoolean(KEY_RESULT, deleteAttachments(getHelper().getWritableDatabase(), extras.getLong(KEY_TRANSACTIONID), Arrays.asList(extras.getStringArray(KEY_URI_LIST))));
        return result;
      }
      case METHOD_SAVE_CATEGORY ->  {
        Bundle bundle = saveCategory(getHelper().getWritableDatabase(), Objects.requireNonNull(extras));
        notifyChange(CATEGORIES_URI, false);
        notifyChange(TRANSACTIONS_URI, false);
        return bundle;
      }
      case METHOD_ENSURE_CATEGORY -> {
        Bundle bundle = ensureCategory(getHelper().getWritableDatabase(), Objects.requireNonNull(extras));
        notifyChange(CATEGORIES_URI, false);
        notifyChange(TRANSACTIONS_URI, false);
        return bundle;
      }
      case METHOD_ENSURE_CATEGORY_TREE -> {
        Bundle bundle = ensureCategoryTree(getHelper().getWritableDatabase(), Objects.requireNonNull(extras));
        notifyChange(CATEGORIES_URI, false);
        notifyChange(TRANSACTIONS_URI, false);
        return bundle;
      }
      case METHOD_SAVE_TRANSACTION_TAGS ->  {
        saveTransactionTags(getHelper().getWritableDatabase(), Objects.requireNonNull(extras));
        notifyChange(TRANSACTIONS_URI, true);
      }
      case METHOD_MERGE_CATEGORIES -> {
        mergeCategories(getHelper().getWritableDatabase(), Objects.requireNonNull(extras));
        notifyChange(CATEGORIES_URI, false);
        notifyChange(TRANSACTIONS_URI, false);
      }
      case METHOD_ARCHIVE -> {
        Bundle result = new Bundle(1);
        result.putLong(KEY_TRANSACTIONID, archive(getHelper().getWritableDatabase(), Objects.requireNonNull(extras)));
        notifyChange(TRANSACTIONS_URI, true);
        notifyChange(ACCOUNTS_URI, false);
        return result;
      }
      case METHOD_CAN_BE_ARCHIVED -> {
         Bundle result = new Bundle(1);
         result.putParcelable(KEY_RESULT, canBeArchived(getHelper().getWritableDatabase(), Objects.requireNonNull(extras)));
         return result;
      }
      case METHOD_RECALCULATE_EQUIVALENT_AMOUNTS -> {
        Bundle result = new Bundle(1);
        result.putSerializable(KEY_RESULT, recalculateEquivalentAmounts(getHelper().getWritableDatabase(), Objects.requireNonNull(extras)));
        notifyChange(TRANSACTIONS_URI, true);
        notifyChange(ACCOUNTS_URI, false);
        return result;
      }
      case METHOD_RECALCULATE_EQUIVALENT_AMOUNTS_FOR_DATE -> {
        Bundle result = new Bundle(1);
        result.putInt(KEY_RESULT, recalculateEquivalentAmountsForDate(getHelper().getWritableDatabase(), Objects.requireNonNull(extras)));
        notifyChange(TRANSACTIONS_URI, true);
        notifyChange(ACCOUNTS_URI, false);
        return result;
      }
      case METHOD_FLAG_SORT -> {
        Bundle result = setFlagSort(getHelper().getWritableDatabase(), Objects.requireNonNull(extras));
        notifyChange(ACCOUNT_FLAGS_URI, false);
        return result;
      }
      case METHOD_TYPE_SORT ->  {
        Bundle result = setTypeSort(getHelper().getWritableDatabase(), Objects.requireNonNull(extras));
        notifyChange(ACCOUNT_TYPES_URI, false);
        notifyChange(ACCOUNTS_URI, false);
        return result;
      }
      case METHOD_CLEANUP_UNUSED_PAYEES -> {
        cleanupUnusedPayees(getHelper().getWritableDatabase());
        notifyChange(PAYEES_URI, false);
        return null;
      }
    }
    return null;
  }

  static {
    URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    URI_MATCHER.addURI(AUTHORITY, "transactions", TRANSACTIONS);
    URI_MATCHER.addURI(AUTHORITY, "transactionsUncommitted/", UNCOMMITTED);
    URI_MATCHER.addURI(AUTHORITY, "transactions/" + URI_SEGMENT_GROUPS + "/*", TRANSACTIONS_GROUPS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/" + URI_SEGMENT_SUMS_FOR_ACCOUNTS, TRANSACTIONS_SUMS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#", TRANSACTION_ID);
    URI_MATCHER.addURI(AUTHORITY, "transactionsUncommitted/#", UNCOMMITTED_ID);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#/" + URI_SEGMENT_MOVE + "/#", TRANSACTION_MOVE);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#/" + URI_SEGMENT_TOGGLE_CRSTATUS, TRANSACTION_TOGGLE_CRSTATUS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#/" + URI_SEGMENT_UNDELETE, TRANSACTION_UNDELETE);
    //uses uuid in order to be usable from sync adapter
    URI_MATCHER.addURI(AUTHORITY, "transactions/" + URI_SEGMENT_UNSPLIT, UNSPLIT);
    URI_MATCHER.addURI(AUTHORITY, "categories", CATEGORIES);
    URI_MATCHER.addURI(AUTHORITY, "categories/#", CATEGORY_ID);
    URI_MATCHER.addURI(AUTHORITY, "accounts", ACCOUNTS);
    URI_MATCHER.addURI(AUTHORITY, "accountsbase", ACCOUNTS_BASE);
    URI_MATCHER.addURI(AUTHORITY, "accounts/#", ACCOUNT_ID);
    URI_MATCHER.addURI(AUTHORITY, URI_SEGMENT_GROUPING + "/*/*", ACCOUNT_ID_GROUPING);
    URI_MATCHER.addURI(AUTHORITY, URI_SEGMENT_SORT + "/*/*/*", ACCOUNT_ID_SORT);
    URI_MATCHER.addURI(AUTHORITY, "payees", PAYEES);
    URI_MATCHER.addURI(AUTHORITY, "payees/#", PAYEE_ID);
    URI_MATCHER.addURI(AUTHORITY, "methods", METHODS);
    URI_MATCHER.addURI(AUTHORITY, "methods/#", METHOD_ID);
    //methods/typeFilter/{TransactionType}/{AccountType}
    //TransactionType: 1 Income, -1 Expense
    //AccountType: CASH BANK CCARD ASSET LIABILITY
    URI_MATCHER.addURI(AUTHORITY, "methods/" + URI_SEGMENT_TYPE_FILTER + "/*", METHODS_FILTERED);
    URI_MATCHER.addURI(AUTHORITY, "accounttypes_methods", ACCOUNTTYPES_METHODS);
    URI_MATCHER.addURI(AUTHORITY, "templates", TEMPLATES);
    URI_MATCHER.addURI(AUTHORITY, "templatesUncommitted", TEMPLATES_UNCOMMITTED);
    URI_MATCHER.addURI(AUTHORITY, "templates/#", TEMPLATE_ID);
    URI_MATCHER.addURI(AUTHORITY, "templates/#/" + URI_SEGMENT_INCREASE_USAGE, TEMPLATES_INCREASE_USAGE);
    URI_MATCHER.addURI(AUTHORITY, "sqlite_sequence/*", SQLITE_SEQUENCE_TABLE);
    URI_MATCHER.addURI(AUTHORITY, "planinstance_transaction", PLANINSTANCE_TRANSACTION_STATUS);
    URI_MATCHER.addURI(AUTHORITY, "planinstance_transaction/#/#", PLANINSTANCE_STATUS_SINGLE);
    URI_MATCHER.addURI(AUTHORITY, "currencies", CURRENCIES);
    URI_MATCHER.addURI(AUTHORITY, "currencies/" + URI_SEGMENT_CHANGE_FRACTION_DIGITS + "/*/#", CURRENCIES_CHANGE_FRACTION_DIGITS);
    URI_MATCHER.addURI(AUTHORITY, "accounts/aggregates/*", AGGREGATE_ID);
    URI_MATCHER.addURI(AUTHORITY, "methods_transactions", MAPPED_METHODS);
    URI_MATCHER.addURI(AUTHORITY, "dual", DUAL);
    URI_MATCHER.addURI(AUTHORITY, "eventcache", EVENT_CACHE);
    URI_MATCHER.addURI(AUTHORITY, "debug_schema", DEBUG_SCHEMA);
    URI_MATCHER.addURI(AUTHORITY, "stale_images", STALE_IMAGES);
    URI_MATCHER.addURI(AUTHORITY, "stale_images/#", STALE_IMAGES_ID);
    URI_MATCHER.addURI(AUTHORITY, "transfer_account_transactions", MAPPED_TRANSFER_ACCOUNTS);
    URI_MATCHER.addURI(AUTHORITY, "changes", CHANGES);
    URI_MATCHER.addURI(AUTHORITY, "settings", SETTINGS);
    URI_MATCHER.addURI(AUTHORITY, "autofill/#", AUTOFILL);
    URI_MATCHER.addURI(AUTHORITY, "account_exchangerates/#/*/*", ACCOUNT_EXCHANGE_RATE);
    URI_MATCHER.addURI(AUTHORITY, "budgets", BUDGETS);
    URI_MATCHER.addURI(AUTHORITY, "budgets/#", BUDGET_ID);
    URI_MATCHER.addURI(AUTHORITY, "budgets/#/#", BUDGET_CATEGORY);
    URI_MATCHER.addURI(AUTHORITY, "budgets/#/" + URI_SEGMENT_BUDGET_ALLOCATIONS, BUDGET_FOR_PERIOD);
    URI_MATCHER.addURI(AUTHORITY, "currencies/*", CURRENCIES_CODE);
    URI_MATCHER.addURI(AUTHORITY, "accountsMinimal", ACCOUNTS_MINIMAL);
    URI_MATCHER.addURI(AUTHORITY, "tags", TAGS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/tags", TRANSACTIONS_TAGS);
    URI_MATCHER.addURI(AUTHORITY, "tags/#", TAG_ID);
    URI_MATCHER.addURI(AUTHORITY, "templates/tags", TEMPLATES_TAGS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/" + URI_SEGMENT_LINK_TRANSFER + "/*", TRANSACTION_LINK_TRANSFER);
    URI_MATCHER.addURI(AUTHORITY, "transactions/" + URI_SEGMENT_UNLINK_TRANSFER + "/*", TRANSACTION_UNLINK_TRANSFER);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#/" + URI_SEGMENT_TRANSFORM_TO_TRANSFER + "/#", TRANSACTION_TRANSFORM_TO_TRANSFER);
    URI_MATCHER.addURI(AUTHORITY, "accounts/tags", ACCOUNTS_TAGS);
    URI_MATCHER.addURI(AUTHORITY, "debts", DEBTS);
    URI_MATCHER.addURI(AUTHORITY, "debts/#", DEBT_ID);
    URI_MATCHER.addURI(AUTHORITY, "budgets/" + URI_SEGMENT_BUDGET_ALLOCATIONS, BUDGET_ALLOCATIONS);
    URI_MATCHER.addURI(AUTHORITY, "budgets/" + URI_SEGMENT_DEFAULT_BUDGET_ALLOCATIONS + "/*/*", ACCOUNT_DEFAULT_BUDGET_ALLOCATIONS);
    URI_MATCHER.addURI(AUTHORITY, "banks", BANKS);
    URI_MATCHER.addURI(AUTHORITY, "banks/#", BANK_ID);
    URI_MATCHER.addURI(AUTHORITY, "attributes", ATTRIBUTES);
    URI_MATCHER.addURI(AUTHORITY, "transactions/attributes", TRANSACTION_ATTRIBUTES);
    URI_MATCHER.addURI(AUTHORITY, "accounts/attributes", ACCOUNT_ATTRIBUTES);
    URI_MATCHER.addURI(AUTHORITY, "transactions/attachments", TRANSACTION_ATTACHMENTS);
    URI_MATCHER.addURI(AUTHORITY, "attachments", ATTACHMENTS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/attachments/#/#", TRANSACTION_ID_ATTACHMENT_ID);
    URI_MATCHER.addURI(AUTHORITY, "transactions/" + URI_SEGMENT_UNARCHIVE, UNARCHIVE);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#/" + URI_SEGMENT_SUMS_FOR_ARCHIVE, ARCHIVE_SUMS);
    URI_MATCHER.addURI(AUTHORITY, "prices", PRICES);
    URI_MATCHER.addURI(AUTHORITY, "dynamicCurrencies", DYNAMIC_CURRENCIES);
    URI_MATCHER.addURI(AUTHORITY, "accountTypes", ACCOUNT_TYPES);
    URI_MATCHER.addURI(AUTHORITY, "accountTypes/#", ACCOUNT_TYPE_ID);
    URI_MATCHER.addURI(AUTHORITY, "accountFlags", ACCOUNT_FLAGS);
    URI_MATCHER.addURI(AUTHORITY, "accountFlags/#", ACCOUNT_FLAG_ID);
  }

  /**
   * A test package can call this to get a handle to the database underlying TransactionProvider,
   * so it can insert test data into the database. The test case class is responsible for
   * instantiating the provider in a test context; ProviderTestCase2 does
   * this during the call to setUp()
   *
   * @return a handle to the database helper object for the provider's data.
   */
  @VisibleForTesting
  public SupportSQLiteOpenHelper getOpenHelperForTest() {
    return getHelper();
  }

  public static ContentProviderOperation resumeChangeTrigger() {
    return ContentProviderOperation.newDelete(
        DUAL_URI.buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_SYNC_END, "1").build())
        .build();
  }

  static int resumeChangeTrigger(SupportSQLiteDatabase db) {
    return db.delete(TABLE_SYNC_STATE, null, null);
  }

  public static ContentProviderOperation pauseChangeTrigger() {
    return ContentProviderOperation.newInsert(
        DUAL_URI.buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_SYNC_BEGIN, "1").build())
        .build();
  }

  static long pauseChangeTrigger(SupportSQLiteDatabase db) {
    ContentValues values = new ContentValues(1);
    values.put(KEY_STATUS, "1");
    return MoreDbUtilsKt.insert(db, TABLE_SYNC_STATE, values);
  }
}
