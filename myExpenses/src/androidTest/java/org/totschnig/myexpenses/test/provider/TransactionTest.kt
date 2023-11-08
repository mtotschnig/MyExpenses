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
package org.totschnig.myexpenses.test.provider

import android.content.ContentUris
import android.content.ContentValues
import junit.framework.TestCase
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.shared_test.CursorSubject.Companion.assertThat
import java.util.Date

class TransactionTest : BaseDbTest() {
    // Contains the test data, as an array of TransactionInfo instances.
    private lateinit var infos: Array<TransactionInfo>
    private val payee = "N.N"
    private var testAccountId: Long = 0
    private var payeeId: Long = 0

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0)
        testAccountId = mDb.insert(TABLE_ACCOUNTS, testAccount.contentValues)
        payeeId = mDb.insert(TABLE_PAYEES, PayeeInfo(payee).contentValues)
    }

    /**
     * Sets up test data.
     * The test data is in an SQL database. It is created in setUp() without any data,
     * and populated in insertData if necessary.
     */
    private fun insertData() {
        infos = arrayOf(
            TransactionInfo(testAccountId, 0, Date(), "Transaction 0", payeeId),
            TransactionInfo(testAccountId, 100, Date(), "Transaction 1", payeeId),
            TransactionInfo(testAccountId, -100, Date(), "Transaction 2", payeeId)
        )

        // Sets up test data
        for (transactionInfo in infos) {

            // Adds a record to the database.
            mDb.insert(
                TABLE_TRANSACTIONS,  // the table name for the insert
                transactionInfo.contentValues // the values map to insert
            )
        }
    }

    /*
   * Tests the provider's public API for querying data in the table, using the URI for
   * a dataset of records.
   */
    fun testQueriesOnTransactionUri() {
        // Defines a projection of column names to return for a query
        val projection = arrayOf(
            KEY_COMMENT, KEY_DATE, KEY_PAYEEID
        )

        // Defines a selection column for the query. When the selection columns are passed
        // to the query, the selection arguments replace the placeholders.
        val selection = "$KEY_COMMENT = ?"

        // Defines the selection columns for a query.
        val selectionColumns = "$selection OR $selection OR $selection"

        // Defines the arguments for the selection columns.
        val selectionArgs = arrayOf("Transaction 0", "Transaction 1", "Transaction 2")

        // Defines a query sort order
        val sortOrder = "$KEY_COMMENT ASC"

        // Query subtest 1.
        // If there are no records in the table, the returned cursor from a query should be empty.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the URI for the main data table
            null,  // no projection, get all columns
            null,  // no selection criteria, get all records
            null,  // no selection arguments
            null // use default sort order
        )!!.use {
            assertThat(it).hasCount(0)
        }

        insertData()

        // Gets all the columns for all the rows in the table
       mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the URI for the main data table
            null,  // no projection, get all columns
            null,  // no selection criteria, get all records
            null,  // no selection arguments
            null // use default sort order
        )!!.use {
           assertThat(it).hasCount(infos.size)
       }

        // Query subtest 3.
        // A query that uses a projection should return a cursor with the same number of columns
        // as the projection, with the same names, in the same order.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the URI for the main data table
            projection,  // get the comment, date and payee_id
            null,  // no selection columns, get all the records
            null,  // no selection criteria
            null // use default the sort order
        )!!.use {
            with(assertThat(it)) {
                hasColumnCount(projection.size)
                hasColumns(*projection)
            }
        }

        // Query subtest 4
        // A query that uses selection criteria should return only those rows that match the
        // criteria. Use a projection so that it's easy to get the data in a particular column.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the URI for the main data table
            projection,  //  get the comment, date and payee_id
            selectionColumns,  // select on the title column
            selectionArgs,  // select titles "Transaction 0", "Transaction 1", or "Transaction 2"
            sortOrder // sort ascending on the title column
        )!!.use {
            with(assertThat(it)) {
                hasCount(selectionArgs.size)
                var index = 0
                while (it.moveToNext()) {
                    hasString(0, selectionArgs[index])
                    index++
                }
            }
        }
    }

    /*
   * Tests queries against the provider, using the transaction id URI. This URI encodes a single
   * record ID. The provider should only return 0 or 1 record.
   */
    fun testQueriesOnTransactionIdUri() {
        // Defines the selection column for a query. The "?" is replaced by entries in the
        // selection argument array
        val selection = "$KEY_COMMENT = ?"

        // Defines the argument for the selection column.
        val selectionArgs = arrayOf("Transaction 0")

        // Creates a projection includes the transaction id column, so that transaction id can be retrieved.
        val projection = arrayOf(
            KEY_ROWID,
            KEY_COMMENT
        )

        // Query subtest 1.
        // Tests that a query against an empty table returns null.

        // Constructs a URI that matches the provider's transaction id URI pattern, using an arbitrary
        // value of 1 as the transaction ID.
        var transactionIdUri = ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, 1)

        // Queries the table with the transaction's ID URI. This should return an empty cursor.
        mockContentResolver.query(
            transactionIdUri,  // URI pointing to a single record
            null,  // no projection, get all the columns for each record
            null,  // no selection criteria, get all the records in the table
            null,  // no need for selection arguments
            null // default sort, by ascending title
        )!!.use {
            assertThat(it).hasCount(0)
        }

        insertData()

        // Queries the table using the URI for the full table.
        val inputTransactionId = mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the base URI for the table
            projection,  // returns the ID and title columns of rows
            selection,  // select based on the title column
            selectionArgs,  // select title of "Transaction 0"
            null // sort order returned is by title, ascending
        )!!.use {
            with(assertThat(it)) {
                hasCount(1)
                movesToFirst()
            }
            it.getInt(0)
        }

        // Builds a URI based on the provider's content ID URI base and the saved transaction ID.
        transactionIdUri = ContentUris.withAppendedId(
            TransactionProvider.TRANSACTIONS_URI,
            inputTransactionId.toLong()
        )

        // Queries the table using the content ID URI, which returns a single record with the
        // specified transaction ID, matching the selection criteria provided.
        mockContentResolver.query(
            transactionIdUri,  // the URI for a single transaction
            projection,  // same projection, get ID and title columns
            selection,  // same selection, based on title column
            selectionArgs,  // same selection arguments, title = "Transaction 0"
            null // same sort order returned, by title, ascending
        )!!.use {
            with(assertThat(it)) {
                hasCount(1)
                movesToFirst()
                hasInt(0, inputTransactionId)
            }
        }
    }

    /*
   *  Tests inserts into the data model.
   */
    fun testInserts() {
        // Creates a new transaction instance
        val transaction = TransactionInfo(
            testAccountId,
            1000, Date(), "Transaction 4", payeeId
        )

        // Insert subtest 1.
        // Inserts a row using the new transaction instance.
        // No assertion will be done. The insert() method either works or throws an Exception
        val rowUri = mockContentResolver.insert(
            TransactionProvider.TRANSACTIONS_URI,  // the main table URI
            transaction.contentValues // the map of values to insert as a new record
        )

        // Parses the returned URI to get the transaction ID of the new transaction. The ID is used in subtest 2.
        val transactionId = ContentUris.parseId(rowUri!!)

        // Does a full query on the table. Since insertData() hasn't yet been called, the
        // table should only contain the record just inserted.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the main table URI
            null,  // no projection, return all the columns
            null,  // no selection criteria, return all the rows in the model
            null,  // no selection arguments
            null // default sort order
        )!!.use {
            with(assertThat(it)) {
                hasCount(1)
                movesToFirst()
                hasString(KEY_COMMENT, transaction.comment)
                hasLong(KEY_DATE, transaction.dateAsLong)
                hasLong(KEY_DISPLAY_AMOUNT, transaction.amount)
                hasString(KEY_PAYEE_NAME, payee)
            }
        }

        // Insert subtest 2.
        // Tests that we can't insert a record whose id value already exists.

        // Defines a ContentValues object so that the test can add a transaction ID to it.
        val values = transaction.contentValues

        // Adds the transaction ID retrieved in subtest 1 to the ContentValues object.
        values.put(KEY_ROWID, transactionId)

        // Tries to insert this record into the table.
        try {
            mockContentResolver.insert(TransactionProvider.TRANSACTIONS_URI, values)
            TestCase.fail("Expected insert failure for existing record but insert succeeded.")
        } catch (e: Exception) {
            // succeeded, do nothing
        }
    }

    //Test that we can't insert a record that links to an account_id that does not exist
    fun testInsertViolatesForeignKey() {
        val transaction = TransactionInfo(
            testAccountId + 1,
            1000, Date(), "Transaction 4", payeeId
        )
        try {
            mockContentResolver.insert(
                TransactionProvider.TRANSACTIONS_URI,
                transaction.contentValues
            )
            TestCase.fail("Expected insert failure for link to non-existing account but insert succeeded.")
        } catch (e: Exception) {
            // succeeded, so do nothing
        }
    }

    /*
   * Tests deletions from the data model.
   */
    fun testDeletes() {
        // Subtest 1.
        // Tries to delete a record from a data model that is empty.

        // Sets the selection column to "title"
        val selection = "$KEY_COMMENT = ?"

        // Sets the selection argument "Transaction 0"
        val selectionArgs = arrayOf("Transaction 0")

        // Tries to delete rows matching the selection criteria from the data model.
        var rowsDeleted = mockContentResolver.delete(
            TransactionProvider.TRANSACTIONS_URI,  // the base URI of the table
            selection,  // select based on the title column
            selectionArgs // select title = "Transaction 0"
        )

        // Assert that the deletion did not work. The number of deleted rows should be zero.
        TestCase.assertEquals(0, rowsDeleted)

        // Subtest 2.
        // Tries to delete an existing record. Repeats the previous subtest, but inserts data first.

        // Inserts data into the model.
        insertData()

        // Uses the same parameters to try to delete the row with comment "Transaction 0"
        rowsDeleted = mockContentResolver.delete(
            TransactionProvider.TRANSACTIONS_URI,  // the base URI of the table
            selection,  // same selection column, "title"
            selectionArgs // same selection arguments, comment = "Transaction 0"
        )

        // The number of deleted rows should be 1.
        TestCase.assertEquals(1, rowsDeleted)

        // Tests that the record no longer exists. Tries to get it from the table, and
        // asserts that nothing was returned.

        // Queries the table with the same selection column and argument used to delete the row.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the base URI of the table
            null,  // no projection, return all columns
            selection,  // select based on the title column
            selectionArgs,  // select title = "Transaction 0"
            null // use the default sort order
        )!!.use {
            assertThat(it).hasCount(0)
        }
    }

    /*
   * Tests updates to the data model.
   */
    fun testUpdates() {
        // Selection column for identifying a record in the data model.
        val columns = "$KEY_COMMENT = ?"

        // Selection argument for the selection column.
        val selectionArgs = arrayOf("Transaction 1")

        // Defines a map of column names and values
        val values = ContentValues()

        // Subtest 1.
        // Tries to update a record in an empty table.

        // Sets up the update by putting the "comment" column and a value into the values map.
        values.put(KEY_COMMENT, "Testing an update with this string")

        // Tries to update the table
        var rowsUpdated = mockContentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,  // the URI of the data table
            values,  // a map of the updates to do (column title and value)
            columns,  // select based on the title column
            selectionArgs // select "title = Transaction 1"
        )

        // Asserts that no rows were updated.
        TestCase.assertEquals(0, rowsUpdated)

        // Subtest 2.
        // Builds the table, and then tries the update again using the same arguments.

        // Inserts data into the model.
        insertData()

        //  Does the update again, using the same arguments as in subtest 1.
        rowsUpdated = mockContentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,  // The URI of the data table
            values,  // the same map of updates
            columns,  // same selection, based on the title column
            selectionArgs // same selection argument, to select "title = Transaction 0"
        )

        // Asserts that only one row was updated. The selection criteria evaluated to
        // "title = Transaction 0", and the test data should only contain one row that matches that.
        TestCase.assertEquals(1, rowsUpdated)
    }

    fun testToggleCrStatus() {
        insertData()
        val projection = arrayOf(
            KEY_ROWID,
            KEY_COMMENT,
            KEY_CR_STATUS
        )

        // Queries the table using the URI for the full table.
        val inputTransactionId = mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,  // the base URI for the table
            projection,
            null,
            null,
            null
        )!!.use {
            assertThat(it).movesToFirst()
            assertThat(it).hasString(2, CrStatus.UNRECONCILED.name)
            it.getInt(0)

        }

        val transactionIdUri = ContentUris.withAppendedId(
            TransactionProvider.TRANSACTIONS_URI,
            inputTransactionId.toLong()
        )

        //toggle, then should be cleared
        mockContentResolver.update(
            transactionIdUri.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
                .build(),
            null, null, null
        )

        mockContentResolver.query(
            transactionIdUri,  // the base URI for the table
            projection,
            null,
            null,
            null
        )!!.use {
            assertThat(it).movesToFirst()
            assertThat(it).hasString(2, CrStatus.CLEARED.name)
        }

        //toggle again, then should be unreconciled
        mockContentResolver.update(
            transactionIdUri.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
                .build(),
            null, null, null
        )
        mockContentResolver.query(
            transactionIdUri,  // the base URI for the table
            projection,
            null,
            null,
            null
        )!!.use {
            assertThat(it).movesToFirst()
            assertThat(it).hasString(2, CrStatus.UNRECONCILED.name)
        }
    }
}
