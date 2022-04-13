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
package org.totschnig.myexpenses.test.provider

import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.util.ColorUtils

class CategoryTest : BaseDbTest() {
    private lateinit var testCategories: List<Pair<Long, CategoryInfo>>
    private fun insertData() {
        testCategories = buildList {
            CategoryInfo("Main 1", null).let {
                add(mDb.insertOrThrow(TABLE_CATEGORIES, null, it.contentValues) to it)
            }
            val main1Id = get(0).first
            CategoryInfo("Main 2", null).let {
                add(mDb.insertOrThrow(TABLE_CATEGORIES, null, it.contentValues) to it)
            }
            CategoryInfo("Sub 1", main1Id).let {
                add(mDb.insertOrThrow(TABLE_CATEGORIES, null, it.contentValues) to it)
            }
            CategoryInfo("Sub 2", main1Id).let {
                add(mDb.insertOrThrow(TABLE_CATEGORIES, null, it.contentValues) to it)
            }
        }
    }

    fun testQueriesOnCategoriesUri() {
        val testProjection = arrayOf(
            DatabaseConstants.KEY_LABEL, DatabaseConstants.KEY_PARENTID
        )
        val labelSelection = DatabaseConstants.KEY_LABEL + " = " + "?"
        val selectionColumns = "$labelSelection OR $labelSelection OR $labelSelection"
        val selectionArgs = arrayOf("Main 1", "Main 2", "Sub 1")
        val sortOrder = DatabaseConstants.KEY_LABEL + " ASC"
        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            null,
            null,
            null,
            null
        )!!.use {
            assertEquals(0, it.count)
            insertData()
        }

        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            null,
            null,
            null,
            null
        )!!.use {
            assertEquals(testCategories.size, it.count)
        }

        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            testProjection,
            null,
            null,
            null
        )!!.use {
            assertEquals(testProjection.size, it.columnCount)
            assertEquals(testProjection[0], it.getColumnName(0))
            assertEquals(testProjection[1], it.getColumnName(1))
        }

        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            testProjection,
            selectionColumns,
            selectionArgs,
            sortOrder
        )!!.use {
            assertEquals(selectionArgs.size, it.count)
            var index = 0
            while (it.moveToNext()) {
                assertEquals(selectionArgs[index], it.getString(0))
                index++
            }
            assertEquals(selectionArgs.size, index)
        }
    }

    fun testQueriesOnCategoryIdUri() {
        val selectionColumns = DatabaseConstants.KEY_LABEL + " = " + "?"
        val selectionArgs = arrayOf("Main 1")
        val projection = arrayOf(
            DatabaseConstants.KEY_ROWID,
            DatabaseConstants.KEY_LABEL
        )
        var categoryIdUri = ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, 1)

        mockContentResolver.query(
            categoryIdUri,
            null,
            null,
            null,
            null
        )!!.use {
            assertEquals(0, it.count)
            insertData()
        }

        val inputCategoryId = mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            projection,
            selectionColumns,
            selectionArgs,
            null
        )!!.use {
            assertEquals(1, it.count)
            assertTrue(it.moveToFirst())
            it.getInt(0)
        }
        categoryIdUri =
            ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, inputCategoryId.toLong())

        mockContentResolver.query(
            categoryIdUri,
            projection,
            selectionColumns,
            selectionArgs,
            null
        )!!.use {
            assertEquals(1, it.count)
            assertTrue(it.moveToFirst())
            assertEquals(inputCategoryId, it.getInt(0))
        }
    }

    fun testInserts() {
        val transaction = CategoryInfo(
            "Main 3", null
        )
        val rowUri = mockContentResolver.insert(
            TransactionProvider.CATEGORIES_URI,
            transaction.contentValues
        )
        val categoryId = ContentUris.parseId(rowUri!!)

        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            null,
            null,
            null,
            null
        )!!.use {
            assertEquals(1, it.count)
            assertTrue(it.moveToFirst())
            val labelIndex = it.getColumnIndex(DatabaseConstants.KEY_LABEL)
            val parentIdIndex = it.getColumnIndex(DatabaseConstants.KEY_PARENTID)
            assertEquals(transaction.parentId, DbUtils.getLongOrNull(it, parentIdIndex))
            assertEquals(transaction.label, it.getString(labelIndex))
        }

        val values = transaction.contentValues
        values.put(DatabaseConstants.KEY_ROWID, categoryId)
        try {
            mockContentResolver.insert(TransactionProvider.CATEGORIES_URI, values)
            fail("Expected insert failure for existing record but insert succeeded.")
        } catch (e: Exception) {
        }
        values.remove(DatabaseConstants.KEY_ROWID)
        values.put(DatabaseConstants.KEY_PARENTID, 100)
        try {
            mockContentResolver.insert(TransactionProvider.CATEGORIES_URI, values)
            fail("Expected insert failure for link to non-existing parent but insert succeeded.")
        } catch (e: Exception) {
        }
    }

    fun testDeleteCascades() {
        val selectionColumns = DatabaseConstants.KEY_LABEL + " IN (?,?)"
        val selectionArgsMain = arrayOf("Main 1", "Main 2")
        val selectionArgsSub = arrayOf("Sub 1", "Sub 2")
        insertData()
        val rowsDeleted = mockContentResolver.delete(
            TransactionProvider.CATEGORIES_URI,
            selectionColumns,
            selectionArgsMain
        )
        assertEquals(2, rowsDeleted)
        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            null,
            selectionColumns,
            selectionArgsMain,
            null
        )!!.use {
            assertEquals(0, it.count)
        }
        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            null,
            selectionColumns,
            selectionArgsSub,
            null
        )!!.use {
            assertEquals(0, it.count)
        }
    }

    fun testUpdates() {
        val selectionColumns = DatabaseConstants.KEY_LABEL + " = " + "?"
        val selectionArgs = arrayOf("Main 1")
        try {
            mockContentResolver.update(
                TransactionProvider.CATEGORIES_URI,
                ContentValues().apply {
                    put(DatabaseConstants.KEY_LABEL, "Testing an update with this string")
                },
                selectionColumns,
                selectionArgs
            )
            fail("Bulk update should not succeed")
        } catch (ignored: UnsupportedOperationException) {
        }
    }

    fun testUniqueConstraintsCreateMain() {
        insertData()
        val category = CategoryInfo(
            "Main 1", null
        )
        try {
            mockContentResolver.insert(
                TransactionProvider.CATEGORIES_URI,
                category.contentValues
            )
            fail("Expected unique constraint to prevent main category from being created.")
        } catch (e: SQLiteConstraintException) {
        }
    }

    fun testUniqueConstraintsCreateSub() {
        insertData()
        val category = CategoryInfo(
            "Sub 1", testCategories[0].first
        )
        try {
            mockContentResolver.insert(
                TransactionProvider.CATEGORIES_URI,
                category.contentValues
            )
            fail("Expected unique constraint to prevent sub category from being created.")
        } catch (e: SQLiteConstraintException) {
        }
    }

    fun testUniqueConstraintsUpdateMain() {
        insertData()

        //we try to set the name of Main 2 to Main 1
        try {
            mockContentResolver.update(
                TransactionProvider.CATEGORIES_URI.buildUpon()
                    .appendPath(testCategories[1].first.toString())
                    .build(),
                ContentValues().apply {
                    put(DatabaseConstants.KEY_LABEL, "Main 1")
                }, null, null
            )
            fail("Expected unique constraint to prevent main category from being updated.")
        } catch (e: SQLiteConstraintException) {
            // succeeded, so do nothing
        }
    }

    fun testUniqueConstraintsUpdateSub() {
        insertData()
        try {
            mockContentResolver.update(
                TransactionProvider.CATEGORIES_URI.buildUpon()
                    .appendPath(testCategories[3].first.toString())
                    .build(),
                ContentValues().apply {
                    put(DatabaseConstants.KEY_LABEL, "Sub 1")
                }, null, null
            )
            fail("Expected unique constraint to prevent sub category from being created.")
        } catch (e: SQLiteConstraintException) {
        }
    }

    fun testUpdateColor() {
        insertData()
        val testColor = ColorUtils.MAIN_COLORS[0]
        testCategories.forEach { pair ->
            val categoryIdUri =
                TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(pair.first.toString()).build()
            mockContentResolver.update(
                categoryIdUri,
                ContentValues().apply {
                    put(DatabaseConstants.KEY_COLOR, testColor)
                }, null, null
            )
            val projection = arrayOf(
                DatabaseConstants.KEY_COLOR
            )
            mockContentResolver.query(
                categoryIdUri,
                projection,
                null,
                null,
                null
            )!!.use {
                assertEquals(1, it.count)
                assertTrue(it.moveToFirst())
                assertEquals(testColor, it.getInt(0))
            }
        }
    }

    fun testAutomaticInsertOfColorForMainCategories() {
        val projection = arrayOf(
            DatabaseConstants.KEY_PARENTID,
            DatabaseConstants.KEY_COLOR
        )
        mockContentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            projection,
            null,
            null,
            null
        )!!.use {
            while (it.moveToNext()) {
                val color = it.getInt(1)
                if (it.isNull(0)) {
                    //parentId
                    assertTrue(color != 0)
                } else {
                    assertEquals(0, color)
                }
            }
        }
    }

    fun testMoveToOtherCategorySucceeds() {
        insertData()
        //Main 2 can be moved to Sub 1
        val sub1Id = testCategories[2].first
        val categoryIdUri = TransactionProvider.CATEGORIES_URI.buildUpon()
            .appendPath(testCategories[1].first.toString())
            .build()
        assertEquals(1, mockContentResolver.update(
            categoryIdUri,
            ContentValues().apply {
                put(DatabaseConstants.KEY_PARENTID, sub1Id)
            }, null, null
        ))
        val projection = arrayOf(
            DatabaseConstants.KEY_PARENTID
        )
        mockContentResolver.query(
            categoryIdUri,
            projection,
            null,
            null,
            null
        )!!.use {
            assertEquals(1, it.count)
            assertTrue(it.moveToFirst())
            assertEquals(sub1Id, it.getLong(0))
        }
    }

    fun testShouldNotAllowSelfReference() {
        insertData()
        //Main 1 can not be moved to itself
        val main1Id = testCategories[0].first
        val categoryIdUri = TransactionProvider.CATEGORIES_URI.buildUpon()
            .appendPath(main1Id.toString())
            .build()
        try {
            mockContentResolver.update(
                categoryIdUri,
                ContentValues().apply {
                    put(DatabaseConstants.KEY_PARENTID, main1Id)
                }, null, null
            )
            fail("Row with parentId equal to _id must not be allowed")
        } catch (e: SQLiteConstraintException) {
        }
    }


    fun testShouldNotAllowCyclicHierarchy() {
        insertData()
        //Main 1 can not be moved to its own child Sub 1
        val main1Id = testCategories[0].first
        val sub1Id = testCategories[2].first
        val categoryIdUri = TransactionProvider.CATEGORIES_URI.buildUpon()
            .appendPath(main1Id.toString())
            .build()
        try {
            mockContentResolver.update(
                categoryIdUri,
                ContentValues().apply {
                    put(DatabaseConstants.KEY_PARENTID, sub1Id)
                }, null, null
            )
            fail("Moving a category to its own descendant must be blocked.")
        } catch (e: SQLiteConstraintException) {
        }
    }
}