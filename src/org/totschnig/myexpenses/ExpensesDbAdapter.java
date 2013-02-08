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

package org.totschnig.myexpenses;

import java.io.File;
import java.util.ArrayList;

import org.totschnig.myexpenses.Account.Type;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Simple  database access helper class. Defines the basic CRUD operations
 * and gives the ability to manipulate transactions, accounts, categories, payees.
 * follows the pattern from the Notepad example of the Android SDK
 *
 * @author Michael Totschnig
 *
 */
public class ExpensesDbAdapter {

  public static final String KEY_DATE = "date";
  public static final String KEY_AMOUNT = "amount";
  public static final String KEY_COMMENT = "comment";
  public static final String KEY_ROWID = "_id";
  public static final String KEY_CATID = "cat_id";
  public static final String KEY_ACCOUNTID = "account_id";
  public static final String KEY_PAYEE = "payee";
  public static final String KEY_TRANSFER_PEER = "transfer_peer";
  public static final String KEY_METHODID = "payment_method_id";
  public static final String KEY_TITLE = "title";
  public static final String KEY_LABEL_MAIN = "label_sub";
  public static final String KEY_LABEL_SUB = "label_main";
  public static final String BACKUP_DB_PATH = "BACKUP";

  private static final String TAG = "ExpensesDbAdapter";
  private DatabaseHelper mDbHelper;
  private SQLiteDatabase mDb;


  private String mDatabaseName;
  private static final String DATABASE_TABLE = "transactions";
  private static final int DATABASE_VERSION = 26;

  /**
   * SQL statement for expenses TABLE
   * both transactions and transfers are stored in this table
   * for transfers there are two rows (one per account) which
   * are linked by transfer_peer
   * for normal transactions transfer_peer is set to NULL
   * for transfers cat_id stores the account
   */
  private static final String DATABASE_CREATE =
    "CREATE TABLE " + DATABASE_TABLE  +  "( "
    + KEY_ROWID         + " integer primary key autoincrement, "
    + KEY_COMMENT       + " text not null, "
    + KEY_DATE          + " DATETIME not null, "
    + KEY_AMOUNT        + " integer not null, "
    + KEY_CATID         + " integer, "
    + KEY_ACCOUNTID     + " integer, "
    + KEY_PAYEE         + " text, "
    + KEY_TRANSFER_PEER + " integer default 0, "
    + KEY_METHODID      + " integer);";


  /**
   * SQL statement for accounts TABLE
   */
  private static final String ACCOUNTS_CREATE =
    "CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, " +
    "opening_balance integer, description text, currency text not null, type text default 'CASH', color integer default -3355444);";


  /**
   * SQL statement for categories TABLE
   * Table definition reflects format of Grisbis categories
   * Main categories have parent_id 0
   * usages counts how often the cat is selected
   */
  private static final String CATEGORIES_CREATE =
    "CREATE TABLE categories (_id integer primary key autoincrement, label text not null, " +
    "parent_id integer not null default 0, usages integer default 0, unique (label,parent_id));";

  private static final String PAYMENT_METHODS_CREATE =
      "CREATE TABLE paymentmethods (_id integer primary key autoincrement, label text not null, type integer default 0);";

  private static final String ACCOUNTTYE_METHOD_CREATE =
      "CREATE TABLE accounttype_paymentmethod (type text, method_id integer, primary key (type,method_id));";

