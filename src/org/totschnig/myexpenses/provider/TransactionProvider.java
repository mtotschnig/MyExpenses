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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.model.Account.Grouping;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

public class TransactionProvider extends ContentProvider {

  protected static TransactionDatabase mOpenHelper;
  public static final String AUTHORITY = "org.totschnig.myexpenses";
  public static final Uri ACCOUNTS_URI =
      Uri.parse("content://" + AUTHORITY + "/accounts");
  public static final Uri TRANSACTIONS_URI =
      Uri.parse("content://" + AUTHORITY + "/transactions");
  public static final Uri UNCOMMITTED_URI =
      Uri.parse("content://" + AUTHORITY + "/transactions/uncommitted");
  public static final Uri TEMPLATES_URI =
      Uri.parse("content://" + AUTHORITY + "/templates");
  public static final Uri CATEGORIES_URI =
      Uri.parse("content://" + AUTHORITY + "/categories");
  public static final Uri AGGREGATES_URI =
      Uri.parse("content://" + AUTHORITY + "/accounts/aggregates");
  public static final Uri PAYEES_URI =
      Uri.parse("content://" + AUTHORITY + "/payees");
  public static final Uri METHODS_URI =
      Uri.parse("content://" + AUTHORITY + "/methods");
  public static final Uri ACCOUNTTYPES_METHODS_URI =
      Uri.parse("content://" + AUTHORITY + "/accounttypes_methods");
  public static final Uri FEATURE_USED_URI =
      Uri.parse("content://" + AUTHORITY + "/feature_used");
  public static final Uri SQLITE_SEQUENCE_TRANSACTIONS_URI =
      Uri.parse("content://" + AUTHORITY + "/sqlite_sequence/" + TABLE_TRANSACTIONS);

  
  static final String TAG = "TransactionProvider";

  private static final UriMatcher URI_MATCHER;
  //Basic tables
  private static final int TRANSACTIONS = 1;
  private static final int TRANSACTION_ID = 2;
  private static final int CATEGORIES = 3;
  private static final int ACCOUNTS = 4;
  private static final int ACCOUNT_ID = 5;
  private static final int AGGREGATES = 6;
  private static final int PAYEES = 7;
  private static final int METHODS = 8;
  private static final int METHOD_ID = 9;
  private static final int ACCOUNTTYPES_METHODS = 10;
  private static final int TEMPLATES = 11;
  private static final int TEMPLATES_ID = 12;
  private static final int CATEGORY_ID = 13;
  private static final int CATEGORY_INCREASE_USAGE = 14;
  private static final int PAYEES_ID = 15;
  private static final int METHODS_FILTERED = 16;
  private static final int TEMPLATES_INCREASE_USAGE = 17;
  private static final int FEATURE_USED = 18;
  private static final int SQLITE_SEQUENCE_TABLE = 19;
  private static final int AGGREGATES_COUNT = 20;
  private static final int UNCOMMITTED = 21;
  private static final int TRANSACTIONS_GROUPS = 22;
  private static final int ACCOUNT_INCREASE_USAGE = 23;
  private static final int TRANSACTIONS_SUMS = 24;
  private static final int TRANSACTION_MOVE = 25;
  
