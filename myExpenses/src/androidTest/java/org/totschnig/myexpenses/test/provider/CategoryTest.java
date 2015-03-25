/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses.test.provider;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

/*
 */
/**
 * This class tests the content provider for the Note Pad sample application.
 *
 * To learn how to run an entire test package or one of its classes, please see
 * "Testing in Eclipse, with ADT" or "Testing in Other IDEs" in the Developer Guide.
 */
public class CategoryTest extends ProviderTestCase2<TransactionProvider> {

    // Contains a reference to the mocked content resolver for the provider under test.
    private MockContentResolver mMockResolver;

    // Contains an SQLite database, used as test data
    private SQLiteDatabase mDb;

    // Contains the test data, as an array of NoteInfo instances.
    private CategoryInfo[] TEST_CATEGORIES = new CategoryInfo[4];
    private Long[] testIds = new Long[4];
 
    /*
     * Constructor for the test case class.
     * Calls the super constructor with the class name of the provider under test and the
     * authority name of the provider.
     */
    public CategoryTest() {
        super(TransactionProvider.class,TransactionProvider.AUTHORITY);
    }

    /*
     * Sets up the test environment before each test method. Creates a mock content resolver,
     * gets the provider under test, and creates a new database for the provider.
     */
    @Override
    protected void setUp() throws Exception {
        // Calls the base class implementation of this method.
        super.setUp();

        // Gets the resolver for this test.
        mMockResolver = getMockContentResolver();

        /*
         * Gets a handle to the database underlying the provider. Gets the provider instance
         * created in super.setUp(), gets the DatabaseOpenHelper for the provider, and gets
         * a database object from the helper.
         */
        mDb = getProvider().getOpenHelperForTest().getWritableDatabase();
    }

    /*
     *  This method is called after each test method, to clean up the current fixture. Since
     *  this sample test case runs in an isolated context, no cleanup is necessary.
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Sets up test data.
     * The test data is in an SQL database. It is created in setUp() without any data,
     * and populated in insertData if necessary.
     */
    private void insertData() {

      TEST_CATEGORIES[0] = new CategoryInfo("Main 1",null);
      testIds[0] = mDb.insertOrThrow(DatabaseConstants.TABLE_CATEGORIES, null, TEST_CATEGORIES[0].getContentValues());
      TEST_CATEGORIES[1] = new CategoryInfo("Main 2", null);
      TEST_CATEGORIES[2] = new CategoryInfo("Sub 1", testIds[0]);
      TEST_CATEGORIES[3] = new CategoryInfo("Sub 2", testIds[0]);

        // Sets up test data
        for (int index = 1; index < TEST_CATEGORIES.length; index++) {
            
            // Adds a record to the database.
            testIds[index] = mDb.insertOrThrow(
                DatabaseConstants.TABLE_CATEGORIES,             // the table name for the insert
                null,      // column set to null if empty values map
                TEST_CATEGORIES[index].getContentValues()  // the values map to insert
            );
        }
    }


