package org.totschnig.myexpenses.provider

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class CategoryTreeTest : BaseTestWithRepository() {

    @Test
    fun testCategoryDepth() {
        runDepthTest()
    }

    private fun runDepthTest() {

        contentResolver.query(
            TransactionProvider.DUAL_URI,
            arrayOf("sqlite_version()"),
            null, null, null
        )?.use {
            if (it.moveToFirst()) {
                //Ideally the test would run with SQLite version 3.50, which was affected by
                //https://github.com/mtotschnig/MyExpenses/issues/1883
                //Unfortunately Robolectric's SDK 36 implementation still uses 3.44.3
                println("Testing with SQLite Version: ${it.getString(0)}")
            }
        }

        val rootId = writeCategory("Root", null)
        val childId = writeCategory("Child", rootId)
        writeCategory("Grandchild", childId)

        contentResolver.query(
            BaseTransactionProvider.CATEGORY_DEPTH_URI,
            null, null, null, null
        ).useAndAssert {
            movesToFirst()
            hasInt(0, 3)
        }
    }
}