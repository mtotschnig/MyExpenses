/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class ExpensesDbAdapter {

  public static final String KEY_DATE = "date";
  public static final String KEY_AMOUNT = "amount";
  public static final String KEY_COMMENT = "comment";
  public static final String KEY_ROWID = "_id";
  public static final String KEY_CATID = "cat_id";
  public static final String KEY_ACCOUNTID = "account_id";
  public static final String KEY_TRANSFER_PEER = "transfer_peer";
  public static final String KEY_PAYEE = "payee";

  private static final String TAG = "ExpensesDbAdapter";
  private DatabaseHelper mDbHelper;
  private SQLiteDatabase mDb;

  /**
   * Database creation sql statement
   */
  private static final String DATABASE_NAME = "data";
  private static final String DATABASE_TABLE = "expenses";
  private static final int DATABASE_VERSION = 19;

  private static final String DATABASE_CREATE =
    "create table " + DATABASE_TABLE  +  "(_id integer primary key autoincrement, "
    + "comment text not null, date DATETIME not null, amount float not null, "
    + "cat_id integer, account_id integer, payee text,transfer_peer integer default null);";
  private static final String ACCOUNTS_CREATE = 
    "create table accounts (_id integer primary key autoincrement, label text not null, opening_balance float, description text, currency text not null);";
  // Table definition reflects format of Grisbis categories
  //Main Categories have parent_id null
  private static final String CATEGORIES_CREATE =
    "create table categories (_id integer primary key autoincrement, label text not null, parent_id integer not null default 0, usages integer default 0, unique (label,parent_id));";
  //private static final String JOIN_EXP = DATABASE_TABLE + " LEFT JOIN categories cat on (cat_id = cat._id)";
  private static final String FULL_LABEL = "case when \n" + 
  		"  transfer_peer\n" + 
  		"     then\n" + 
  		"  '=>' || (select label from accounts where _id = cat_id)\n" + 
  		"     else\n" + 
  		"  case when \n" + 
  		"    (select parent_id from categories where _id = cat_id)\n" + 
  		"       then\n" + 
  		"    (select label from categories where _id = (select parent_id from categories where _id = cat_id)) || ' : ' || (select label from categories where _id = cat_id)\n" + 
  		"       else\n" + 
  		"    (select label from categories where _id = cat_id)\n" + 
  		"  end\n" + 
  		"end as label";
  private static final String SHORT_LABEL = "case when \n" + 
  "  transfer_peer\n" + 
  "     then\n" + 
  "  '=>' || (select label from accounts where _id = cat_id)\n" + 
  "     else\n" + 
  "    (select label from categories where _id = cat_id)\n" + 
  "end as label";

  private static final String PAYEE_CREATE = 
    "create table payee (_id integer primary key autoincrement, name text unique not null);";

  private final Context mCtx;

  private static class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

      db.execSQL(DATABASE_CREATE);
      db.execSQL(CATEGORIES_CREATE);
      db.execSQL(ACCOUNTS_CREATE);
      db.execSQL(PAYEE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
          + newVersion + ".");
      if (oldVersion < 17) {
        db.execSQL("drop table accounts");
        db.execSQL(ACCOUNTS_CREATE);
        //db.execSQL("alter table expenses add column account_id integer");
      }
      if (oldVersion < 18) {
        db.execSQL(PAYEE_CREATE);
        db.execSQL("alter table expenses add column payee text");
      }
      if (oldVersion < 19) {
        db.execSQL("alter table expenses add column transfer_peer text");
      }
    }
  }

  /**
   * Constructor - takes the context to allow the database to be
   * opened/created
   * 
   * @param ctx the Context within which to work
   */
  public ExpensesDbAdapter(Context ctx) {
    this.mCtx = ctx;
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
    mDbHelper = new DatabaseHelper(mCtx);
    mDb = mDbHelper.getWritableDatabase();
    return this;
  }

  public void close() {
    mDbHelper.close();
  }

  /**
   * TRANSACTIONS
   */

  /**
   * Create a new expense using the date, amount and comment provided. If the note is
   * successfully created return the new rowId for that note, otherwise return
   * a -1 to indicate failure.
   * 
   * @param date the date of the expense
   * @param amount the amount of the expense
   * @param comment the comment describing the expense
   * @return rowId or -1 if failed
   */
  public long createExpense(String date, Float amount, String comment,int cat_id,int account_id, String payee) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_DATE, date);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_CATID, cat_id);
    initialValues.put(KEY_ACCOUNTID, account_id);
    initialValues.put(KEY_PAYEE, payee);
    long _id = mDb.insert(DATABASE_TABLE, null, initialValues);
    incrCategoryUsage(cat_id);
    return _id;
  }
  
  /**
   * Create a new transfer pair of expense using the date, amount and comment provided. 
   * 
   * 
   * @param date the date of the expense
   * @param amount the amount of the expense
   * @param comment the comment describing the expense
   * @return rowId or -1 if failed
   */
  public long createTransfer(String date, Float amount, String comment,int account_id, int account_peer) {
    //the id of the account is stored in KEY_CATID, the id of the peer transaction is stored in KEY_TRANSFER_PEER
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_DATE, date);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_CATID, account_peer);
    initialValues.put(KEY_ACCOUNTID, account_id);
    long _id = mDb.insert(DATABASE_TABLE, null, initialValues);
    initialValues.put(KEY_AMOUNT, 0 - amount);
    initialValues.put(KEY_CATID, account_id);
    initialValues.put(KEY_ACCOUNTID, account_peer);
    initialValues.put(KEY_TRANSFER_PEER,_id);
    long transfer_peer = mDb.insert(DATABASE_TABLE, null, initialValues);
    ContentValues args = new ContentValues();
    args.put(KEY_TRANSFER_PEER,transfer_peer);
    mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + _id, null);
    return _id;
  }

  /**
   * Delete the note with the given rowId
   * 
   * @param rowId id of note to delete
   * @return true if deleted, false otherwise
   */
  public boolean deleteExpense(long rowId) {

    return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
  }
  public boolean deleteTransfer(long rowId, int transfer_peer) {
    return mDb.delete(DATABASE_TABLE, KEY_ROWID + " in (" + rowId + "," + transfer_peer + ")", null) > 0;
  }
  public void deleteExpenseAll(int account_id ) {
    mDb.execSQL("DELETE from expenses WHERE account_id = " + account_id);
  }

  /**
   * Return a Cursor over the list of all expenses in the database
   * exposes the full label which concatenate main and sub label if appropriate
   * @return Cursor over all expenses
   */
  public Cursor fetchExpenseAll(int account_id) {

    return mDb.query(DATABASE_TABLE,
        new String[] {KEY_ROWID,KEY_DATE,KEY_AMOUNT, KEY_COMMENT, KEY_CATID,FULL_LABEL,KEY_PAYEE,KEY_TRANSFER_PEER}, 
        "account_id = " + account_id, null, null, null, KEY_DATE);
  }

  /**
   * Return a Cursor positioned at the note that matches the given rowId
   * exposes just the label for the linked category
   * @param rowId id of note to retrieve
   * @return Cursor positioned to matching note, if found
   * @throws SQLException if note could not be found/retrieved
   */
  public Cursor fetchExpense(long rowId) throws SQLException {

    Cursor mCursor =

      mDb.query(DATABASE_TABLE,
          new String[] {KEY_ROWID,KEY_DATE,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,SHORT_LABEL,KEY_PAYEE,KEY_ACCOUNTID},
          DATABASE_TABLE+"."+KEY_ROWID + "=" + rowId,
          null, null, null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;

  }

  /**
   * Update the expense using the details provided. The expense to be updated is
   * specified using the rowId, and it is altered to use the date, amount and comment
   * values passed in
   * 
   * @param rowId id of note to update
   * @param date value to set 
   * @param amount value to set
   * @param comment value to set
   * @return true if the note was successfully updated, false otherwise
   */
  public void updateExpense(long rowId, String date, float amount, String comment,int cat_id,String payee) {
    ContentValues args = new ContentValues();
    args.put(KEY_DATE, date);
    args.put(KEY_AMOUNT, amount);
    args.put(KEY_COMMENT, comment);
    args.put(KEY_CATID, cat_id);
    args.put(KEY_PAYEE, payee);

    mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null);
    incrCategoryUsage(cat_id);
  }
  public float getExpenseSum(int account_id) {
    Cursor mCursor = mDb.rawQuery("select sum(" + KEY_AMOUNT + ") from " + DATABASE_TABLE +  " WHERE account_id = " + account_id, null);
    mCursor.moveToFirst();
    float result = mCursor.getFloat(0);
    mCursor.close();
    return result;
  }
  public int getExpenseCount(int cat_id) {
    Cursor mCursor = mDb.rawQuery("select count(*) from " + DATABASE_TABLE +  " WHERE cat_id = " + cat_id, null);
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }
  
  public void updateExpenseAccountAll(long account_id) {
    ContentValues args = new ContentValues();
    args.put("account_id",account_id);
    mDb.update(DATABASE_TABLE,args,null,null);
  }

  /**
   * CATEGORIES
   */

  public long createCategory(String label, String parent_id) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("parent_id", parent_id);

    //should return -1 if unique constraint is not met  
    return mDb.insert("categories", null, initialValues);
  }
  public long updateCategoryLabel(String label, String cat_id) {
    ContentValues args = new ContentValues();
    args.put("label", label);

    //should return -1 if unique constraint is not met
    try {
      return mDb.update("categories", args, "_id = " + cat_id, null);
    } catch (SQLiteConstraintException e) {
      return -1;
    }
  }
  
  public void incrCategoryUsage(int cat_id) {
      mDb.execSQL("update categories set usages = usages +1 where _id = " + cat_id + " or _id = (select parent_id from categories where _id = " +cat_id + ")");
  }
  
  public long getCategoryId(String label, String parent_id) {
    Cursor mCursor = mDb.rawQuery("select _id from categories where parent_id = ? and label = ?",  new String[] {parent_id, label});
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
  
  public boolean deleteCategory(int cat_id) {
    return mDb.delete("categories", KEY_ROWID + "=" + cat_id, null) > 0;
  }

  public Cursor fetchCategoryMain() {
    return mDb.query("categories",
        new String[] {KEY_ROWID, "label"},
        "parent_id = 0",
        null,
        null,
        null,
        "usages DESC"
    );
  }
  //this methods appends a special "account transfer" category into
  //the categories
  public Cursor fetchCategoryMainUnionTransfer() {
    return mDb.rawQuery(
        "SELECT _id,label,usages FROM categories WHERE parent_id = 0 UNION " +
        "SELECT -1,\"__TRANSFER__\",-1 WHERE (SELECT count(*) FROM accounts)>1 " +
        "ORDER BY usages DESC;n", null);
  }
  
  
  public int getCategoryCountSub(int parent_id){
    Cursor mCursor = mDb.rawQuery("select count(*) from categories where parent_id = " + parent_id, null);
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }
  public Cursor fetchCategorySub(String parent_id) {
    return mDb.query("categories", new String[] {KEY_ROWID,
    "label"}, "parent_id = " + parent_id, null, null, null, "usages DESC");
  }
  /*    public int getCategoriesCount() {
      return (int) mDb.compileStatement("select count(_id) from categories").simpleQueryForLong();
    }*/



  /**
   * ACCOUNTS
   */
  
  public void updateAccountCurrency(String account_id, String newStr) {
    mDb.execSQL("update accounts set currency = '" + newStr + "' where _id = " + account_id);
  }
  

  public long createAccount(String label, String opening_balance, String description, String currency) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("opening_balance",opening_balance);
    initialValues.put("description",description);
    initialValues.put("currency",currency);
    return mDb.insert("accounts", null, initialValues);
  }
  public void updateAccount(long rowId, String label, String opening_balance, String description, String currency) {
    ContentValues args = new ContentValues();
    args.put("label", label);
    args.put("opening_balance",opening_balance);
    args.put("description",description);
    args.put("currency",currency);
    mDb.update("accounts", args, KEY_ROWID + "=" + rowId, null);
  }
  public Cursor fetchAccountAll() {
    return mDb.query("accounts",
        new String[] {"accounts."+KEY_ROWID,"label","description","opening_balance","currency"}, 
        null, null, null, null, null);
  }
  //fetches all accounts except one
  public Cursor fetchAccountOther(int account_id) {
    return mDb.query("accounts",
        new String[] {"accounts."+KEY_ROWID,"label"}, 
        KEY_ROWID + "!=" + account_id,
        null, null, null, null);
  }
  
  public Cursor fetchAccount(long rowId) throws SQLException {
    Cursor mCursor =
      mDb.query("accounts",
          new String[] {"label","description","opening_balance","currency"},
          KEY_ROWID + "=" + rowId,
          null, null, null,null);
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;
  }
  public void updateAccountOpeningBalance(long account_id,float opening_balance) {
    ContentValues args = new ContentValues();
    args.put("opening_balance",opening_balance);
    mDb.update("accounts",args, KEY_ROWID + "=" + account_id,null);
  }

  public boolean deleteAccount(long rowId) {
    return mDb.delete("accounts", KEY_ROWID + "=" + rowId, null) > 0;
  }
  
  /**
   * PAYEES
   */
  
  public void createPayeeOrIgnore(String name) {
    mDb.execSQL("INSERT OR IGNORE INTO payee(name) values('" + name + "');");
    return;
  }
  
  public Cursor fetchPayeeAll() {
    return mDb.query("payee",
        new String[] {"name"}, 
        null, null, null, null, null);
  }
  

}