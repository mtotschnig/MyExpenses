package org.totschnig.myexpenses.provider

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class CategoryTreeTest : BaseTestWithRepository() {

    @Test
    fun testCategoryDepth() {
        runDepthTest()
    }

    @Test
    @Config(qualifiers = "en")
    fun parseEnglishCategoriesFromXml() {
        runParseTest()
    }

    @Test
    @Config(qualifiers = "de")
    fun parseGermanCategoriesFromXml() {
        runParseTest()
    }

    private fun runParseTest() {
        val categories = parseCategoriesFromXml(application.resources)
        assertWithMessage("Categories should not be empty").that(categories).isNotEmpty()
        for ((_, label, _, _, subcategories) in categories) {
            assertWithMessage("Category label should not start with @: $label")
                .that(label.startsWith("@")).isFalse()
            for ((label1) in subcategories) {
                assertWithMessage("Subcategory label should not start with @: $label1")
                    .that(label1.startsWith("@")).isFalse()
            }
        }
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