    /*
     * Tests the provider's public API for querying data in the table, using the URI for
     * a dataset of records.
     */
    public void testQueriesOnCategoriesUri() {
        // Defines a projection of column names to return for a query
        final String[] TEST_PROJECTION = {
            DatabaseConstants.KEY_LABEL,DatabaseConstants.KEY_PARENTID
        };

        // Defines a selection column for the query. When the selection columns are passed
        // to the query, the selection arguments replace the placeholders.
        final String LABEL_SELECTION = DatabaseConstants.KEY_LABEL + " = " + "?";

        // Defines the selection columns for a query.
        final String SELECTION_COLUMNS =
        LABEL_SELECTION + " OR " + LABEL_SELECTION + " OR " + LABEL_SELECTION;

         // Defines the arguments for the selection columns.
        final String[] SELECTION_ARGS = { "Main 1", "Main 2", "Sub 1" };
        final String SORT_ORDER = DatabaseConstants.KEY_LABEL + " ASC";

        // Query subtest 1.
        // If there are no records in the table, the returned cursor from a query should be empty.
        Cursor cursor = mMockResolver.query(
            TransactionProvider.CATEGORIES_URI,  // the URI for the main data table
            null,                       // no projection, get all columns
            null,                       // no selection criteria, get all records
            null,                       // no selection arguments
            null                        // use default sort order
        );

         // Asserts that the returned cursor contains no records
        assertEquals(0, cursor.getCount());

         // Query subtest 2.
         // If the table contains records, the returned cursor from a query should contain records.

        // Inserts the test data into the provider's underlying data source
        insertData();

        // Gets all the columns for all the rows in the table
        cursor = mMockResolver.query(
            TransactionProvider.CATEGORIES_URI,  // the URI for the main data table
            null,                       // no projection, get all columns
            null,                       // no selection criteria, get all records
            null,                       // no selection arguments
            null                        // use default sort order
        );

        // Asserts that the returned cursor contains the same number of rows as the size of the
        // test data array.
        assertEquals(TEST_CATEGORIES.length, cursor.getCount());

        // Query subtest 3.
        // A query that uses a projection should return a cursor with the same number of columns
        // as the projection, with the same names, in the same order.
        Cursor projectionCursor = mMockResolver.query(
            TransactionProvider.CATEGORIES_URI,  // the URI for the main data table
              TEST_PROJECTION,            // get the title, note, and mod date columns
              null,                       // no selection columns, get all the records
              null,                       // no selection criteria
              null                        // use default the sort order
        );

        // Asserts that the number of columns in the cursor is the same as in the projection
        assertEquals(TEST_PROJECTION.length, projectionCursor.getColumnCount());

        // Asserts that the names of the columns in the cursor and in the projection are the same.
        // This also verifies that the names are in the same order.
        assertEquals(TEST_PROJECTION[0], projectionCursor.getColumnName(0));
        assertEquals(TEST_PROJECTION[1], projectionCursor.getColumnName(1));

        // Query subtest 4
        // A query that uses selection criteria should return only those rows that match the
        // criteria. Use a projection so that it's easy to get the data in a particular column.
        projectionCursor = mMockResolver.query(
            TransactionProvider.CATEGORIES_URI, // the URI for the main data table
            TEST_PROJECTION,           // get the title, note, and mod date columns
            SELECTION_COLUMNS,         // select on the title column
            SELECTION_ARGS,            // select titles "Note0", "Note1", or "Note5"
            SORT_ORDER                 // sort ascending on the label column
        );

        // Asserts that the cursor has the same number of rows as the number of selection arguments
        assertEquals(SELECTION_ARGS.length, projectionCursor.getCount());

        int index = 0;

        while (projectionCursor.moveToNext()) {

            // Asserts that the selection argument at the current index matches the value of
            // the label column (column 0) in the current record of the cursor
            assertEquals(SELECTION_ARGS[index], projectionCursor.getString(0));

            index++;
        }

        // Asserts that the index pointer is now the same as the number of selection arguments, so
        // that the number of arguments tested is exactly the same as the number of rows returned.
        assertEquals(SELECTION_ARGS.length, index);
    }

    /*
     * Tests queries against the provider, using the note id URI. This URI encodes a single
     * record ID. The provider should only return 0 or 1 record.
     */
    public void testQueriesOnCategoryIdUri() {
      // Defines the selection column for a query. The "?" is replaced by entries in the
      // selection argument array
      final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

      // Defines the argument for the selection column.
      final String[] SELECTION_ARGS = { "Main 1" };

      // Creates a projection includes the note id column, so that note id can be retrieved.
      final String[] CATEGORY_ID_PROJECTION = {
          DatabaseConstants.KEY_ROWID,                 // The Notes class extends BaseColumns,
                                              // which includes _ID as the column name for the
                                              // record's id in the data model
          DatabaseConstants.KEY_LABEL};  // The note's title

      // Query subtest 1.
      // Tests that a query against an empty table returns null.

      // Constructs a URI that matches the provider's notes id URI pattern, using an arbitrary
      // value of 1 as the note ID.
      Uri categoryIdUri = ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, 1);

      // Queries the table with the notes ID URI. This should return an empty cursor.
      Cursor cursor = mMockResolver.query(
          categoryIdUri, // URI pointing to a single record
          null,      // no projection, get all the columns for each record
          null,      // no selection criteria, get all the records in the table
          null,      // no need for selection arguments
          null       // default sort, by ascending title
      );

