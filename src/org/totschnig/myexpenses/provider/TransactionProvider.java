package org.totschnig.myexpenses.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

public class TransactionProvider extends ContentProvider {

  private TransactionDatabase mOpenHelper;
  private static final boolean debug = true;
  public static final String AUTHORITY = "org.totschnig.myexpenses";
  public static final Uri ACCOUNTS_URI = Uri.parse("content://" + AUTHORITY
      + "/accounts");
  
  static final String TAG = "TransactionProvider";

  private static final UriMatcher URI_MATCHER;
  //Basic tables
  private static final int TRANSACTIONS = 1;
  private static final int TRANSACTIONS_ID = 2;
  private static final int CATEGORIES = 3;
  private static final int ACCOUNTS = 4;
  private static final int ACCOUNTS_ID = 5;
  private static final int AGGREGATES_FOR_CURRENCIES_HAVING_MULTIPLE_ACCOUNTS = 8;
  private static final int PAYEES = 9;
  private static final int PAYMENT_METHODS = 10;
  private static final int PAYMENT_METHOD_ID = 11;
  private static final int ACCOUNT_TYPES_FOR_METHOD = 12;
  private static final int TEMPLATES = 13;
  private static final int TEMPLATE_ID = 14;
  
  @Override
  public boolean onCreate() {
    mOpenHelper = new TransactionDatabase(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    if (debug)
      Log.d(TAG, "Query for URL: " + uri);
    String defaultOrderBy = null;
    String groupBy = null;
    String having = null;

    switch (URI_MATCHER.match(uri)) {
    case TRANSACTIONS:
      qb.setTables(TABLE_TRANSACTIONS);
      qb.appendWhere("account_id=" + uri.getPathSegments().get(1));
      defaultOrderBy = KEY_DATE + " DESC";
      break;
    case TRANSACTIONS_ID:
      qb.setTables(TABLE_TRANSACTIONS);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case CATEGORIES:
      qb.setTables(TABLE_CATEGORIES);
      qb.appendWhere("parent_id=" + uri.getPathSegments().get(1));
      //boolean categories_sort = MyApplication.getInstance().getSettings()
      //  .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true);
      //String orderBy = (categories_sort ? "usages DESC, " : "") + "label";
      break;
    case ACCOUNTS:
      qb.setTables(TABLE_ACCOUNTS);
      break;
    case ACCOUNTS_ID:
      qb.setTables(TABLE_ACCOUNTS);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case AGGREGATES_FOR_CURRENCIES_HAVING_MULTIPLE_ACCOUNTS:
      qb.setTables("(select currency,opening_balance,"+
          "(SELECT coalesce(abs(sum(amount)),0) FROM transactions WHERE account_id = accounts._id and amount<0 and transfer_peer = 0) as sum_expenses," +
          "(SELECT coalesce(abs(sum(amount)),0) FROM transactions WHERE account_id = accounts._id and amount>0 and transfer_peer = 0) as sum_income," +
          "opening_balance + (SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id) as current_balance " +
          "from " + TABLE_ACCOUNTS + ") as t");
      groupBy = "currency";
      having = "count(*) > 1";
      break;
    case PAYEES:
      qb.setTables(TABLE_PAYEE);
      break;
    case PAYMENT_METHODS:
      qb.setTables(TABLE_PAYMENT_METHODS);
      break;
    case PAYMENT_METHOD_ID:
      qb.setTables(TABLE_PAYMENT_METHODS);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case ACCOUNT_TYPES_FOR_METHOD:
      qb.setTables(TABLE_ACCOUNTTYE_METHOD);
      qb.appendWhere("method_id =" + uri.getPathSegments().get(1));
      break;
    case TEMPLATES:
      qb.setTables(TABLE_TEMPLATES);
      qb.appendWhere("account_id =" + uri.getPathSegments().get(1));
      defaultOrderBy =  "usages DESC";
      break;
    case TEMPLATE_ID:
      qb.setTables(TABLE_TEMPLATES);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
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
    if (debug) {
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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    // TODO Auto-generated method stub
    return 0;
  }
  static {
    URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    URI_MATCHER.addURI(AUTHORITY, "accounts/#/transactions", TRANSACTIONS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#", TRANSACTIONS_ID);
    //categories/0/children gives main categories
    URI_MATCHER.addURI(AUTHORITY, "categories/#/children", CATEGORIES);
    URI_MATCHER.addURI(AUTHORITY, "accounts", ACCOUNTS);
    URI_MATCHER.addURI(AUTHORITY, "accounts/#", ACCOUNTS_ID);
    URI_MATCHER.addURI(AUTHORITY, "payees", PAYEES);
    URI_MATCHER.addURI(AUTHORITY, "payment_methods", PAYMENT_METHODS);
    URI_MATCHER.addURI(AUTHORITY, "payment_methods/#", PAYMENT_METHOD_ID);
    URI_MATCHER.addURI(AUTHORITY, "currencies/aggregates", AGGREGATES_FOR_CURRENCIES_HAVING_MULTIPLE_ACCOUNTS);
    URI_MATCHER.addURI(AUTHORITY, "payment_methods/#/account_types", ACCOUNT_TYPES_FOR_METHOD);
    URI_MATCHER.addURI(AUTHORITY, "accounts/#/templates", TEMPLATES);
    URI_MATCHER.addURI(AUTHORITY, "templates/#", TEMPLATE_ID);
  }
}