  private static final String TEMPLATE_CREATE =
      "CREATE TABLE templates ( "
      + KEY_ROWID         + " integer primary key autoincrement, "
      + KEY_COMMENT       + " text not null, "
      + KEY_AMOUNT        + " integer not null, "
      + KEY_CATID         + " integer, "
      + KEY_ACCOUNTID     + " integer, "
      + KEY_PAYEE         + " text, "
      + KEY_TRANSFER_PEER + " integer default 0, "
      + KEY_METHODID      + " integer, "
      + KEY_TITLE         + " text not null, "
      + "usages integer default 0, "
      + "unique(" + KEY_ACCOUNTID + "," + KEY_TITLE + "));";
  /**
   * an SQL CASE expression for transactions
   * that gives either the category for normal transactions
   * or the account for transfers
   * full means "Main : Sub"
   */
  private static final String LABEL_MAIN =
    "CASE WHEN " +
    "  transfer_peer " +
    "THEN " +
    "  (SELECT label FROM accounts WHERE _id = cat_id) " +
    "WHEN " +
    "  cat_id " +
    "THEN " +
    "  CASE WHEN " +
    "    (SELECT parent_id FROM categories WHERE _id = cat_id) " +
    "  THEN " +
    "    (SELECT label FROM categories WHERE _id = (SELECT parent_id FROM categories WHERE _id = cat_id)) " +
    "  ELSE " +
    "    (SELECT label FROM categories WHERE _id = cat_id) " +
    "  END " +
    "END AS " + KEY_LABEL_MAIN;
 private static final String LABEL_SUB =
    "CASE WHEN " +
    "  NOT transfer_peer AND cat_id AND (SELECT parent_id FROM categories WHERE _id = cat_id) " +
    "THEN " +
    "  (SELECT label FROM categories WHERE _id = cat_id) " +
    "END AS " + KEY_LABEL_SUB;
  /**
   * same as {@link FULL_LABEL}, but if transaction is linked to a subcategory
   * only the label from the subcategory is returned
   */
  private static final String SHORT_LABEL = 
    "CASE WHEN " +
    "  transfer_peer " +
    "THEN " +
    "  (SELECT label FROM accounts WHERE _id = cat_id) " +
    "ELSE " +
    "  (SELECT label FROM categories WHERE _id = cat_id) " +
    "END AS label";

  /**
   * stores payees and payers
   */
  private static final String PAYEE_CREATE =
    "CREATE TABLE payee (_id integer primary key autoincrement, name text unique not null);";

  private final MyApplication mCtx;

