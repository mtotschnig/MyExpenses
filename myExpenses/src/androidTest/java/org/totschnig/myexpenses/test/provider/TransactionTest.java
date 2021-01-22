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
 * MT: adapted to MyExpenses TransactionProvider from Android Notes Tutorial
 */

package org.totschnig.myexpenses.test.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.testutils.BaseDbTest;

import java.util.Date;

public class TransactionTest extends BaseDbTest {

  // Contains the test data, as an array of TransactionInfo instances.
  private TransactionInfo[] TEST_TRANSACTIONS = new TransactionInfo[3];
  private String PAYEE_NAME = "N.N";
  private long testAccountId;
  private long payeeId;


  @Override
  protected void setUp() throws Exception {
    super.setUp();

    AccountInfo testAccount = new AccountInfo("Test account", AccountType.CASH, 0);
    testAccountId = mDb.insertOrThrow(DatabaseConstants.TABLE_ACCOUNTS, null, testAccount.getContentValues());
    payeeId = mDb.insertOrThrow(DatabaseConstants.TABLE_PAYEES, null, new PayeeInfo(PAYEE_NAME).getContentValues());
  }

  /**
   * Sets up test data.
   * The test data is in an SQL database. It is created in setUp() without any data,
   * and populated in insertData if necessary.
   */
  private void insertData() {

    TEST_TRANSACTIONS[0] = new TransactionInfo("Transaction 0", new Date(), 0, testAccountId, payeeId);
    TEST_TRANSACTIONS[1] = new TransactionInfo("Transaction 1", new Date(), 100, testAccountId, payeeId);
    TEST_TRANSACTIONS[2] = new TransactionInfo("Transaction 2", new Date(), -100, testAccountId, payeeId);

    // Sets up test data
    for (TransactionInfo TEST_TRANSACTION : TEST_TRANSACTIONS) {

      // Adds a record to the database.
      mDb.insertOrThrow(
          DatabaseConstants.TABLE_TRANSACTIONS,             // the table name for the insert
          null,      // column set to null if empty values map
          TEST_TRANSACTION.getContentValues()  // the values map to insert
      );
    }
  }


  /*
   * Tests the provider's public API for querying data in the table, using the URI for
   * a dataset of records.
   */
  public void testQueriesOnTransactionUri() {
    // Defines a projection of column names to return for a query
    final String[] TEST_PROJECTION = {
        DatabaseConstants.KEY_COMMENT, DatabaseConstants.KEY_DATE, DatabaseConstants.KEY_PAYEEID
    };

    // Defines a selection column for the query. When the selection columns are passed
    // to the query, the selection arguments replace the placeholders.
    final String COMMENT_SELECTION = DatabaseConstants.KEY_COMMENT + " = " + "?";

    // Defines the selection columns for a query.
    final String SELECTION_COLUMNS =
        COMMENT_SELECTION + " OR " + COMMENT_SELECTION + " OR " + COMMENT_SELECTION;

    // Defines the arguments for the selection columns.
    final String[] SELECTION_ARGS = {"Transaction 0", "Transaction 1", "Transaction 2"};

    // Defines a query sort order
    final String SORT_ORDER = DatabaseConstants.KEY_COMMENT + " ASC";

    // Query subtest 1.
    // If there are no records in the table, the returned cursor from a query should be empty.
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI,  // the URI for the main data table
        null,                       // no projection, get all columns
        null,                       // no selection criteria, get all records
        null,                       // no selection arguments
        null                        // use default sort order
    );
    assert cursor != null;

    // Asserts that the returned cursor contains no records
    assertEquals(0, cursor.getCount());

    // Query subtest 2.
    // If the table contains records, the returned cursor from a query should contain records.

    // Inserts the test data into the provider's underlying data source
    insertData();
    cursor.close();

