package org.totschnig.myexpenses.db2

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository

@RunWith(AndroidJUnit4::class)
class CategoryHelperTest: BaseTestWithRepository() {

    private lateinit var categoryToId: MutableMap<String, Long>

    @Before
    fun setup() {
        categoryToId = mutableMapOf()
    }

    @Test
    fun shouldInsertNormalWithStrip() {
        insert("abc", true)
        assertNotNull(categoryToId["abc"])
    }

    @Test
    fun shouldInsertNormalWithoutStrip() {
        insert("abc", false)
        assertNotNull(categoryToId["abc"])
    }

    @Test
    fun shouldInsertTwoLevels() {
        insert("abc:def", false)
        assertNotNull(categoryToId["abc:def"])
        assertNotNull(categoryToId["abc"])
    }

    @Test
    fun shouldInsertThreeLevels() {
        insert("abc:def:ghi", false)
        assertNotNull(categoryToId["abc:def:ghi"])
        assertNotNull(categoryToId["abc:def"])
        assertNotNull(categoryToId["abc"])
    }

    @Test
    fun shouldInsertSpecialWithStrip() {
        insert("abc/", true)
        assertNotNull(categoryToId["abc"])
        assertNull(categoryToId["abc/"])
    }

    @Test
    fun shouldInsertSpecialWithoutStrip() {
        insert("abc/", false)
        assertNull(categoryToId["abc"])
        assertNotNull(categoryToId["abc/"])
    }

    private fun insert(label: String, stripQifCategoryClass: Boolean) {
        CategoryHelper.insert(repository, label, categoryToId, stripQifCategoryClass)
    }
}