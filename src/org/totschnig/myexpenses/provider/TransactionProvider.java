package org.totschnig.myexpenses.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class TransactionProvider extends ContentProvider {

  private TransactionDatabase mOpenHelper;
  
  static final String TAG = "TransactionProvider";

  private static final UriMatcher URI_MATCHER;
  //Basic tables
  private static final int TRANSACTIONS = 1;
  private static final int TRANSACTION_ID = 2;
  private static final int CATEGORIES = 3;
  private static final int ACCOUNTS = 5;
  private static final int ACCOUNT_ID = 6;
  private static final int AGGREGATES_FOR_CURRENCIES_HAVING_MULTIPLE_ACCOUNTS = 7;
  private static final int PAYEES = 8;
  private static final int PAYMENT_METHODS = 9;
  private static final int PAYMENT_METHOD_ID = 10;
  private static final int ACCOUNT_TYPES_FOR_METHOD = 11;
  private static final int TEMPLATES = 12;
  private static final int TEMPLATE_ID = 13;
  
  @Override
  public boolean onCreate() {
    mOpenHelper = new TransactionDatabase(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    String defaultOrderBy = null;
    String groupBy = null;

    switch (URI_MATCHER.match(uri)) {
    }
    return null;
  }

  @Override
  public String getType(Uri uri) {
    // TODO Auto-generated method stub
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
    URI_MATCHER.addURI("org.totschnig.myexpenses", "transactions", TRANSACTIONS);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "transactions/#", TRANSACTION_ID);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "categories", CATEGORIES);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "accounts", ACCOUNTS);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "accounts/#", ACCOUNT_ID);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "payees", PAYEES);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "payment_methods", PAYMENT_METHODS);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "payment_methods/#", PAYMENT_METHOD_ID);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "currencies/aggregates", AGGREGATES_FOR_CURRENCIES_HAVING_MULTIPLE_ACCOUNTS);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "account_types/for_payment_method/#", ACCOUNT_TYPES_FOR_METHOD);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "templates", TEMPLATES);
    URI_MATCHER.addURI("org.totschnig.myexpenses", "templates/#", TEMPLATE_ID);
  }
}