      // Asserts that the cursor is null.
      assertEquals(0,cursor.getCount());

      // Query subtest 2.
      // Tests that a query against a table containing records returns a single record whose ID
      // is the one requested in the URI provided.

      // Inserts the test data into the provider's underlying data source.
      insertData();

      // Queries the table using the URI for the full table.
      cursor = mMockResolver.query(
          TransactionProvider.CATEGORIES_URI, // the base URI for the table
          CATEGORY_ID_PROJECTION,        // returns the ID and title columns of rows
          SELECTION_COLUMNS,         // select based on the title column
          SELECTION_ARGS,            // select title of "Note1"
          null                 // sort order returned is by title, ascending
      );

      // Asserts that the cursor contains only one row.
      assertEquals(1, cursor.getCount());

      // Moves to the cursor's first row, and asserts that this did not fail.
      assertTrue(cursor.moveToFirst());

      // Saves the record's note ID.
      int inputCategoryId = cursor.getInt(0);

      // Builds a URI based on the provider's content ID URI base and the saved note ID.
      categoryIdUri = ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, inputCategoryId);

      // Queries the table using the content ID URI, which returns a single record with the
      // specified note ID, matching the selection criteria provided.
      cursor = mMockResolver.query(categoryIdUri, // the URI for a single note
          CATEGORY_ID_PROJECTION,                 // same projection, get ID and title columns
          SELECTION_COLUMNS,                  // same selection, based on title column
          SELECTION_ARGS,                     // same selection arguments, title = "Note1"
          null                          // same sort order returned, by title, ascending
      );

      // Asserts that the cursor contains only one row.
      assertEquals(1, cursor.getCount());

      // Moves to the cursor's first row, and asserts that this did not fail.
      assertTrue(cursor.moveToFirst());

      // Asserts that the note ID passed to the provider is the same as the note ID returned.
      assertEquals(inputCategoryId, cursor.getInt(0));
    }

    /*
     *  Tests inserts into the data model.
     */
    public void testInserts() {
        // Creates a new transaction instance
        CategoryInfo transaction = new CategoryInfo(
            "Main 3",null);

        // Insert subtest 1.
        // Inserts a row using the new category instance.
        // No assertion will be done. The insert() method either works or throws an Exception
        Uri rowUri = mMockResolver.insert(
            TransactionProvider.CATEGORIES_URI,  // the main table URI
            transaction.getContentValues()     // the map of values to insert as a new record
        );

        // Parses the returned URI to get the note ID of the new note. The ID is used in subtest 2.
        long categoryId = ContentUris.parseId(rowUri);

        // Does a full query on the table. Since insertData() hasn't yet been called, the
        // table should only contain the record just inserted.
        Cursor cursor = mMockResolver.query(
            TransactionProvider.CATEGORIES_URI, // the main table URI
            null,                      // no projection, return all the columns
            null,                      // no selection criteria, return all the rows in the model
            null,                      // no selection arguments
            null                       // default sort order
        );

        // Asserts that there should be only 1 record.
        assertEquals(1, cursor.getCount());

        // Moves to the first (and only) record in the cursor and asserts that this worked.
        assertTrue(cursor.moveToFirst());

        // Since no projection was used, get the column indexes of the returned columns
        int labelIndex = cursor.getColumnIndex(DatabaseConstants.KEY_LABEL);
        int parentIdIndex = cursor.getColumnIndex(DatabaseConstants.KEY_PARENTID);

        // Tests each column in the returned cursor against the data that was inserted, comparing
        // the field in the NoteInfo object to the data at the column index in the cursor.
        assertEquals(transaction.parentId, cursor.getString(parentIdIndex));
        assertEquals(transaction.label, cursor.getString(labelIndex));
        // Insert subtest 2.
        // Tests that we can't insert a record whose id value already exists.

        // Defines a ContentValues object so that the test can add a note ID to it.
        ContentValues values = transaction.getContentValues();

        // Adds the note ID retrieved in subtest 1 to the ContentValues object.
        values.put(DatabaseConstants.KEY_ROWID, categoryId);

        // Tries to insert this record into the table.
        try {
          rowUri = mMockResolver.insert(TransactionProvider.CATEGORIES_URI, values);
          fail("Expected insert failure for existing record but insert succeeded.");
        } catch (Exception e) {
          // succeeded, so do nothing.
        }
        //Insert subtest 3.
        //Test that we can't insert a record that links to a non-existing parent
        values.remove(DatabaseConstants.KEY_ROWID);
        values.put(DatabaseConstants.KEY_PARENTID, 100);
        try {
          rowUri = mMockResolver.insert(TransactionProvider.CATEGORIES_URI, values);
          fail("Expected insert failure for link to non-existing parent but insert succeeded.");
        } catch (Exception e) {
          // succeeded, so do nothing
        }
    }

    /*
     * Tests deletion fails on foreign keys.
     */
    public void testDeleteRespectsForeignKeys() {
        // Subtest 1.
        // Tries to delete a record from a data model that is empty.

        // Sets the selection column to "title"
        final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

        // Sets the selection argument for deleting main or sub category
        final String[] SELECTION_ARGS_MAIN = { "Main 1" };

        // Tries to delete rows matching the selection criteria from the data model.
        int rowsDeleted = mMockResolver.delete(
            TransactionProvider.CATEGORIES_URI, // the base URI of the table
            SELECTION_COLUMNS,         // select based on the title column
            SELECTION_ARGS_MAIN           // select title = "Note0"
        );

        // Assert that the deletion did not work. The number of deleted rows should be zero.
        assertEquals(0, rowsDeleted);

        // Subtest 2.
        // Tries to delete an existing record. Repeats the previous subtest, but inserts data first.

        // Inserts data into the model.
        insertData();

        // Uses the same parameters to try to delete the row with label "Main 1"
        try {
          rowsDeleted = mMockResolver.delete(
              TransactionProvider.CATEGORIES_URI, // the base URI of the table
              DatabaseConstants.KEY_LABEL + " = " + "?", 
              new String[] { "Main 1" } //this category is referenced by subcategories
          );
          fail("Expected foreign key to prevent main category from being deleted.");
        } catch (SQLiteConstraintException e) {
       // succeeded, so do nothing
        }
    }
    /*
     * Tests deletion succeeds if no foreign key constraint is violated.
     */
    public void testDeleteSucceeds() {
        // Subtest 1.
        // Tries to delete a record from a data model that is empty.

        // Sets the selection column to "title"
        final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " IN (?,?)";

        // Sets the selection argument for deleting main or sub category
        final String[] SELECTION_ARGS_MAIN = { "Main 1","Main 2" };
        final String[] SELECTION_ARGS_SUB = { "Sub 1", "Sub 2" };

        // Inserts data into the model.
        insertData();

        // Now delete sub 1 and sub2, this should succeed
        int rowsDeleted = mMockResolver.delete(
          TransactionProvider.CATEGORIES_URI, // the base URI of the table
          SELECTION_COLUMNS,         // same selection column, "title"
          SELECTION_ARGS_SUB             // same selection arguments, label "Main 1"
        );

        // The number of deleted rows should be 1.
        assertEquals(2, rowsDeleted);

        // Since sub has been deleted, now main is deletable
        rowsDeleted = mMockResolver.delete(
          TransactionProvider.CATEGORIES_URI, // the base URI of the table
          SELECTION_COLUMNS,         // same selection column, "title"
          SELECTION_ARGS_MAIN           // same selection arguments, label "Main 1"
        );

        // The number of deleted rows should be 1.
        assertEquals(2, rowsDeleted);

        // Tests that the record no longer exists. Tries to get it from the table, and
        // asserts that nothing was returned.

        // Queries the table with the same selection column and argument used to delete the row.
        Cursor cursor = mMockResolver.query(
            TransactionProvider.CATEGORIES_URI, // the base URI of the table
            null,                      // no projection, return all columns
            SELECTION_COLUMNS,         // select based on the title column
            SELECTION_ARGS_MAIN,            // select title = "Note0"
            null                       // use the default sort order
        );

        // Asserts that the cursor is empty since the record had already been deleted.
        assertEquals(0, cursor.getCount());
        
        // Queries the table with the same selection column and argument used to delete the row.
        cursor = mMockResolver.query(
            TransactionProvider.CATEGORIES_URI, // the base URI of the table
            null,                      // no projection, return all columns
            SELECTION_COLUMNS,         // select based on the title column
            SELECTION_ARGS_SUB,            // select title = "Note0"
            null                       // use the default sort order
        );

        // Asserts that the cursor is empty since the record had already been deleted.
        assertEquals(0, cursor.getCount());
    }

    /*
     * Tests updates to the data model.
     */
    public void testUpdates() {
        // Selection column for identifying a record in the data model.
        final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

        // Selection argument for the selection column.
        final String[] selectionArgs = { "Main 1" };

        // Defines a map of column names and values
        ContentValues values = new ContentValues();

        // Subtest 1.
        // Tries to update a record in an empty table.

        // Sets up the update by putting the "note" column and a value into the values map.
        values.put(DatabaseConstants.KEY_LABEL, "Testing an update with this string");

        // Tries to update the table
        int rowsUpdated = mMockResolver.update(
            TransactionProvider.CATEGORIES_URI,  // the URI of the data table
            values,                     // a map of the updates to do (column title and value)
            SELECTION_COLUMNS,           // select based on the title column
            selectionArgs               // select "title = Note1"
        );

        // Asserts that no rows were updated.
        assertEquals(0, rowsUpdated);

        // Subtest 2.
        // Builds the table, and then tries the update again using the same arguments.

        // Inserts data into the model.
        insertData();

        //  Does the update again, using the same arguments as in subtest 1.
        rowsUpdated = mMockResolver.update(
            TransactionProvider.CATEGORIES_URI,   // The URI of the data table
            values,                      // the same map of updates
            SELECTION_COLUMNS,            // same selection, based on the title column
            selectionArgs                // same selection argument, to select "title = Note1"
        );

        // Asserts that only one row was updated. The selection criteria evaluated to
        // "title = Note1", and the test data should only contain one row that matches that.
        assertEquals(1, rowsUpdated);
    }
    public void testUniqueConstraintsCreateMain() {
      insertData();
      // Creates a new transaction instance with an already existing label
      CategoryInfo category = new CategoryInfo(
          "Main 1",null);

      try {
        mMockResolver.insert(
            TransactionProvider.CATEGORIES_URI,  // the main table URI
            category.getContentValues()     // the map of values to insert as a new record
        );
        fail("Expected unique constraint to prevent main category from being created.");
      } catch (SQLiteConstraintException e) {
        // succeeded, so do nothing
      }
    }
    public void testUniqueConstraintsCreateSub() {
      insertData();
      // Creates a new transaction instance with an already existing label
      CategoryInfo category = new CategoryInfo(
          "Sub 1",testIds[0]);

      try {
        mMockResolver.insert(
            TransactionProvider.CATEGORIES_URI,  // the main table URI
            category.getContentValues()     // the map of values to insert as a new record
        );
        fail("Expected unique constraint to prevent sub category from being created.");
      } catch (SQLiteConstraintException e) {
        // succeeded, so do nothing
      } 
    }
    public void testUniqueConstraintsUpdateMain() {
      insertData();

      //we try to set the name of Main 2 to Main 1
      try {
        ContentValues args = new ContentValues();
        args.put(KEY_LABEL, "Main 1");
        mMockResolver.update(
            TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(testIds[1])).build(),
            args,null,null
        );
        fail("Expected unique constraint to prevent main category from being updated.");
      } catch (SQLiteConstraintException e) {
        // succeeded, so do nothing
      }
    }
    public void testUniqueConstraintsUpdateSub() {
      insertData();
      try {
        ContentValues args = new ContentValues();
        args.put(KEY_LABEL, "Sub 1");
        mMockResolver.update(
            TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(testIds[3])).build(),
            args,null,null
        );
        fail("Expected unique constraint to prevent sub category from being created.");
      } catch (SQLiteConstraintException e) {
        // succeeded, so do nothing
      }
    }
}