  /**
   * the helper class with hooks for creating and updating the database
   */
  private static class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context,String databaseName) {
      super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

      db.execSQL(DATABASE_CREATE);
      db.execSQL(CATEGORIES_CREATE);
      db.execSQL(ACCOUNTS_CREATE);
      db.execSQL(PAYEE_CREATE);
      db.execSQL(PAYMENT_METHODS_CREATE);
      db.execSQL(ACCOUNTTYE_METHOD_CREATE);
      insertDefaultPaymentMethods(db);
      db.execSQL(TEMPLATE_CREATE);

    }

    /**
     * @param db
     * insert the predefined payment methods in the database, all of them are valid only for bank accounts
     */
    private void insertDefaultPaymentMethods(SQLiteDatabase db) {
      ContentValues initialValues;
      long _id;
      for (PaymentMethod.PreDefined pm: PaymentMethod.PreDefined.values()) {
        initialValues = new ContentValues();
        initialValues.put("label", pm.name());
        initialValues.put("type",pm.paymentType);
        _id = db.insert("paymentmethods", null, initialValues);
        initialValues = new ContentValues();
        initialValues.put("method_id", _id);
        initialValues.put("type","BANK");
        db.insert("accounttype_paymentmethod", null, initialValues);
      }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
          + newVersion + ".");
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
            + "date DATETIME not null, amount integer not null, cat_id integer, account_id integer, "
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
        insertDefaultPaymentMethods(db);
        db.execSQL("ALTER TABLE transactions add column payment_method_id text default 'CASH'");
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
          Log.e(TAG,e.getLocalizedMessage());
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

    }
  }

  /**
   * Constructor - takes the context to allow the database to be
   * opened/created
   *
   * @param ctx the Context within which to work
   */
  public ExpensesDbAdapter(MyApplication ctx) {
    this.mCtx = ctx;
    mDatabaseName = ctx.getDatabaseName();
  }

  /**
   * Open the expenses database. If it cannot be opened, try to create a new
   * instance of the database. If it cannot be created, throw an exception to
   * signal the failure
   *
   * @return this (self reference, allowing this to be chained in an
   *         initialization call)
   * @throws SQLException if the database could be neither opened or created
   */
  public ExpensesDbAdapter open() throws SQLException {
    mDbHelper = new DatabaseHelper(mCtx,mDatabaseName);
    mDb = mDbHelper.getWritableDatabase();
    return this;
  }

  public void close() {
    mDbHelper.close();
  }

  public File getBackupFile() {
    File appDir = Utils.requireAppDir();
    if (appDir == null)
      return null;
    return new File(appDir, BACKUP_DB_PATH);
  }

  public boolean backup() {
    File backupDb = getBackupFile();
    if (backupDb == null)
      return false;
    File currentDb = new File(mDb.getPath());

    if (currentDb.exists()) {
      return Utils.copy(currentDb, backupDb);
    }
    return false;
  }
  /**
   * should only be called during the first run, before the database is created
   * @return
   */
  public boolean maybeRestore() {
    try {
      File dataDir = new File("/data/data/"+ mCtx.getPackageName()+ "/databases/");
      dataDir.mkdir();
      File backupDb = getBackupFile();
      if (backupDb == null)
        return false;
      //line below gives app_databases instead of databases ???
      //File currentDb = new File(mCtx.getDir("databases", 0),mDatabaseName);
      File currentDb = new File(dataDir,mDatabaseName);

      if (backupDb.exists()) {
        return Utils.copy(backupDb,currentDb);
      }
    } catch (Exception e) {
      Log.e(TAG,e.getLocalizedMessage());
    }
    return false;
  }


  /**
   * TRANSACTIONS
   */

  /**
   * Create a new transaction using the date, amount and comment provided. If the note is
   * successfully created return the new rowId for that note, otherwise return
   * a -1 to indicate failure.
   *
   * @param date the date of the expense
   * @param amount the amount of the expense
   * @param comment the comment describing the expense
   * @return rowId or -1 if failed
   */
  public long createTransaction(String date, long amount, String comment,
      long cat_id,long account_id, String payee, long payment_method_id) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_DATE, date);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_CATID, cat_id);
    initialValues.put(KEY_ACCOUNTID, account_id);
    initialValues.put(KEY_PAYEE, payee);
    initialValues.put(KEY_METHODID, payment_method_id);
    initialValues.put(KEY_TRANSFER_PEER,0);
    long _id = mDb.insert(DATABASE_TABLE, null, initialValues);
    incrCategoryUsage(cat_id);
    return _id;
  }
  /**
   * Create a new transfer pair of transactions using the date, amount and comment provided.
   *
   *
   * @param date the date of the expense
   * @param amount the amount of the expense
   * @param comment the comment describing the expense
   * @param cat_id this stores the peer account
   * @param account_id stores the account in whose context the transaction is created
   * @return rowId or -1 if failed
   */
  public long[] createTransfer(String date, long amount, String comment,
      long cat_id,long account_id) {
    //the id of the account is stored in KEY_CATID,
    //the id of the peer transaction is stored in KEY_TRANSFER_PEER
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_DATE, date);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_CATID, cat_id);
    initialValues.put(KEY_ACCOUNTID, account_id);
    long _id = mDb.insert(DATABASE_TABLE, null, initialValues);
    initialValues.put(KEY_AMOUNT, 0 - amount);
    initialValues.put(KEY_CATID, account_id);
    initialValues.put(KEY_ACCOUNTID, cat_id);
    initialValues.put(KEY_TRANSFER_PEER,_id);
    long transfer_peer = mDb.insert(DATABASE_TABLE, null, initialValues);
    ContentValues args = new ContentValues();
    args.put(KEY_TRANSFER_PEER,transfer_peer);
    mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + _id, null);
    return new long[] {_id,transfer_peer};
  }
  /**
   * Update the transaction using the details provided. The expense to be updated is
   * specified using the rowId, and it is altered to use the date, amount and comment
   * values passed in
   * as a side effect, increases the usage counters for categories
   *
   * @param rowId id of transaction to update
   * @param date value to set
   * @param amount value to set
   * @param comment value to set
   * @return should return 1 if row has been successfully updated
   */
  public int updateTransaction(long rowId, String date, long amount,
      String comment,long cat_id,String payee, long payment_method_id) {
    ContentValues args = new ContentValues();
    args.put(KEY_DATE, date);
    args.put(KEY_AMOUNT, amount);
    args.put(KEY_COMMENT, comment);
    args.put(KEY_CATID, cat_id);
    args.put(KEY_PAYEE, payee);
    args.put(KEY_METHODID, payment_method_id);

    int result = mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null);
    incrCategoryUsage(cat_id);
    return result;
  }
  public int moveTransaction(long rowId, long accountId) {
    ContentValues args = new ContentValues();
    args.put(KEY_ACCOUNTID, accountId);
    return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null);
  }

  /**
   * Update the transfer using the details provided. The expense to be updated is
   * specified using the rowId, and it is altered to use the date, amount and comment
   * values passed in
   *
   * @param rowId id of transaction to update
   * @param date value to set
   * @param amount value to set
   * @param comment value to set
   * @param cat_id stores the peer_account, this can be altered by the user
   * @return should return 2 if both transactions have been successfully updated
   */
  public int updateTransfer(long rowId, String date, long amount,
      String comment, long cat_id) {
    int result = 0;
    ContentValues args = new ContentValues();
    args.put(KEY_DATE, date);
    args.put(KEY_AMOUNT, amount);
    args.put(KEY_COMMENT, comment);
    args.put(KEY_CATID, cat_id);
    result += mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null);
    args.put(KEY_AMOUNT, 0 -amount);
    //if the user has changed the account to which we should transfer,
    //in the peer transaction we need to update the account_id
    args.put(KEY_ACCOUNTID, cat_id);
    //the account from which is transfered is not altered
    args.remove(KEY_CATID);
    result += mDb.update(DATABASE_TABLE, args,
        KEY_ROWID + "= (SELECT transfer_peer FROM " + DATABASE_TABLE +
        " WHERE " + KEY_ROWID + " = " + rowId + ")",
        null);
    return result;
  }

  /**
   * Delete the transaction with the given rowId
   *
   * @param rowId id of note to delete
   * @return true if deleted, false otherwise
   */
  public boolean deleteTransaction(long rowId) {

    return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
  }

  /**
   * Delete both transactions that make up a transfer
   * it is the caller's responsibilty to pass in two ids that
   * actually are linked
   * @param rowId
   * @param transfer_peer
   * @return
   */
  public boolean deleteTransfer(long rowId, long transfer_peer) {
    return mDb.delete(DATABASE_TABLE,
        KEY_ROWID + " in (" + rowId + "," + transfer_peer + ")",
        null) > 0;
  }

  /**
   * Deletes all transactions for a given account
   * For transfers the peer transaction will survive, but we transform it to a normal transaction
   * with a note about the deletion of the peer_transaction
   *
   * @param account_id
   */
  public void deleteTransactionAll(Account account ) {
    //TODO:
    //starting with Android 2.2., we could handle this easier with foreign keys

    //to speed up the loop for transfers, we delete in the first step all entries that are not transfers
    String[] selectArgs = new String[] { String.valueOf(account.id) };

    mDb.delete(DATABASE_TABLE, "account_id = ? and transfer_peer = 0", selectArgs);

    Cursor c = mDb.query(DATABASE_TABLE,
        new String[] {KEY_ROWID,KEY_TRANSFER_PEER},
        "account_id = ?",selectArgs,null,null,null);
    c.moveToFirst();
    while(!c.isAfterLast()) {
      long transfer_peer = c.getLong(c.getColumnIndex(ExpensesDbAdapter.KEY_TRANSFER_PEER));
      if (transfer_peer != 0) {
        ContentValues args = new ContentValues();
        args.put(KEY_COMMENT, mCtx.getString(R.string.peer_transaction_deleted,account.label));
        args.put(KEY_CATID,0);
        args.put(KEY_TRANSFER_PEER,0);
        mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + transfer_peer, null);
      }
      deleteTransaction(c.getLong(c.getColumnIndex(ExpensesDbAdapter.KEY_ROWID)));
      c.moveToNext();
    }
    c.close();
  }

  /**
   * before 1.4.9.1 transactions were not deleted, when an account was deleted,
   * during installation of 1.4.9.1 this method was used to purge transactions
   * from deleted accounts
   */
  public int purgeTransactions() {
    return mDb.delete(DATABASE_TABLE,
        KEY_ACCOUNTID + " not in (SELECT _id FROM accounts)",
        null);
  }

  /**
   * Return a Cursor over the list of all expenses in the database for an account
   * exposes the full label which concatenate main and sub label if appropriate
   * @return Cursor over all transactions
   */
  public Cursor fetchTransactionAll(long account_id) {

    return mDb.query(DATABASE_TABLE,
        new String[] {KEY_ROWID,KEY_DATE,KEY_AMOUNT, KEY_COMMENT,
            KEY_CATID,LABEL_MAIN,LABEL_SUB,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_METHODID},
        "account_id = " + account_id, null, null, null, KEY_DATE + " DESC");
  }

  /**
   * Return a Cursor positioned at the transaction that matches the given rowId
   * exposes just the label for the linked category
   * @param rowId id of transaction to retrieve
   * @return Cursor positioned to matching transaction, if found
   * @throws SQLException if transaction could not be found/retrieved
   */
  public Cursor fetchTransaction(long rowId) throws SQLException {

    Cursor mCursor =
      mDb.query(DATABASE_TABLE,
          new String[] {KEY_ROWID,KEY_DATE,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,
              SHORT_LABEL,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_ACCOUNTID,KEY_METHODID},
          KEY_ROWID + "=" + rowId,
          null, null, null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;
  }

  /**
   * calculates sum of all transcations for an account
   * @param account_id
   * @return
   */
  public long getTransactionSum(long account_id) {
    Cursor mCursor = mDb.rawQuery("SELECT sum(" + KEY_AMOUNT + ") FROM " +
        DATABASE_TABLE +  " WHERE account_id = " + account_id,
        null);
    mCursor.moveToFirst();
    long result = mCursor.getLong(0);
    mCursor.close();
    return result;
  }

  /**
   * CATEGORIES
   */

  /**
   * Creates a new category under a parent
   * @param label
   * @param parent_id
   * @return the row ID of the newly inserted row, or -1 if category already exists
   */
  public long createCategory(String label, long parent_id) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("parent_id", parent_id);

    try {
      return mDb.insertOrThrow("categories", null, initialValues);
    } catch (SQLiteConstraintException e) {
      return -1;
    }
  }

  /**
   * Updates the label for category
   * @param label
   * @param cat_id
   * @return number of rows affected, or -1 if unique constraint is violated
   */
  public int updateCategoryLabel(String label, long cat_id) {
    ContentValues args = new ContentValues();
    args.put("label", label);

    try {
      return mDb.update("categories", args, "_id = " + cat_id, null);
    } catch (SQLiteConstraintException e) {
      return -1;
    }
  }

  /**
   * increases the usage counter for a category, and its parent (if it is a subcategory)
   * @param cat_id
   */
  public void incrCategoryUsage(long cat_id) {
      mDb.execSQL("update categories set usages = usages +1 WHERE _id = " + cat_id +
          " or _id = (SELECT parent_id FROM categories WHERE _id = " +cat_id + ")");
  }

  /**
   * Looks for a cat with a label under a given parent
   * @param label
   * @param parent_id
   * @return id or -1 if not found
   */
  public long getCategoryId(String label, long parent_id) {
    Cursor mCursor = mDb.rawQuery(
        "SELECT _id FROM categories WHERE parent_id = ? and label = ?",
        new String[] {String.valueOf(parent_id), label}
    );
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return -1;
    } else {
      mCursor.moveToFirst();
      long result = mCursor.getLong(0);
      mCursor.close();
      return result;
    }
  }

  /**
   * deletes category with give id
   * @param cat_id
   * @return true if a row has been deleted, false otherwise
   */
  public boolean deleteCategory(long cat_id) {
    return mDb.delete("categories", KEY_ROWID + "=" + cat_id, null) > 0;
  }

  /**
   * @return a Cursor that holds all main categories ordered by usage counter
   */
  public Cursor fetchCategoryMain() {
    boolean categories_sort = mCtx.getSettings()
        .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true);
    String orderBy = (categories_sort ? "usages DESC, " : "") + "label";
    return mDb.query("categories",
        new String[] {KEY_ROWID, "label"},
        "parent_id = 0",
        null,
        null,
        null,
        orderBy
    );
  }

  /**
   * How many subcategories under a given parent?
   * @param parent_id
   * @return number of subcategories
   */
  public int getCategoryCountSub(long parent_id){
    Cursor mCursor = mDb.rawQuery(
        "SELECT count(*) FROM categories WHERE parent_id = " + parent_id,
        null);
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }

  /**
   * @param parent_id
   * @return a Cursor that holds all sub categories under a parent ordered by usage counter
   */
  public Cursor fetchCategorySub(long parent_id) {
    boolean categories_sort = mCtx.getSettings()
        .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true);
    String orderBy = (categories_sort ? "usages DESC, " : "") + "label";
    return mDb.query("categories",
        new String[] {KEY_ROWID,"label"},
        "parent_id = " + parent_id,
        null,
        null,
        null,
        orderBy);
  }

  /**
   * ACCOUNTS
   */

  /**
   * updates the currency field of an account
   * @param account_id
   * @param newStr currency symbol
   * @return number of rows affected
   */
  public int updateAccountCurrency(long rowId, String newStr) {
    ContentValues args = new ContentValues();
    args.put("currency",newStr);
    return mDb.update("accounts", args, KEY_ROWID + "=" + rowId, null);
  }

  /**
   * create a new account
   * @param label
   * @param opening_balance
   * @param description
   * @param currency
   * @param color
   * @return rowId or -1 if failed
   */
  public long createAccount(String label, long opening_balance,
      String description, String currency, String type, int color) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("opening_balance",opening_balance);
    initialValues.put("description",description);
    initialValues.put("currency",currency);
    initialValues.put("type",type);
    initialValues.put("color",color);
    return mDb.insert("accounts", null, initialValues);
  }
  /**
   * Updates the account with the given values
   * @param rowId
   * @param label
   * @param opening_balance
   * @param description
   * @param currency
   * @param color
   * @return number of rows affected
   */
  public int updateAccount(long rowId, String label, long opening_balance,
      String description, String currency,String type, int color) {
    ContentValues args = new ContentValues();
    args.put("label", label);
    args.put("opening_balance",opening_balance);
    args.put("description",description);
    args.put("currency",currency);
    args.put("type",type);
    args.put("color",color);
    return mDb.update("accounts", args, KEY_ROWID + "=" + rowId, null);
  }

  /**
   * @return Cursor that holds all accounts
   */
  public Cursor fetchAccountAll() {
    return mDb.query("accounts",
        new String[] {KEY_ROWID,"label","description","opening_balance","currency","color",
        "(SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id and amount>0 and transfer_peer = 0) as sum_income",
        "(SELECT coalesce(abs(sum(amount)),0) FROM transactions WHERE account_id = accounts._id and amount<0 and transfer_peer = 0) as sum_expenses",
        "(SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id and transfer_peer != 0) as sum_transfer",
        "opening_balance + (SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id) as current_balance"},
        null, null, null, null, null);
  }

  public long fetchAccountIdNext(long accountId) {
    String strAccountId = String.valueOf(accountId);
    Cursor mCursor = mDb.rawQuery(
        "SELECT coalesce((SELECT min(_id) FROM accounts WHERE _id > ?),(SELECT min(_id) FROM accounts))",
        new String[] {strAccountId}
    );
    mCursor.moveToFirst();
    Log.d(TAG,String.valueOf(mCursor.getCount()));
    long result = mCursor.getLong(0);
    mCursor.close();
    return result;
  }
  /**
   * fetches all accounts except the one passed in
   * @param account_id
   * @param sameCurrency if true only retrieve accounts with the same currency
   * @return Cursor with all retrieved accounts
   */
  public Cursor fetchAccountOther(long accountId, boolean sameCurrency) {
    String selectionArg = String.valueOf(accountId);
    String selectionArgs[];
    String selection = KEY_ROWID + " != ? ";
    if (sameCurrency) {
      selection += " AND currency = (SELECT currency FROM accounts WHERE " +
          KEY_ROWID + " = ? )";
      selectionArgs = new String[] {selectionArg,selectionArg};
    } else {
      selectionArgs = new String[] {selectionArg};
    }
    return mDb.query("accounts",
        new String[] {KEY_ROWID,"label"},
        selection,
        selectionArgs,
        null,
        null,
        null);
  }

  /**
   * fetches the account with given row id
   * @param rowId
   * @return Cursor with fields "label","description","opening_balance","currency","type", "color"
   * @throws SQLException
   */
  public Cursor fetchAccount(long rowId) throws SQLException {
    Cursor mCursor =
      mDb.query("accounts",
          new String[] {"label","description","opening_balance","currency","type","color"},
          KEY_ROWID + "=" + rowId,
          null, null, null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;
  }

  /**
   * updates the opening balance of an account
   * @param account_id
   * @param opening_balance
   * @return number of affected rows
   */
  public int updateAccountOpeningBalance(long account_id,long opening_balance) {
    ContentValues args = new ContentValues();
    args.put("opening_balance",opening_balance);
    return mDb.update("accounts",args, KEY_ROWID + "=" + account_id,null);
  }

  /**
   * delete the account with the given id
   * @param rowId
   * @return
   */
  public boolean deleteAccount(long rowId) {
    return mDb.delete("accounts", KEY_ROWID + "=" + rowId, null) > 0;
  }

  /**
   * @see #fetchAccountOther(long)
   * @param currency
   * @return number of accounts with the same currency
   * if currency is null return total number of accounts
   */
  public int getAccountCount(String currency) {
    String query = "SELECT count(*) FROM accounts";
    String selectionArgs[] = null;
    if (currency != null) {
      query += " WHERE currency = ?";
      selectionArgs = new String[] {currency};
    }
    Cursor mCursor = mDb.rawQuery(
        query,
        selectionArgs);
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }

  public Long getFirstAccountId() {
    Cursor mCursor = mDb.rawQuery("SELECT min(_id) FROM accounts",null);
    mCursor.moveToFirst();
    Long result;
    if (mCursor.isNull(0))
      result = null;
    else
      result = mCursor.getLong(0);
    mCursor.close();
    return result;
  }

  /**
   * PAYEES
   */

  /**
   * inserts a new payee if it does not exist yet
   * @param name
   */
  public long createPayee(String name) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("name", name);

    try {
      return mDb.insertOrThrow("payee", null, initialValues);
    } catch (SQLiteConstraintException e) {
      return -1;
    }
  }

  /**
   * @return Cursor over all rows of table payee
   */
  public Cursor fetchPayeeAll() {
    return mDb.query("payee",
        new String[] {KEY_ROWID,"name"},
        null, null, null, null, "name");
  }

  public boolean deletePayee(long id) {
    return mDb.delete("payee", KEY_ROWID + "=" + id, null) > 0;
  }
  /**
   * PAYMENT METHODS
   */

  /**
   * inserts a new payment method if it does not exist yet
   * @param name
   */
  public long createMethod(String label, int paymentType, ArrayList<Account.Type>  accountTypes) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("type",paymentType);
    long _id = mDb.insert("paymentmethods", null, initialValues);
    setMethodAccountTypes(_id,accountTypes);
    return _id;
  }

  public int updateMethod(long rowId, String label, int paymentType, ArrayList<Account.Type>  accountTypes) {
    ContentValues args = new ContentValues();
    args.put("label", label);
    args.put("type", paymentType);
    int result = mDb.update("paymentmethods", args, KEY_ROWID + "=" + rowId, null);
    setMethodAccountTypes(rowId,accountTypes);
    return result;
  }

  private void setMethodAccountTypes(long rowId, ArrayList<Account.Type>  accountTypes) {
    mDb.delete("accounttype_paymentmethod", "method_id=" + rowId, null);
    ContentValues initialValues = new ContentValues();
    initialValues.put("method_id", rowId);
    for (Account.Type accountType : accountTypes) {
      initialValues.put("type",accountType.name());
      try {
        mDb.insertOrThrow("accounttype_paymentmethod", null, initialValues);
      } catch (SQLiteConstraintException e) {
        //already mapped
      }
    }
  }

  public Cursor fetchPaymentMethod(long rowId) {
    Cursor mCursor =
        mDb.query("paymentmethods",
            new String[] {"label","type"},
            KEY_ROWID + "=" + rowId,
            null, null, null, null, null);
      if (mCursor != null) {
        mCursor.moveToFirst();
      }
      return mCursor;
  }

  public Cursor fetchPaymentMethodsAll() {
    return mDb.query("paymentmethods",
        new String[] {KEY_ROWID,"label"},
        null, null, null, null, null);
  }

  /**
   * @param paymentType
   * @param accountType
   * @return Cursor
   * return Cursor with paymentMethods valid for a given account type (CASH,BANK, etc) and payment type (expense or income)
   */
  public Cursor fetchPaymentMethodsFiltered(boolean paymentType, Account.Type accountType) {
    String selection;
    if (paymentType == ExpenseEdit.INCOME) {
      selection = "paymentmethods.type > -1";
    } else {
      selection = "paymentmethods.type < 1";
    }
    selection += " and accounttype_paymentmethod.type = ?";

    return mDb.query("paymentmethods join accounttype_paymentmethod on (_id = method_id)",
        new String[] {KEY_ROWID,"label"},
        selection, new String[] {accountType.name()}, null, null, null);
  }

  public Cursor fetchAccountTypesForPaymentMethod(long rowId) {
    return mDb.query("accounttype_paymentmethod", new String[] {"type"},
        "method_id =" + rowId, null, null, null, null);
  }

  public int getPaymentMethodsCount(Type type) {
    Cursor mCursor = mDb.rawQuery("SELECT count(*) FROM accounttype_paymentmethod " +
        " WHERE type = ?",
       new String[] {type.name()});
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }
  public boolean deletePaymentMethod(long id) {
    mDb.delete("accounttype_paymentmethod","method_id = " +id , null);
    return mDb.delete("paymentmethods", KEY_ROWID + "=" + id, null) > 0;
  }

  /**
   * @param accountId
   * @return return all templates for an account, if accountId = 0, return all templates
   */
  public Cursor fetchTemplates(long accountId) {
    String selection = null;
    String[] selectionArgs = null;
    if (accountId != 0L) {
     selection =  "account_id = ?";
     selectionArgs = new String[] { String.valueOf(accountId) };
    }
    return mDb.query("templates",
        new String[] {KEY_ROWID,KEY_TITLE},
        selection,
        selectionArgs,
        null,
        null,
        "usages DESC");
  }

  public boolean deleteTemplate(long id) {
    return mDb.delete("templates", KEY_ROWID + "=" + id, null) > 0;
  }
  public long createTemplate(String date, long amount, String comment,
      long cat_id,long account_id, String payee, long transfer_peer, long payment_method_id, String title) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_CATID, cat_id);
    initialValues.put(KEY_ACCOUNTID, account_id);
    initialValues.put(KEY_PAYEE, payee);
    initialValues.put(KEY_TRANSFER_PEER, transfer_peer);
    initialValues.put(KEY_METHODID, payment_method_id);
    initialValues.put(KEY_TITLE, title);
    try {
      return mDb.insertOrThrow("templates", null, initialValues);
    } catch (SQLiteConstraintException e) {
      return -1;
    }
  }

  public int getTemplateCount(long accountId) {
    Cursor mCursor = mDb.rawQuery("SELECT count(*) FROM templates WHERE " + KEY_ACCOUNTID + " = ?",
        new String[] { String.valueOf(accountId) } );
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }
  /**
   * Return a Cursor positioned at the template that matches the given rowId
   * @param rowId id of transaction to retrieve
   * @return Cursor positioned to matching template, if found
   * @throws SQLException if template could not be found/retrieved
   */
  public Cursor fetchTemplate(long rowId) throws SQLException {
    Cursor mCursor =
      mDb.query("templates",
          new String[] {KEY_ROWID,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,
          SHORT_LABEL,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_ACCOUNTID,KEY_METHODID,KEY_TITLE},
          KEY_ROWID + "=" + rowId,
          null, null, null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;
  }

  public void incrTemplateUsage(long id) {
    mDb.execSQL("update templates set usages = usages +1 WHERE _id = " + id);
  }

  //Counters
  private long getCountFromQuery(String query) {
    Cursor mCursor = mDb.rawQuery("SELECT count(*) FROM " + query,
        null);
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }
  /**
   * @param cat_id
   * @return number of transactions linked to a category
   */
  public long getTransactionCountPerCat(long catId) {
    //since cat_id stores the account to which is transfered for transfers
    //we have to restrict to normal transactions by checking if transfer_peer is 0
    return getCountFromQuery(DATABASE_TABLE +
        " WHERE transfer_peer = 0 and " + KEY_CATID +" = " + catId);
  }
  /**
   * @param cat_id
   * @return number of transactions linked to a method
   */
  public long getTransactionCountPerMethod(long methodId) {
    return getCountFromQuery( DATABASE_TABLE +
        " WHERE " + KEY_METHODID +" = " + methodId);
  }
  /**
   * @param cat_id
   * @return number of transactions linked to a category
   */
  public long getTemplateCountPerCat(long catId) {
    //since cat_id stores the account to which is transfered for transfers
    //we have to restrict to normal transactions by checking if transfer_peer is 0
    return getCountFromQuery("templates WHERE transfer_peer = 0 and " + KEY_CATID +" = " + catId);
  }
  /**
   * @param cat_id
   * @return number of transactions linked to a method
   */
  public long getTemplateCountPerMethod(long methodId) {
    return getCountFromQuery("templates WHERE " + KEY_METHODID +" = " + methodId);
  }
}
