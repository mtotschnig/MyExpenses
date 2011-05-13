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
    public static final String KEY_CATID = "cat_id";

    private static final String TAG = "ExpensesDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "expenses";
    private static final int DATABASE_VERSION = 10;
    
    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE  +  "(_id integer primary key autoincrement, "
                    + "comment text not null, date DATETIME not null, amount float not null, "
                    + "cat_id integer);";
    // Table definition reflects format of Grisbis categories
    //Main Categories have parent_id null
   private static final String CATEGORIES_CREATE =
	   		"create table categories (_id integer primary key autoincrement, label text not null, parent_id integer, usages integer default 0);";
   private static final String JOIN_EXP = DATABASE_TABLE + " LEFT JOIN categories on (cat_id = categories._id)";
   //private static final String CAT_LABEL_CONCAT = "main.label||' : '||coalesce(sub.label,'') as full_label";
   



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
                    + newVersion + ".");
            switch (newVersion) {
            	case 10:
            		db.execSQL("DROP TABLE expenses;");
            		db.execSQL("DROP TABLE categories;");
            		db.execSQL(DATABASE_CREATE);
            		db.execSQL(CATEGORIES_CREATE);
            		break;
            	default:
            		break;
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
     * Create a new expense using the date, amount and comment provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param date the date of the expense
     * @param amount the amount of the expense
     * @param comment the comment describing the expense
     * @return rowId or -1 if failed
     */
    public long createExpense(String date, String amount, String comment,String cat_id) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_COMMENT, comment);
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_AMOUNT, amount);
        initialValues.put(KEY_CATID, cat_id);
        
        long _id = mDb.insert(DATABASE_TABLE, null, initialValues);
        recordCatUsage(cat_id);
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
    public void deleteAll() {
    	mDb.execSQL("DELETE from expenses");
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllExpenses() {

        return mDb.query(JOIN_EXP,
        		new String[] {DATABASE_TABLE+"."+KEY_ROWID,KEY_DATE,KEY_AMOUNT, KEY_COMMENT, KEY_CATID,"label"}, 
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

                mDb.query(JOIN_EXP,
                		new String[] {DATABASE_TABLE+"."+KEY_ROWID,KEY_DATE,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,"label"},
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
    public void updateExpense(long rowId, String date, String amount, String comment,String cat_id) {
        ContentValues args = new ContentValues();
        args.put(KEY_DATE, date);
        args.put(KEY_AMOUNT, amount);
        args.put(KEY_COMMENT, comment);
        args.put(KEY_CATID, cat_id);

        mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null);
        recordCatUsage(cat_id);
    }
    public float getSum() {
    	Cursor mCursor = mDb.rawQuery("select sum(" + KEY_AMOUNT + ") from " + DATABASE_TABLE, null);
    	mCursor.moveToFirst();
    	float result = mCursor.getFloat(0);
    	mCursor.close();
    	return result;
    }
    public void recordCatUsage(String cat_id) {
    	if (cat_id != null) {
    		mDb.execSQL("update categories set usages = usages +1 where _id = " + cat_id + " or _id = (select parent_id from categories where _id = " +cat_id + ")");
    	}
    }
    
    //Categories
    public long createCategory(String label, String parent_id) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("label", label);
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
        		"usages DESC"
        );
    }
    public Cursor fetchSubCategories(String parent_id) {
    	 return mDb.query("categories", new String[] {KEY_ROWID,
         "label"}, "parent_id = " + parent_id, null, null, null, "usages DESC");
    }
/*    public int getCategoriesCount() {
    	return (int) mDb.compileStatement("select count(_id) from categories").simpleQueryForLong();
    }*/
}