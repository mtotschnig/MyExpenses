/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.totschnig.myexpenses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
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
    public static final String KEY_MAINCATID = "main_cat_id";
    public static final String KEY_SUBCATID = "sub_cat_id";

    private static final String TAG = "ExpensesDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "expenses";
    private static final int DATABASE_VERSION = 7;
    
    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE  +  "(_id integer primary key autoincrement, "
                    + "comment text not null, date DATETIME not null, amount float not null, "
                    + "main_cat_id integer, sub_cat_id integer);";
    // Table definition reflects format of Grisbis categories
   private static final String CATEGORIES_CREATE =
	   		"create table categories (_id integer, label text not null, parent_id integer, "
	   			+ "PRIMARY KEY (_id,parent_id));";


    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
            db.execSQL(CATEGORIES_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            db.execSQL("DROP TABLE IF EXISTS categories");
            onCreate(db);
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
     * Open the notes database. If it cannot be opened, try to create a new
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
     * Create a new note using the date, amount and comment provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param date the date of the expense
     * @param amount the amount of the expense
     * @param comment the comment describing the expense
     * @return rowId or -1 if failed
     */
    public long createExpense(String date, String amount, String comment,String main_cat_id,String sub_cat_id) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_COMMENT, comment);
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_AMOUNT, amount);
        initialValues.put(KEY_MAINCATID, main_cat_id);
        initialValues.put(KEY_SUBCATID, sub_cat_id);
        
        return mDb.insert(DATABASE_TABLE, null, initialValues);
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
    public void deleteAll() {
    	mDb.execSQL("DELETE from expenses");
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllExpenses() {

        return mDb.query(DATABASE_TABLE + " LEFT JOIN categories as main on (main_cat_id = main._id and main.parent_id is null) " +
        			"LEFT JOIN categories as sub on (main_cat_id = sub.parent_id and sub_cat_id = sub._id)",
        		new String[] {DATABASE_TABLE+"."+KEY_ROWID,KEY_DATE,KEY_AMOUNT, KEY_COMMENT, KEY_MAINCATID, KEY_SUBCATID,"main.label||' : '||sub.label as label"}, 
        		null, null, null, null, KEY_DATE);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchExpense(long rowId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE + " LEFT JOIN categories on (main_cat_id = parent_id and sub_cat_id = categories._id)", new String[] {DATABASE_TABLE+"."+KEY_ROWID,
                        KEY_DATE,KEY_AMOUNT,KEY_COMMENT, KEY_MAINCATID, KEY_SUBCATID, "label"}, DATABASE_TABLE+"."+KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the note using the details provided. The expense to be updated is
     * specified using the rowId, and it is altered to use the date, amount and comment
     * values passed in
     * 
     * @param rowId id of note to update
     * @param date value to set 
     * @param amount value to set
     * @param comment value to set
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateExpense(long rowId, String date, String amount, String comment,String main_cat_id,String sub_cat_id) {
        ContentValues args = new ContentValues();
        args.put(KEY_DATE, date);
        args.put(KEY_AMOUNT, amount);
        args.put(KEY_COMMENT, comment);
        args.put(KEY_MAINCATID, main_cat_id);
        args.put(KEY_SUBCATID, sub_cat_id);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    public float getSum() {
    	Cursor mCursor = mDb.rawQuery("select sum(" + KEY_AMOUNT + ") from " + DATABASE_TABLE, null);
    	mCursor.moveToFirst();
    	return mCursor.getFloat(0);
    }
    
    //Categories
    public long createCategory(String label, String id, String parent_id) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("label", label);
        initialValues.put("_id", id);
        initialValues.put("parent_id", parent_id);

        return mDb.insert("categories", null, initialValues);
    }
    
    public Cursor fetchMainCategories() {
        return mDb.query("categories",
        		new String[] {KEY_ROWID, "label"},
        		"parent_id is null",
        		null,
        		null,
        		null,
        		null
        );
    }
    public Cursor fetchSubCategories(String parent_id) {
    	 return mDb.query("categories", new String[] {KEY_ROWID,
         "label"}, "parent_id = " + parent_id, null, null, null, null);
    }
    public int getCategoriesCount() {
    	return (int) mDb.compileStatement("select count(_id) from categories").simpleQueryForLong();
    }
}