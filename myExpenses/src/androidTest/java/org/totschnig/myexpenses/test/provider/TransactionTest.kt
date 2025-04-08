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
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.PayeeInfo
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

class TransactionTest : BaseDbTest() {
    private lateinit var infos: Array<TransactionInfo>
    private val payee = "N.N"
    private var testAccountId: Long = 0
    private var payeeId: Long = 0

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        testAccountId = setupTestAccount()
        payeeId = mDb.insert(TABLE_PAYEES, PayeeInfo(payee).contentValues)
    }

    private fun insertData() {
        infos = arrayOf(
            TransactionInfo(
                accountId = testAccountId,
                amount = 0,
                comment = "Transaction 0",
                payeeId = payeeId
            ),
            TransactionInfo(
                accountId = testAccountId,
                amount = 100,
                comment = "Transaction 1",
                payeeId = payeeId
            ),
            TransactionInfo(
                accountId = testAccountId,
                amount = -100,
                comment = "Transaction 2",
                payeeId = payeeId
            )
        )

        for (transactionInfo in infos) {

            mDb.insert(
                TABLE_TRANSACTIONS,
                transactionInfo.contentValues
            )
        }
    }

    /*
   * Tests the provider's public API for querying data in the table, using the URI for
   * a dataset of records.
   */
    fun testQueriesOnTransactionUri() {
        val projection = arrayOf(
            KEY_COMMENT, KEY_DATE, KEY_PAYEEID
        )
        val selection = "$KEY_COMMENT = ?"
        val selectionColumns = "$selection OR $selection OR $selection"
        val selectionArgs = arrayOf("Transaction 0", "Transaction 1", "Transaction 2")
        val sortOrder = "$KEY_COMMENT ASC"
        // Query subtest 1.
        // If there are no records in the table, the returned cursor from a query should be empty.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            null,
            null,
            null,
            null
        ).useAndAssert { hasCount(0) }

        insertData()

        // Gets all the columns for all the rows in the table
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            null,
            null,
            null,
            null
        ).useAndAssert { hasCount(infos.size) }

        // Query subtest 3.
        // A query that uses a projection should return a cursor with the same number of columns
        // as the projection, with the same names, in the same order.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            projection,
            null,
            null,
            null
        ).useAndAssert {
            hasColumnCount(projection.size)
            hasColumns(*projection)
        }

        // Query subtest 4
        // A query that uses selection criteria should return only those rows that match the
        // criteria. Use a projection so that it's easy to get the data in a particular column.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            projection,
            selectionColumns,
            selectionArgs,
            sortOrder
        ).useAndAssert {
            hasCount(selectionArgs.size)
            var index = 0
            while (actual.moveToNext()) {
                hasString(0, selectionArgs[index])
                index++
            }
        }
    }

    /*
   * Tests queries against the provider, using the transaction id URI. This URI encodes a single
   * record ID. The provider should only return 0 or 1 record.
   */
    fun testQueriesOnTransactionIdUri() {
        val selection = "$KEY_COMMENT = ?"

        val selectionArgs = arrayOf("Transaction 0")

        val projection = arrayOf(
            KEY_ROWID,
            KEY_COMMENT
        )

        var transactionIdUri = ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, 1)

        // Queries the table with the transaction's ID URI. This should return an empty cursor.
        mockContentResolver.query(
            transactionIdUri,
            null,
            null,
            null,
            null
        ).useAndAssert { hasCount(0) }

        insertData()

        // Queries the table using the URI for the full table.
        val inputTransactionId = mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            projection,
            selection,
            selectionArgs,
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            actual.getInt(0)
        }

        // Builds a URI based on the provider's content ID URI base and the saved transaction ID.
        transactionIdUri = ContentUris.withAppendedId(
            TransactionProvider.TRANSACTIONS_URI,
            inputTransactionId.toLong()
        )

        // Queries the table using the content ID URI, which returns a single record with the
        // specified transaction ID, matching the selection criteria provided.
        mockContentResolver.query(
            transactionIdUri,
            projection,
            selection,
            selectionArgs,
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasInt(0, inputTransactionId)
        }
    }

    /*
   *  Tests inserts into the data model.
   */
    fun testInserts() {
        val transaction = TransactionInfo(
            accountId = testAccountId,
            amount = 1000,
            comment = "Transaction 4",
            payeeId = payeeId
        )

        // Insert subtest 1.
        // Inserts a row using the new transaction instance.
        // No assertion will be done. The insert() method either works or throws an Exception
        val rowUri = mockContentResolver.insert(
            TransactionProvider.TRANSACTIONS_URI,
            transaction.contentValues
        )

        val transactionId = ContentUris.parseId(rowUri!!)

        // Does a full query on the table. Since insertData() hasn't yet been called, the
        // table should only contain the record just inserted.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            null,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasString(KEY_COMMENT, transaction.comment)
            hasLong(KEY_DATE, transaction.dateAsLong)
            hasLong(KEY_DISPLAY_AMOUNT, transaction.amount)
            hasString(KEY_PAYEE_NAME, payee)
        }

        // Insert subtest 2.
        // Tests that we can't insert a record whose id value already exists.

        val values = transaction.contentValues
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
            accountId = testAccountId + 1,
            amount = 1000,
            comment = "Transaction 4",
            payeeId = payeeId
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

        val selection = "$KEY_COMMENT = ?"
        val selectionArgs = arrayOf("Transaction 0")

        // Tries to delete rows matching the selection criteria from the data model.
        var rowsDeleted = mockContentResolver.delete(
            TransactionProvider.TRANSACTIONS_URI,
            selection,
            selectionArgs
        )

        // Assert that the deletion did not work. The number of deleted rows should be zero.
        TestCase.assertEquals(0, rowsDeleted)

        // Subtest 2.
        // Tries to delete an existing record. Repeats the previous subtest, but inserts data first.

        insertData()

        // Uses the same parameters to try to delete the row with comment "Transaction 0"
        rowsDeleted = mockContentResolver.delete(
            TransactionProvider.TRANSACTIONS_URI,
            selection,
            selectionArgs
        )

        TestCase.assertEquals(1, rowsDeleted)

        // Tests that the record no longer exists. Tries to get it from the table, and
        // asserts that nothing was returned.

        // Queries the table with the same selection column and argument used to delete the row.
        mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            null,
            selection,
            selectionArgs,
            null
        ).useAndAssert { hasCount(0) }
    }

    /*
   * Tests updates to the data model.
   */
    fun testUpdates() {
        val columns = "$KEY_COMMENT = ?"
        val selectionArgs = arrayOf("Transaction 1")
        val values = ContentValues()

        // Subtest 1.
        // Tries to update a record in an empty table.
        values.put(KEY_COMMENT, "Testing an update with this string")

        var rowsUpdated = mockContentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,
            values,
            columns,
            selectionArgs
        )

        TestCase.assertEquals(0, rowsUpdated)

        // Subtest 2.
        // Builds the table, and then tries the update again using the same arguments.

        insertData()

        //  Does the update again, using the same arguments as in subtest 1.
        rowsUpdated = mockContentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,
            values,
            columns,
            selectionArgs
        )

        // Asserts that only one row was updated. The selection criteria evaluated to
        // "title = Transaction 0", and the test data should only contain one row that matches that.
        TestCase.assertEquals(1, rowsUpdated)
    }

    fun testToggleCrStatus() {
        insertData()

        val inputTransactionId = mockContentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID, KEY_CR_STATUS),
            null,
            null,
            null
        ).useAndAssert {
            movesToFirst()
            hasString(1, CrStatus.UNRECONCILED.name)
            actual.getInt(0)
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
            transactionIdUri,
            arrayOf(KEY_CR_STATUS),
            null,
            null,
            null
        ).useAndAssert {
            movesToFirst()
            hasString(0, CrStatus.CLEARED.name)
        }

        //toggle again, then should be unreconciled
        mockContentResolver.update(
            transactionIdUri.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
                .build(),
            null, null, null
        )
        mockContentResolver.query(
            transactionIdUri,
            arrayOf(KEY_CR_STATUS),
            null,
            null,
            null
        ).useAndAssert {
            movesToFirst()
            hasString(0, CrStatus.UNRECONCILED.name)
        }
    }
}
