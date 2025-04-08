package org.totschnig.myexpenses.test.provider

import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.CATEGORIES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_SAVE_CATEGORY
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.myexpenses.testutils.CategoryInfo
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

class CategoryTest : BaseDbTest() {
    private lateinit var testCategories: List<Pair<Long, CategoryInfo>>
    private fun insertData() {
        testCategories = buildList {
            CategoryInfo("Main Expense", type = FLAG_EXPENSE.toInt()).let {
                add(mDb.insert(TABLE_CATEGORIES, it.contentValues) to it)
            }
            val main1Id = get(0).first
            CategoryInfo("Main Income", type = FLAG_EXPENSE.toInt()).let {
                add(mDb.insert(TABLE_CATEGORIES, it.contentValues) to it)
            }
            CategoryInfo("Sub 1", parentId = main1Id).let {
                add(mDb.insert(TABLE_CATEGORIES, it.contentValues) to it)
            }
            CategoryInfo("Sub 2", parentId = main1Id).let {
                add(mDb.insert(TABLE_CATEGORIES, it.contentValues) to it)
            }
        }
    }

    fun testQueriesOnCategoriesUri() {
        val testProjection = arrayOf(
            KEY_LABEL, KEY_PARENTID
        )
        val labelSelection = "$KEY_LABEL = ?"
        val selectionColumns = "$labelSelection OR $labelSelection OR $labelSelection"
        val selectionArgs = arrayOf("Main Expense", "Main Income", "Sub 1")
        val sortOrder = "$KEY_LABEL ASC"

        val origSize = repository.count(CATEGORIES_URI)
        insertData()

        mockContentResolver.query(
            CATEGORIES_URI,
            null,
            null,
            null,
            null
        ).useAndAssert { hasCount(origSize + testCategories.size) }

        mockContentResolver.query(
            CATEGORIES_URI,
            testProjection,
            null,
            null,
            null
        ).useAndAssert {
            hasColumnCount(testProjection.size)
            hasColumns(*testProjection)
        }

        mockContentResolver.query(
            CATEGORIES_URI,
            testProjection,
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

    fun testQueriesOnCategoryIdUri() {
        val selectionColumns = "$KEY_LABEL = ?"
        val selectionArgs = arrayOf("Main Expense")
        val projection = arrayOf(KEY_ROWID, KEY_LABEL)
        insertData()

        val inputCategoryId = mockContentResolver.query(
            CATEGORIES_URI,
            projection,
            selectionColumns,
            selectionArgs,
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            actual.getInt(0)
        }
        val categoryIdUri =
            ContentUris.withAppendedId(CATEGORIES_URI, inputCategoryId.toLong())

        mockContentResolver.query(
            categoryIdUri,
            projection,
            selectionColumns,
            selectionArgs,
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasInt(0, inputCategoryId)
        }
    }

    private fun insertCategory(categoryInfo: CategoryInfo) = mockContentResolver.call(
        DUAL_URI, METHOD_SAVE_CATEGORY, null, Bundle(1).apply {
            putParcelable(TransactionProvider.KEY_CATEGORY, Category(
                label = categoryInfo.label,
                parentId = categoryInfo.parentId,
                type = categoryInfo.type.toByte()
            )
            )
        })?.takeIf { it.containsKey(KEY_ROWID) }?.getLong(KEY_ROWID)

    fun testInserts() {
        val origSize = repository.count(CATEGORIES_URI)
        val categoryInfo = CategoryInfo("Main 3")
        assertThat(insertCategory(categoryInfo)).isNotNull()

        mockContentResolver.query(
            CATEGORIES_URI,
            null,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(origSize + 1)
            movesToFirst()
            isNull(KEY_PARENTID)
            hasString(KEY_LABEL, categoryInfo.label)
        }
        assertThat(insertCategory(categoryInfo.copy(parentId = 100))).isNull()
    }

    fun testDeleteCascades() {
        val selectionColumns = "$KEY_LABEL IN (?,?)"
        val selectionArgsMain = arrayOf("Main Expense", "Main Income")
        val selectionArgsSub = arrayOf("Sub 1", "Sub 2")
        insertData()
        assertThat(mockContentResolver.delete(
            CATEGORIES_URI,
            selectionColumns,
            selectionArgsMain
        )).isEqualTo(2)
        mockContentResolver.query(
            CATEGORIES_URI,
            null,
            selectionColumns,
            selectionArgsMain,
            null
        ).useAndAssert { hasCount(0) }
        mockContentResolver.query(
            CATEGORIES_URI,
            null,
            selectionColumns,
            selectionArgsSub,
            null
        ).useAndAssert { hasCount(0) }
    }

    fun testUpdates() {
        val selectionColumns = "$KEY_LABEL = ?"
        val selectionArgs = arrayOf("Main Expense")
        try {
            mockContentResolver.update(
                CATEGORIES_URI,
                ContentValues().apply {
                    put(KEY_LABEL, "Testing an update with this string")
                },
                selectionColumns,
                selectionArgs
            )
            fail("Bulk update should not succeed")
        } catch (_: UnsupportedOperationException) {
        }
    }

    fun testUniqueConstraintsCreateMain() {
        insertData()
        assertThat(insertCategory(CategoryInfo("Main Expense"))).isNull()
    }

    fun testUniqueConstraintsCreateSub() {
        insertData()
        assertThat(insertCategory(
            CategoryInfo("Sub 1", parentId = testCategories[0].first)
        )).isNull()
    }

    fun testUniqueConstraintsUpdateMain() {
        insertData()

        //we try to set the name of Main Income to Main Expense
        try {
            mockContentResolver.update(
                CATEGORIES_URI.buildUpon()
                    .appendPath(testCategories[1].first.toString())
                    .build(),
                ContentValues().apply {
                    put(KEY_LABEL, "Main Expense")
                }, null, null
            )
            fail("Expected unique constraint to prevent main category from being updated.")
        } catch (_: SQLiteConstraintException) {
            // succeeded, so do nothing
        }
    }

    fun testUniqueConstraintsUpdateSub() {
        insertData()
        try {
            mockContentResolver.update(
                CATEGORIES_URI.buildUpon()
                    .appendPath(testCategories[3].first.toString())
                    .build(),
                ContentValues().apply {
                    put(KEY_LABEL, "Sub 1")
                }, null, null
            )
            fail("Expected unique constraint to prevent sub category from being created.")
        } catch (_: SQLiteConstraintException) {
        }
    }

    fun testUpdateColor() {
        insertData()
        val testColor = ColorUtils.MAIN_COLORS[0]
        testCategories.forEach { pair ->
            val categoryIdUri =
                CATEGORIES_URI.buildUpon().appendPath(pair.first.toString()).build()
            mockContentResolver.update(
                categoryIdUri,
                ContentValues().apply {
                    put(KEY_COLOR, testColor)
                }, null, null
            )
            val projection = arrayOf(
                KEY_COLOR
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
            KEY_PARENTID,
            KEY_COLOR
        )
        mockContentResolver.query(
            CATEGORIES_URI,
            projection,
            null,
            null,
            null
        )!!.use {
            while (it.moveToNext()) {
                val color = it.getInt(1)
                if (it.isNull(0)) {
                    assertTrue(color != 0)
                } else {
                    assertEquals(0, color)
                }
            }
        }
    }

    fun testMoveToOtherCategorySucceeds() {
        insertData()
        //Main Income can be moved to Sub 1
        val sub1Id = testCategories[2].first
        val categoryIdUri = CATEGORIES_URI.buildUpon()
            .appendPath(testCategories[1].first.toString())
            .build()
        assertEquals(1, mockContentResolver.update(
            categoryIdUri,
            ContentValues().apply {
                put(KEY_PARENTID, sub1Id)
            }, null, null
        ))
        val projection = arrayOf(
            KEY_PARENTID
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
        //Main Expense can not be moved to itself
        val main1Id = testCategories[0].first
        val categoryIdUri = CATEGORIES_URI.buildUpon()
            .appendPath(main1Id.toString())
            .build()
        try {
            mockContentResolver.update(
                categoryIdUri,
                ContentValues().apply {
                    put(KEY_PARENTID, main1Id)
                }, null, null
            )
            fail("Row with parentId equal to _id must not be allowed")
        } catch (_: SQLiteConstraintException) {
        }
    }


    fun testShouldNotAllowCyclicHierarchy() {
        insertData()
        //Main Expense can not be moved to its own child Sub 1
        val main1Id = testCategories[0].first
        val sub1Id = testCategories[2].first
        val categoryIdUri = CATEGORIES_URI.buildUpon()
            .appendPath(main1Id.toString())
            .build()
        try {
            mockContentResolver.update(
                categoryIdUri,
                ContentValues().apply {
                    put(KEY_PARENTID, sub1Id)
                }, null, null
            )
            fail("Moving a category to its own descendant must be blocked.")
        } catch (_: SQLiteConstraintException) {
        }
    }
}