    // Gets all the columns for all the rows in the table
    cursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI,  // the URI for the main data table
        null,                       // no projection, get all columns
        null,                       // no selection criteria, get all records
        null,                       // no selection arguments
        null                        // use default sort order
    );
    assert cursor != null;

    // Asserts that the returned cursor contains the same number of rows as the size of the
    // test data array.
    assertEquals(TEST_TRANSACTIONS.length, cursor.getCount());
    cursor.close();

    // Query subtest 3.
    // A query that uses a projection should return a cursor with the same number of columns
    // as the projection, with the same names, in the same order.
    Cursor projectionCursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI,  // the URI for the main data table
        TEST_PROJECTION,            // get the comment, date and payee_id
        null,                       // no selection columns, get all the records
        null,                       // no selection criteria
        null                        // use default the sort order
    );


    // Asserts that the number of columns in the cursor is the same as in the projection
    assert projectionCursor != null;
    assertEquals(TEST_PROJECTION.length, projectionCursor.getColumnCount());

    // Asserts that the names of the columns in the cursor and in the projection are the same.
    // This also verifies that the names are in the same order.
    assertEquals(TEST_PROJECTION[0], projectionCursor.getColumnName(0));
    assertEquals(TEST_PROJECTION[1], projectionCursor.getColumnName(1));
    assertEquals(TEST_PROJECTION[2], projectionCursor.getColumnName(2));
    projectionCursor.close();

    // Query subtest 4
    // A query that uses selection criteria should return only those rows that match the
    // criteria. Use a projection so that it's easy to get the data in a particular column.
    projectionCursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI, // the URI for the main data table
        TEST_PROJECTION,           //  get the comment, date and payee_id
        SELECTION_COLUMNS,         // select on the title column
        SELECTION_ARGS,            // select titles "Transaction 0", "Transaction 1", or "Transaction 2"
        SORT_ORDER                 // sort ascending on the title column
    );


    // Asserts that the cursor has the same number of rows as the number of selection arguments
    assert projectionCursor != null;
    assertEquals(SELECTION_ARGS.length, projectionCursor.getCount());

    int index = 0;

    while (projectionCursor.moveToNext()) {

      // Asserts that the selection argument at the current index matches the value of
      // the title column (column 0) in the current record of the cursor
      assertEquals(SELECTION_ARGS[index], projectionCursor.getString(0));

      index++;
    }

    // Asserts that the index pointer is now the same as the number of selection arguments, so
    // that the number of arguments tested is exactly the same as the number of rows returned.
    assertEquals(SELECTION_ARGS.length, index);
    projectionCursor.close();
  }

  /*
   * Tests queries against the provider, using the transaction id URI. This URI encodes a single
   * record ID. The provider should only return 0 or 1 record.
   */
  public void testQueriesOnTransactionIdUri() {
    // Defines the selection column for a query. The "?" is replaced by entries in the
    // selection argument array
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_COMMENT + " = " + "?";

    // Defines the argument for the selection column.
    final String[] SELECTION_ARGS = {"Transaction 0"};

    // Creates a projection includes the transaction id column, so that transaction id can be retrieved.
    final String[] TRANSACTION_ID_PROJECTION = {
        DatabaseConstants.KEY_ROWID,
        DatabaseConstants.KEY_COMMENT};

    // Query subtest 1.
    // Tests that a query against an empty table returns null.

    // Constructs a URI that matches the provider's transaction id URI pattern, using an arbitrary
    // value of 1 as the transaction ID.
    Uri transactionIdUri = ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, 1);

    // Queries the table with the transaction's ID URI. This should return an empty cursor.
    Cursor cursor = getMockContentResolver().query(
        transactionIdUri, // URI pointing to a single record
        null,      // no projection, get all the columns for each record
        null,      // no selection criteria, get all the records in the table
        null,      // no need for selection arguments
        null       // default sort, by ascending title
    );
    assert cursor != null;

    // Asserts that the cursor is null.
    assertEquals(0, cursor.getCount());

    // Query subtest 2.
    // Tests that a query against a table containing records returns a single record whose ID
    // is the one requested in the URI provided.

    // Inserts the test data into the provider's underlying data source.
    insertData();
    cursor.close();

    // Queries the table using the URI for the full table.
    cursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI, // the base URI for the table
        TRANSACTION_ID_PROJECTION,        // returns the ID and title columns of rows
        SELECTION_COLUMNS,         // select based on the title column
        SELECTION_ARGS,            // select title of "Transaction 0"
        null                 // sort order returned is by title, ascending
    );
    assert cursor != null;

    // Asserts that the cursor contains only one row.
    assertEquals(1, cursor.getCount());

    // Moves to the cursor's first row, and asserts that this did not fail.
    assertTrue(cursor.moveToFirst());

    // Saves the record's transaction ID.
    int inputTransactionId = cursor.getInt(0);

    // Builds a URI based on the provider's content ID URI base and the saved transaction ID.
    transactionIdUri = ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, inputTransactionId);

    cursor.close();

    // Queries the table using the content ID URI, which returns a single record with the
    // specified transaction ID, matching the selection criteria provided.
    cursor = getMockContentResolver().query(transactionIdUri, // the URI for a single transaction
        TRANSACTION_ID_PROJECTION,                 // same projection, get ID and title columns
        SELECTION_COLUMNS,                  // same selection, based on title column
        SELECTION_ARGS,                     // same selection arguments, title = "Transaction 0"
        null                          // same sort order returned, by title, ascending
    );
    assert cursor != null;

    // Asserts that the cursor contains only one row.
    assertEquals(1, cursor.getCount());

    // Moves to the cursor's first row, and asserts that this did not fail.
    assertTrue(cursor.moveToFirst());

    // Asserts that the transaction ID passed to the provider is the same as the transaction ID returned.
    assertEquals(inputTransactionId, cursor.getInt(0));
    cursor.close();
  }

  /*
   *  Tests inserts into the data model.
   */
  public void testInserts() {
    // Creates a new transaction instance
    TransactionInfo transaction = new TransactionInfo(
        "Transaction 4",
        new Date(), 1000, testAccountId, payeeId);

    // Insert subtest 1.
    // Inserts a row using the new transaction instance.
    // No assertion will be done. The insert() method either works or throws an Exception
    Uri rowUri = getMockContentResolver().insert(
        TransactionProvider.TRANSACTIONS_URI,  // the main table URI
        transaction.getContentValues()     // the map of values to insert as a new record
    );

    // Parses the returned URI to get the transaction ID of the new transaction. The ID is used in subtest 2.
    long transactionId = ContentUris.parseId(rowUri);

    // Does a full query on the table. Since insertData() hasn't yet been called, the
    // table should only contain the record just inserted.
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI, // the main table URI
        null,                      // no projection, return all the columns
        null,                      // no selection criteria, return all the rows in the model
        null,                      // no selection arguments
        null                       // default sort order
    );
    assert cursor != null;

    // Asserts that there should be only 1 record.
    assertEquals(1, cursor.getCount());

    // Moves to the first (and only) record in the cursor and asserts that this worked.
    assertTrue(cursor.moveToFirst());

    // Since no projection was used, get the column indexes of the returned columns
    int dateIndex = cursor.getColumnIndex(DatabaseConstants.KEY_DATE);
    int commentIndex = cursor.getColumnIndex(DatabaseConstants.KEY_COMMENT);
    int amountIndex = cursor.getColumnIndex(DatabaseConstants.KEY_AMOUNT);
    int payeeIndex = cursor.getColumnIndex(DatabaseConstants.KEY_PAYEE_NAME);

    // Tests each column in the returned cursor against the data that was inserted, comparing
    // the field in the TransactionInfo object to the data at the column index in the cursor.
    assertEquals(transaction.getComment(), cursor.getString(commentIndex));
    assertEquals(transaction.getDate(), cursor.getLong(dateIndex));
    assertEquals(transaction.getAmount(), cursor.getLong(amountIndex));
    assertEquals(PAYEE_NAME, cursor.getString(payeeIndex));
    // Insert subtest 2.
    // Tests that we can't insert a record whose id value already exists.

    // Defines a ContentValues object so that the test can add a transaction ID to it.
    ContentValues values = transaction.getContentValues();

    // Adds the transaction ID retrieved in subtest 1 to the ContentValues object.
    values.put(DatabaseConstants.KEY_ROWID, transactionId);

    // Tries to insert this record into the table.
    try {
      getMockContentResolver().insert(TransactionProvider.TRANSACTIONS_URI, values);
      fail("Expected insert failure for existing record but insert succeeded.");
    } catch (Exception e) {
      // succeeded, do nothing
    }
    cursor.close();
  }

  //Test that we can't insert a record that links to an account_id that does not exist
  public void testInsertViolatesForeignKey() {
    TransactionInfo transaction = new TransactionInfo(
        "Transaction 4",
        new Date(), 1000, testAccountId + 1, payeeId);
    try {
      getMockContentResolver().insert(TransactionProvider.TRANSACTIONS_URI, transaction.getContentValues());
      fail("Expected insert failure for link to non-existing account but insert succeeded.");
    } catch (Exception e) {
      // succeeded, so do nothing
    }

  }

  /*
   * Tests deletions from the data model.
   */
  public void testDeletes() {
    // Subtest 1.
    // Tries to delete a record from a data model that is empty.

    // Sets the selection column to "title"
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_COMMENT + " = " + "?";

    // Sets the selection argument "Transaction 0"
    final String[] SELECTION_ARGS = {"Transaction 0"};

    // Tries to delete rows matching the selection criteria from the data model.
    int rowsDeleted = getMockContentResolver().delete(
        TransactionProvider.TRANSACTIONS_URI, // the base URI of the table
        SELECTION_COLUMNS,         // select based on the title column
        SELECTION_ARGS             // select title = "Transaction 0"
    );

    // Assert that the deletion did not work. The number of deleted rows should be zero.
    assertEquals(0, rowsDeleted);

    // Subtest 2.
    // Tries to delete an existing record. Repeats the previous subtest, but inserts data first.

    // Inserts data into the model.
    insertData();

    // Uses the same parameters to try to delete the row with comment "Transaction 0"
    rowsDeleted = getMockContentResolver().delete(
        TransactionProvider.TRANSACTIONS_URI, // the base URI of the table
        SELECTION_COLUMNS,         // same selection column, "title"
        SELECTION_ARGS             // same selection arguments, comment = "Transaction 0"
    );

    // The number of deleted rows should be 1.
    assertEquals(1, rowsDeleted);

    // Tests that the record no longer exists. Tries to get it from the table, and
    // asserts that nothing was returned.

    // Queries the table with the same selection column and argument used to delete the row.
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI, // the base URI of the table
        null,                      // no projection, return all columns
        SELECTION_COLUMNS,         // select based on the title column
        SELECTION_ARGS,            // select title = "Transaction 0"
        null                       // use the default sort order
    );
    assert cursor != null;

    // Asserts that the cursor is empty since the record had already been deleted.
    assertEquals(0, cursor.getCount());
    cursor.close();
  }

  /*
   * Tests updates to the data model.
   */
  public void testUpdates() {
    // Selection column for identifying a record in the data model.
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_COMMENT + " = " + "?";

    // Selection argument for the selection column.
    final String[] selectionArgs = {"Transaction 1"};

    // Defines a map of column names and values
    ContentValues values = new ContentValues();

    // Subtest 1.
    // Tries to update a record in an empty table.

    // Sets up the update by putting the "comment" column and a value into the values map.
    values.put(DatabaseConstants.KEY_COMMENT, "Testing an update with this string");

    // Tries to update the table
    int rowsUpdated = getMockContentResolver().update(
        TransactionProvider.TRANSACTIONS_URI,  // the URI of the data table
        values,                     // a map of the updates to do (column title and value)
        SELECTION_COLUMNS,           // select based on the title column
        selectionArgs               // select "title = Transaction 1"
    );

    // Asserts that no rows were updated.
    assertEquals(0, rowsUpdated);

    // Subtest 2.
    // Builds the table, and then tries the update again using the same arguments.

    // Inserts data into the model.
    insertData();

    //  Does the update again, using the same arguments as in subtest 1.
    rowsUpdated = getMockContentResolver().update(
        TransactionProvider.TRANSACTIONS_URI,   // The URI of the data table
        values,                      // the same map of updates
        SELECTION_COLUMNS,            // same selection, based on the title column
        selectionArgs                // same selection argument, to select "title = Transaction 0"
    );

    // Asserts that only one row was updated. The selection criteria evaluated to
    // "title = Transaction 0", and the test data should only contain one row that matches that.
    assertEquals(1, rowsUpdated);
  }

  public void testToggleCrStatus() {
    insertData();
    final String[] TRANSACTION_ID_PROJECTION = {
        DatabaseConstants.KEY_ROWID,
        DatabaseConstants.KEY_COMMENT,
        DatabaseConstants.KEY_CR_STATUS};

    // Queries the table using the URI for the full table.
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI, // the base URI for the table
        TRANSACTION_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;

    // Moves to the cursor's first row, and asserts that this did not fail.
    assertTrue(cursor.moveToFirst());

    // Saves the record's note ID.
    int inputTransactionId = cursor.getInt(0);
    Uri transactionIdUri = ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, inputTransactionId);
    //default value should be unreconciled
    assertEquals(CrStatus.UNRECONCILED, CrStatus.valueOf(cursor.getString(2)));
    //toggle, then should be cleared
    getMockContentResolver().update(
        transactionIdUri.buildUpon()
            .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
            .build(),
        null, null, null);
    cursor.close();

    cursor = getMockContentResolver().query(
        transactionIdUri, // the base URI for the table
        TRANSACTION_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;
    assertTrue(cursor.moveToFirst());
    assertEquals(CrStatus.CLEARED, CrStatus.valueOf(cursor.getString(2)));

    //toggle again, then should be unreconciled
    getMockContentResolver().update(
        transactionIdUri.buildUpon()
            .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
            .build(),
        null, null, null);
    cursor.close();

    cursor = getMockContentResolver().query(
        transactionIdUri, // the base URI for the table
        TRANSACTION_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;
    assertTrue(cursor.moveToFirst());
    assertEquals(CrStatus.UNRECONCILED, CrStatus.valueOf(cursor.getString(2)));
    cursor.close();
  }
}
