package org.totschnig.myexpenses.provider;

import org.totschnig.myexpenses.model.*;

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

  protected static TransactionDatabase mOpenHelper;
  private static final boolean debug = false;
  public static final String AUTHORITY = "org.totschnig.myexpenses";
  public static final Uri ACCOUNTS_URI = Uri.parse("content://" + AUTHORITY
      + "/accounts");
  public static final Uri TRANSACTIONS_URI = Uri.parse("content://" + AUTHORITY
      + "/transactions");
  public static final Uri TEMPLATES_URI    = Uri.parse("content://" + AUTHORITY
      + "/templates");
  public static final Uri CATEGORIES_URI   = Uri.parse("content://" + AUTHORITY
      + "/categories");
  public static final Uri AGGREGATES_URI   = Uri.parse("content://" + AUTHORITY
      + "/aggregates");
  public static final Uri PAYEES_URI   = Uri.parse("content://" + AUTHORITY
      + "/payees");
  public static final Uri METHODS_URI   = Uri.parse("content://" + AUTHORITY
      + "/methods");
  public static final Uri ACCOUNTTYPES_METHODS_URI   = Uri.parse("content://" + AUTHORITY
      + "/accounttypes_methods");
  public static final Uri FEATURE_USED_URI =  Uri.parse("content://" + AUTHORITY
      + "/feature_used");
  public static final Uri SQLITE_SEQUENCE_TRANSACTIONS_URI = Uri.parse("content://" + AUTHORITY
      + "/sqlite_sequence/" + TABLE_TRANSACTIONS);

  
  static final String TAG = "TransactionProvider";

  private static final UriMatcher URI_MATCHER;
  //Basic tables
  private static final int TRANSACTIONS = 1;
  private static final int TRANSACTIONS_ID = 2;
  private static final int CATEGORIES = 3;
  private static final int ACCOUNTS = 4;
  private static final int ACCOUNTS_ID = 5;
  private static final int AGGREGATES = 6;
  private static final int PAYEES = 7;
  private static final int METHODS = 8;
  private static final int METHOD_ID = 9;
  private static final int ACCOUNTTYPES_METHODS = 10;
  private static final int TEMPLATES = 11;
  private static final int TEMPLATES_ID = 12;
  private static final int CATEGORIES_ID = 13;
  private static final int CATEGORIES_INCREASE_USAGE = 14;
  private static final int PAYEES_ID = 15;
  private static final int METHODS_FILTERED = 16;
  private static final int TEMPLATES_INCREASE_USAGE = 17;
  private static final int FEATURE_USED = 18;
  private static final int SQLITE_SEQUENCE_TABLE = 19;
  
  @Override
  public boolean onCreate() {
    mOpenHelper = new TransactionDatabase(getContext());
    Log.i("DEBUG",getContext().toString());
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
      defaultOrderBy = KEY_DATE + " DESC";
      if (projection == null)
        projection = Transaction.PROJECTION;
      break;
    case TRANSACTIONS_ID:
      qb.setTables(TABLE_TRANSACTIONS);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case CATEGORIES:
      qb.setTables(TABLE_CATEGORIES);
      if (projection == null)
        projection = Category.PROJECTION;
      //qb.appendWhere("parent_id=" + uri.getPathSegments().get(1));
      //boolean categories_sort = MyApplication.getInstance().getSettings()
      //  .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true);
      //String orderBy = (categories_sort ? "usages DESC, " : "") + "label";
      break;
    case CATEGORIES_ID:
      qb.setTables(TABLE_CATEGORIES);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case ACCOUNTS:
      qb.setTables(TABLE_ACCOUNTS);
      if (projection == null)
        projection = Account.PROJECTION;
      break;
    case ACCOUNTS_ID:
      qb.setTables(TABLE_ACCOUNTS);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case AGGREGATES:
      qb.setTables("(select currency,opening_balance,"+
          "(SELECT coalesce(abs(sum(amount)),0) FROM transactions WHERE account_id = accounts._id and amount<0 and transfer_peer = 0) as sum_expenses," +
          "(SELECT coalesce(abs(sum(amount)),0) FROM transactions WHERE account_id = accounts._id and amount>0 and transfer_peer = 0) as sum_income," +
          "opening_balance + (SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id) as current_balance " +
          "from " + TABLE_ACCOUNTS + ") as t");
      groupBy = "currency";
      having = "count(*) > 1";
      projection = new String[] {"1 as _id","currency",
          "sum(opening_balance) as opening_balance",
          "sum(sum_income) as sum_income",
          "sum(sum_expenses) as sum_expenses",
          "sum(current_balance) as current_balance"};
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
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
      break;
    case METHODS_FILTERED:
      qb.setTables(TABLE_METHODS + " JOIN " + TABLE_ACCOUNTTYES_METHODS + " ON (" + KEY_ROWID + " = " + KEY_METHODID + ")");
      projection =  new String[] {KEY_ROWID,"label"};
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
      qb.setTables(TABLE_TEMPLATES);
      defaultOrderBy =  "usages DESC";
      break;
    case TEMPLATES_ID:
      qb.setTables(TABLE_TEMPLATES);
      qb.appendWhere(KEY_ROWID + "=" + uri.getPathSegments().get(1));
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
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    long id = 0;
    String newUri;
    switch (URI_MATCHER.match(uri)) {
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
    return id >0 ? Uri.parse(newUri) : null;
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    if (debug)
      Log.d(TAG, "Delete for URL: " + uri);
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    int count;
    String whereString;
    String segment;
    switch (URI_MATCHER.match(uri)) {
    case TRANSACTIONS:
      count = db.delete(TABLE_TRANSACTIONS, where, whereArgs);
      break;
    case TRANSACTIONS_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.delete(TABLE_TRANSACTIONS, "_id=" + segment + whereString,
          whereArgs);
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
    case ACCOUNTS_ID:
      segment = uri.getPathSegments().get(1);
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.delete(TABLE_ACCOUNTS, "_id=" + segment + whereString,
          whereArgs);
      break;
    case CATEGORIES:
      count = db.delete(TABLE_CATEGORIES, where, whereArgs);
      break;
    case CATEGORIES_ID:
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
    switch (URI_MATCHER.match(uri)) {
    case TRANSACTIONS:
      count = db.update(TABLE_TRANSACTIONS, values, where, whereArgs);
      break;
    case TRANSACTIONS_ID:
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
    case ACCOUNTS_ID:
      segment = uri.getPathSegments().get(1); 
      if (!TextUtils.isEmpty(where)) {
        whereString = " AND (" + where + ')';
      } else {
        whereString = "";
      }
      count = db.update(TABLE_ACCOUNTS, values, "_id=" + segment + whereString,
          whereArgs);
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
    case CATEGORIES:
      count = db.update(TABLE_CATEGORIES, values, where, whereArgs);
      break;
    case CATEGORIES_ID:
      segment = uri.getPathSegments().get(1);
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
    case CATEGORIES_INCREASE_USAGE:
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
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }
  static {
    URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    URI_MATCHER.addURI(AUTHORITY, "transactions", TRANSACTIONS);
    URI_MATCHER.addURI(AUTHORITY, "transactions/#", TRANSACTIONS_ID);
    URI_MATCHER.addURI(AUTHORITY, "categories", CATEGORIES);
    URI_MATCHER.addURI(AUTHORITY, "categories/#", CATEGORIES_ID);
    URI_MATCHER.addURI(AUTHORITY, "categories/#/increaseUsage", CATEGORIES_INCREASE_USAGE);
    URI_MATCHER.addURI(AUTHORITY, "accounts", ACCOUNTS);
    URI_MATCHER.addURI(AUTHORITY, "accounts/#", ACCOUNTS_ID);
    URI_MATCHER.addURI(AUTHORITY, "payees", PAYEES);
    URI_MATCHER.addURI(AUTHORITY, "payees/#", PAYEES_ID);
    URI_MATCHER.addURI(AUTHORITY, "methods", METHODS);
    URI_MATCHER.addURI(AUTHORITY, "methods/#", METHOD_ID);
    //methods/typeFilter/{TransactionType}/{AccountType}
    //TransactionType: 1 Income, -1 Expense
    //AccountType: CASH BANK CCARD ASSET LIABILITY
    URI_MATCHER.addURI(AUTHORITY, "methods/typeFilter/*/*", METHODS_FILTERED);
    URI_MATCHER.addURI(AUTHORITY, "aggregates", AGGREGATES);
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