  @Override
  public boolean onCreate() {
    mOpenHelper = new TransactionDatabase(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    if (MyApplication.debug)
      Log.d(TAG, "Query for URL: " + uri);
    String defaultOrderBy = null;
    String groupBy = null;
    String having = null;

    switch (URI_MATCHER.match(uri)) {
    case TRANSACTIONS:
      qb.setTables(VIEW_COMMITTED);
      defaultOrderBy = KEY_DATE + " DESC";
      if (projection == null)
        projection = Transaction.PROJECTION;
      break;
    case UNCOMMITTED:
      qb.setTables(VIEW_UNCOMMITTED);
      defaultOrderBy = KEY_DATE + " DESC";
      if (projection == null)
        projection = Transaction.PROJECTION;
      break;
    case TRANSACTION_ID:
      qb.setTables(VIEW_ALL);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case TRANSACTIONS_SUMS:
      qb.setTables(VIEW_COMMITTED);
      projection = new String[] {"amount>0 as type","abs(sum(amount)) as  sum"};
      groupBy = "type";
      qb.appendWhere(WHERE_TRANSACTION);
      qb.appendWhere(" AND " + KEY_ACCOUNTID + "=" + uri.getPathSegments().get(2));
      break;
    case TRANSACTIONS_GROUPS:
      Grouping group;
      try {
        group = Grouping.valueOf(uri.getPathSegments().get(2));
      } catch (IllegalArgumentException e) {
        group = Grouping.NONE;
      }
      String yearColumn = (group.equals(Grouping.WEEK) ? YEAR_OF_WEEK_START : YEAR) + " AS year";
      String secondColumnAlias = " AS second";
      if (group.equals(Grouping.NONE)) {
        qb.setTables(VIEW_COMMITTED);
        projection = new String[] {
            "1 AS year",
            "1"+secondColumnAlias,
            INCOME_SUM,
            EXPENSE_SUM,
            TRANSFER_SUM,
            MAPPED_CATEGORIES,
            "coalesce(sum(CASE WHEN " + WHERE_NOT_SPLIT + " THEN amount ELSE 0 END),0) AS sum_interim"};
      } else {
        String secondColumn="";
        String subGroupBy = "year,second";
        switch(group) {
        case DAY:
          secondColumn = DAY+secondColumnAlias;
          break;
        case WEEK:
          secondColumn = DAY+secondColumnAlias;
          break;
        case MONTH:
          secondColumn = DAY+secondColumnAlias;
          break;
        case YEAR:
          secondColumn = "1"+secondColumnAlias;
          subGroupBy = "year";
          break;
        }
        qb.setTables("(SELECT "
            + yearColumn + ","
            + secondColumn + ","
            + INCOME_SUM + ","
            + EXPENSE_SUM + ","
            + TRANSFER_SUM + ","
            + MAPPED_CATEGORIES
            + " FROM " + VIEW_COMMITTED
            + " WHERE " + selection
            + " GROUP BY " + subGroupBy + ") AS t");
        projection = new String[] {"year","second","sum_income","sum_expense","sum_transfer","mapped_categories",
            "(SELECT sum(amount) FROM "
                + VIEW_COMMITTED
                + " WHERE " + selection + " AND " + WHERE_NOT_SPLIT
                + " AND (CAST(strftime('%Y',date) AS integer) < year OR "
                + "(CAST(strftime('%Y',date) AS integer) = year AND CAST(strftime('%j',date) AS integer) <= second)))" +
                " AS sum_interim"
            };
        //the selection is used twice, once in the table subquery, once in the column subquery,
        //we do not need it any more in the query builder, but we need the selection arguments twice
        selection = null;
        List<String> newArgs = new ArrayList<String>(selectionArgs.length * 2);
        Collections.addAll(newArgs, selectionArgs);
        Collections.addAll(newArgs, selectionArgs);
        selectionArgs = newArgs.toArray(new String[newArgs.size()]);
      }
      break;
    case CATEGORIES:
      qb.setTables(TABLE_CATEGORIES);
      qb.appendWhere(KEY_ROWID+ " != " + SPLIT_CATID);
      if (projection == null)
        projection = Category.PROJECTION;
      //qb.appendWhere("parent_id=" + uri.getPathSegments().get(1));
      defaultOrderBy = (MyApplication.getInstance().getSettings()
          .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true) ?
              KEY_USAGES + " DESC, " : "")
         + KEY_LABEL;
      break;
    case CATEGORY_ID:
      qb.setTables(TABLE_CATEGORIES);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case ACCOUNTS:
      qb.setTables(TABLE_ACCOUNTS);
      if (projection == null)
        projection = Account.PROJECTION;
      defaultOrderBy = (MyApplication.getInstance().getSettings()
          .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true) ?
              KEY_USAGES + " DESC, " : "")
         + KEY_LABEL;
      break;
    case ACCOUNT_ID:
      qb.setTables(TABLE_ACCOUNTS);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case AGGREGATES:
      //we calculate the aggregates by taking in account the split parts instead of the split transactions,
      //thus we can ignore split parts that are transfers
      qb.setTables("(select currency,opening_balance,"+
          "(SELECT coalesce(abs(sum(amount)),0) FROM "
              + VIEW_COMMITTED
              + " WHERE account_id = accounts._id AND " + WHERE_EXPENSE + ") as sum_expenses," +
          "(SELECT coalesce(abs(sum(amount)),0) FROM "
              + VIEW_COMMITTED
              + " WHERE account_id = accounts._id AND " + WHERE_INCOME + ") as sum_income," +
          "opening_balance + (SELECT coalesce(sum(amount),0) FROM "
              + VIEW_COMMITTED
              + " WHERE account_id = accounts._id and (cat_id is null OR cat_id != "
                  + SPLIT_CATID + ")) as current_balance " +
          "from " + TABLE_ACCOUNTS + ") as t");
      groupBy = "currency";
      having = "count(*) > 1";
      projection = new String[] {"1 as _id","currency",
          "sum(opening_balance) as opening_balance",
          "sum(sum_income) as sum_income",
          "sum(sum_expenses) as sum_expenses",
          "sum(current_balance) as current_balance"};
      break;
    case AGGREGATES_COUNT:
      qb.setTables(TABLE_ACCOUNTS);
      groupBy = "currency";
      having = "count(*) > 1";
      projection = new String[] {"count(*)"};
      break;
    case PAYEES:
      qb.setTables(TABLE_PAYEES);
      defaultOrderBy = "name";
      if (projection == null)
        projection = Payee.PROJECTION;
      break;
    case METHODS:
      qb.setTables(TABLE_METHODS);
      if (projection == null)
        projection = PaymentMethod.PROJECTION;
      break;
    case METHOD_ID:
      qb.setTables(TABLE_METHODS);
      if (projection == null)
        projection = PaymentMethod.PROJECTION;
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case METHODS_FILTERED:
      qb.setTables(TABLE_METHODS + " JOIN " + TABLE_ACCOUNTTYES_METHODS + " ON (" + KEY_ROWID + " = " + KEY_METHODID + ")");
      projection =  new String[] {KEY_ROWID,KEY_LABEL,KEY_IS_NUMBERED};
      String paymentType = uri.getPathSegments().get(2);
      if (paymentType.equals("1")) {
        selection = TABLE_METHODS + ".type > -1";
      } else if (paymentType.equals("-1")) {
        selection = TABLE_METHODS + ".type < 1";
      } else {
        throw new IllegalArgumentException("Unknown paymentType " + paymentType);
      }
      String accountType = uri.getPathSegments().get(3);
      try {
        Account.Type.valueOf(accountType);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Unknown accountType " + accountType);
      }
      selection += " and " + TABLE_ACCOUNTTYES_METHODS + ".type = ?";
      selectionArgs = new String[] {accountType};
      break;
    case ACCOUNTTYPES_METHODS:
      qb.setTables(TABLE_ACCOUNTTYES_METHODS);
      break;
    case TEMPLATES:
      qb.setTables(VIEW_TEMPLATES);
      defaultOrderBy = (MyApplication.getInstance().getSettings()
          .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true) ?
              KEY_USAGES + " DESC, " : "")
         + KEY_TITLE;
      if (projection == null)
        projection = Template.PROJECTION;
      break;
    case TEMPLATES_ID:
      qb.setTables(VIEW_TEMPLATES);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      if (projection == null)
        projection = Template.PROJECTION;
      break;
    case FEATURE_USED:
      qb.setTables(TABLE_FEATURE_USED);
      break;
    case SQLITE_SEQUENCE_TABLE:
      qb.setTables("SQLITE_SEQUENCE");
      projection = new String[] {"seq"};
      selection = "name = ?";
      selectionArgs = new String[] {uri.getPathSegments().get(1)};
      break;
    default:
      throw new IllegalArgumentException("Unknown URL " + uri);
    }
    String orderBy;
    if (TextUtils.isEmpty(sortOrder)) {
      orderBy = defaultOrderBy;
    } else {
      orderBy = sortOrder;
    }

    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
    if (MyApplication.debug) {
      String qs = qb.buildQuery(projection, selection, null, groupBy,
          null, orderBy, null);
      Log.d(TAG, "Query : " + qs);
    }

    Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
        having, orderBy);
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    long id = 0;
    String newUri;
    int uriMatch = URI_MATCHER.match(uri);
    switch (uriMatch) {
    case TRANSACTIONS:
      id = db.insertOrThrow(TABLE_TRANSACTIONS, null, values);
      newUri = TRANSACTIONS_URI + "/" + id;
      break;
    case ACCOUNTS:
      id = db.insertOrThrow(TABLE_ACCOUNTS, null, values);
      newUri = ACCOUNTS_URI + "/" + id;
      break;
    case METHODS:
      id = db.insertOrThrow(TABLE_METHODS, null, values);
      newUri = METHODS_URI + "/" + id;
      break;
    case ACCOUNTTYPES_METHODS:
      id = db.insertOrThrow(TABLE_ACCOUNTTYES_METHODS,null,values);
      //we are not interested in accessing individual entries in this table, but have to return a uri
      newUri = ACCOUNTTYPES_METHODS_URI + "/" + id;
      break;
    case TEMPLATES:
      id = db.insertOrThrow(TABLE_TEMPLATES, null, values);
      newUri = TEMPLATES_URI + "/" + id;
      break;
    case CATEGORIES:
      //for categories we can not rely on the unique constraint, since it does not work for parent_id is null
      Long parentId = values.getAsLong(KEY_PARENTID);
      String label = values.getAsString(KEY_LABEL);
      String selection;
      String[] selectionArgs;
      if (parentId == null) {
        selection = KEY_PARENTID + " is null";
        selectionArgs = new String[]{label};
      } else {
        selection = KEY_PARENTID + " = ?";
        selectionArgs = new String[]{String.valueOf(parentId),label};
      }
      selection += " and " + KEY_LABEL + " = ?";
      Cursor mCursor = db.query(TABLE_CATEGORIES, new String []{KEY_ROWID}, selection, selectionArgs, null, null, null);
      if (mCursor.getCount() != 0) {
        mCursor.close();
        throw new SQLiteConstraintException();
      }
      mCursor.close();
      id = db.insertOrThrow(TABLE_CATEGORIES, null, values);
      newUri = CATEGORIES_URI + "/" + id;
      break;
    case PAYEES:
      id = db.insertOrThrow(TABLE_PAYEES, null, values);
      newUri = PAYEES_URI + "/" + id;
      break;
    case FEATURE_USED:
      id = db.insertOrThrow(TABLE_FEATURE_USED, null, values);
      newUri = FEATURE_USED_URI + "/" + id;
      break;
    default:
      throw new IllegalArgumentException("Unknown URI: " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    //the accounts cursor contains aggregates about transactions
    //we need to notify it when transactions change
    if (uriMatch == TRANSACTIONS) {
      getContext().getContentResolver().notifyChange(ACCOUNTS_URI, null);
      getContext().getContentResolver().notifyChange(UNCOMMITTED_URI, null);
    }
    return id >0 ? Uri.parse(newUri) : null;
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    if (MyApplication.debug)
      Log.d(TAG, "Delete for URL: " + uri);
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    int count;
    String whereString;
    String segment;
    int uriMatch = URI_MATCHER.match(uri);
    switch (uriMatch) {
    case TRANSACTIONS:
      count = db.delete(TABLE_TRANSACTIONS, where, whereArgs);
      break;
    case TRANSACTION_ID:
      //maybe TODO ?: where and whereArgs are ignored
      segment = uri.getPathSegments().get(1);
      count = db.delete(TABLE_TRANSACTIONS,
          KEY_ROWID + " = ? OR " + KEY_PARENTID + " = ? OR " + KEY_TRANSFER_PEER + " = ? OR "
              + KEY_ROWID + " IN "
              + "(SELECT " + KEY_TRANSFER_PEER + " FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_PARENTID + "= ?)",
         new String[] {segment,segment,segment,segment});
      break;
    case TEMPLATES:
      count = db.delete(TABLE_TEMPLATES, where, whereArgs);
      break;
    case TEMPLATES_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.delete(TABLE_TEMPLATES, "_id=" + segment + whereString,
          whereArgs);
      break;
    case ACCOUNTTYPES_METHODS:
      count = db.delete(TABLE_ACCOUNTTYES_METHODS, where, whereArgs);
      break;
    case ACCOUNTS:
      count = db.delete(TABLE_ACCOUNTS, where, whereArgs);
      break;
    case ACCOUNT_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.delete(TABLE_ACCOUNTS, "_id=" + segment + whereString,
          whereArgs);
      //update aggregate cursor
      getContext().getContentResolver().notifyChange(AGGREGATES_URI, null);
      break;
    case CATEGORIES:
      count = db.delete(TABLE_CATEGORIES, where, whereArgs);
      break;
    case CATEGORY_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.delete(TABLE_CATEGORIES, "_id=" + segment + whereString,
          whereArgs);
      break;
    case PAYEES_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.delete(TABLE_PAYEES, "_id=" + segment + whereString,
          whereArgs);
      break;
    case METHOD_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.delete(TABLE_METHODS, "_id=" + segment + whereString,
          whereArgs);
      break;
    default:
      throw new IllegalArgumentException("Unknown URL " + uri);
    }
    if (uriMatch == TRANSACTIONS || uriMatch == TRANSACTION_ID) {
      getContext().getContentResolver().notifyChange(TRANSACTIONS_URI, null);
      getContext().getContentResolver().notifyChange(ACCOUNTS_URI, null);
      getContext().getContentResolver().notifyChange(UNCOMMITTED_URI, null);
    } else
      getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  @Override
  public int update(Uri uri, ContentValues values, String where,
      String[] whereArgs) {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    String segment; // contains rowId
    int count;
    String whereString;
    int uriMatch = URI_MATCHER.match(uri);
    switch (uriMatch) {
    case TRANSACTIONS:
      count = db.update(TABLE_TRANSACTIONS, values, where, whereArgs);
      break;
    case TRANSACTION_ID:
      segment = uri.getPathSegments().get(1); 
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.update(TABLE_TRANSACTIONS, values, "_id=" + segment + whereString,
          whereArgs);
      break;
    case ACCOUNTS:
      count = db.update(TABLE_ACCOUNTS, values, where, whereArgs);
      break;
    case ACCOUNT_ID:
      segment = uri.getPathSegments().get(1); 
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.update(TABLE_ACCOUNTS, values, "_id=" + segment + whereString,
          whereArgs);
      //update aggregate cursor
      getContext().getContentResolver().notifyChange(AGGREGATES_URI, null);
      break;
    case TEMPLATES_ID:
      segment = uri.getPathSegments().get(1); 
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.update(TABLE_TEMPLATES, values, "_id=" + segment + whereString,
            whereArgs);
      break;
    case PAYEES_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.update(TABLE_PAYEES, values, "_id=" + segment + whereString,
            whereArgs);
      getContext().getContentResolver().notifyChange(TRANSACTIONS_URI, null);
      break;
    case CATEGORIES:
      //TODO should not support bulk update of categories
      count = db.update(TABLE_CATEGORIES, values, where, whereArgs);
      break;
    case CATEGORY_ID:
      segment = uri.getPathSegments().get(1);
      //for categories we can not rely on the unique constraint, since it does not work for parent_id is null
      String label = values.getAsString(KEY_LABEL);
      String selection = "label = ? and parent_id is (select parent_id from categories where _id = ?)";
      String[] selectionArgs = new String[]{label,segment};
      Cursor mCursor = db.query(TABLE_CATEGORIES, new String []{KEY_ROWID}, selection, selectionArgs, null, null, null);
      if (mCursor.getCount() != 0) {
        mCursor.close();
        throw new SQLiteConstraintException();
      }
      mCursor.close();
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.update(TABLE_CATEGORIES, values, "_id=" + segment + whereString,
            whereArgs);
      break;
    case METHOD_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.update(TABLE_METHODS, values, "_id=" + segment + whereString,
          whereArgs);
      break;
    case CATEGORY_INCREASE_USAGE:
      segment = uri.getPathSegments().get(1);
      db.execSQL("update " + TABLE_CATEGORIES + " set usages = usages +1 WHERE _id IN (" + segment +
          " , (SELECT parent_id FROM categories WHERE _id = " + segment + "))");
      count = 1;
      break;
    case TEMPLATES_INCREASE_USAGE:
      segment = uri.getPathSegments().get(1);
      db.execSQL("update " + TABLE_TEMPLATES + " set usages = usages +1 WHERE _id = " + segment);
      count = 1;
      break;
    case ACCOUNT_INCREASE_USAGE:
      segment = uri.getPathSegments().get(1);
      db.execSQL("update " + TABLE_ACCOUNTS + " set usages = usages +1 WHERE _id = " + segment);
      count = 1;
      break;
    //   when we move a transaction to a new target we apply two checks
    //1) we do not move a transfer to its own transfer_account
    //2) we check if the transactions method_id is also available in the target account, if not we set it to null
    case TRANSACTION_MOVE:
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
                                    " WHERE " + KEY_ROWID + " = ?) " +
                                    " AND " + KEY_METHODID + " = " + TABLE_TRANSACTIONS + "." + KEY_METHODID + ")" +
                    " THEN " + KEY_METHODID +
                    " ELSE null " +
                " END " +
            " WHERE " + KEY_ROWID + " = ? " +
            //IS NOT instead of != accepts cases where transfer_account is null
            " AND " + KEY_TRANSFER_ACCOUNT + " IS NOT ?",
          new String[]{target,target,segment,target});
      count=1;
      break;
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
    if (uriMatch == TRANSACTIONS || uriMatch == TRANSACTION_ID) {
      getContext().getContentResolver().notifyChange(TRANSACTIONS_URI, null);
      getContext().getContentResolver().notifyChange(ACCOUNTS_URI, null);
      getContext().getContentResolver().notifyChange(UNCOMMITTED_URI, null);
    } else
      getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }
  static {
    URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    URI_MATCHER.addURI(AUTHORITY, "transactions", TRANSACTIONS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/uncommitted", UNCOMMITTED);
    URI_MATCHER.addURI(AUTHORITY, "transactions/groups/*", TRANSACTIONS_GROUPS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/sumsForAccountsGroupedByType/#", TRANSACTIONS_SUMS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#", TRANSACTION_ID);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#/move/#", TRANSACTION_MOVE);
    URI_MATCHER.addURI(AUTHORITY, "categories", CATEGORIES);
    URI_MATCHER.addURI(AUTHORITY, "categories/#", CATEGORY_ID);
    URI_MATCHER.addURI(AUTHORITY, "categories/#/increaseUsage", CATEGORY_INCREASE_USAGE);
    URI_MATCHER.addURI(AUTHORITY, "accounts", ACCOUNTS);
    URI_MATCHER.addURI(AUTHORITY, "accounts/#", ACCOUNT_ID);
    URI_MATCHER.addURI(AUTHORITY, "accounts/#/increaseUsage", ACCOUNT_INCREASE_USAGE);
    URI_MATCHER.addURI(AUTHORITY, "payees", PAYEES);
    URI_MATCHER.addURI(AUTHORITY, "payees/#", PAYEES_ID);
    URI_MATCHER.addURI(AUTHORITY, "methods", METHODS);
    URI_MATCHER.addURI(AUTHORITY, "methods/#", METHOD_ID);
    //methods/typeFilter/{TransactionType}/{AccountType}
    //TransactionType: 1 Income, -1 Expense
    //AccountType: CASH BANK CCARD ASSET LIABILITY
    URI_MATCHER.addURI(AUTHORITY, "methods/typeFilter/*/*", METHODS_FILTERED);
    URI_MATCHER.addURI(AUTHORITY, "accounts/aggregates", AGGREGATES);
    URI_MATCHER.addURI(AUTHORITY, "accounts/aggregates/count", AGGREGATES_COUNT);
    URI_MATCHER.addURI(AUTHORITY, "accounttypes_methods", ACCOUNTTYPES_METHODS);
    URI_MATCHER.addURI(AUTHORITY, "templates", TEMPLATES);
    URI_MATCHER.addURI(AUTHORITY, "templates/#", TEMPLATES_ID);
    URI_MATCHER.addURI(AUTHORITY, "templates/#/increaseUsage", TEMPLATES_INCREASE_USAGE);
    URI_MATCHER.addURI(AUTHORITY, "feature_used", FEATURE_USED);
    URI_MATCHER.addURI(AUTHORITY, "sqlite_sequence/*", SQLITE_SEQUENCE_TABLE);
  }
  public void resetDatabase() {
    mOpenHelper.close();
    mOpenHelper = new TransactionDatabase(getContext());
}
  /**
   * A test package can call this to get a handle to the database underlying NotePadProvider,
   * so it can insert test data into the database. The test case class is responsible for
   * instantiating the provider in a test context; {@link android.test.ProviderTestCase2} does
   * this during the call to setUp()
   *
   * @return a handle to the database helper object for the provider's data.
   */
  public TransactionDatabase getOpenHelperForTest() {
      return mOpenHelper;
  }

}
