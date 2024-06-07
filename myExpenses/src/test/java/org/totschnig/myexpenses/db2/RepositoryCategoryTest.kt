package org.totschnig.myexpenses.db2

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.Repository.Companion.UUID_SEPARATOR
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.model2.CategoryInfo

@RunWith(AndroidJUnit4::class)
class RepositoryCategoryTest : BaseTestWithRepository() {

    @Test
    fun findCategory() {
        val labelUnderTest = " Main "
        val labelUnderTestStripped = "Main"
        assertThat(repository.findCategory(labelUnderTest, null)).isEqualTo(-1)
        assertThat(repository.findCategory(labelUnderTestStripped, null)).isEqualTo(-1)
        assertThat(repository.saveCategory(Category(label = labelUnderTest))).isNotNull()

        val testedId = repository.findCategory(labelUnderTest)
        val testedStrippedId = repository.findCategory(labelUnderTestStripped)
        assertThat(testedId).isGreaterThan(0L)
        assertThat(testedStrippedId).isGreaterThan(0L)
        assertThat(testedId).isEqualTo(testedStrippedId)
    }

    @Test
    fun saveCategoryHierarchy() {
        val parent = Category(label = "Main")
        val parentId = repository.saveCategory(parent)!!
        val sub = Category(label = "Sub", parentId = parentId)
        val subId = repository.saveCategory(sub)!!
        val subsub = Category(label = "SubSub", parentId = subId)
        repository.saveCategory(subsub)
        assertThat(repository.findCategory(parent.label)).isGreaterThan(0L)
        assertThat(repository.findCategory(sub.label, parentId)).isGreaterThan(0L)
        assertThat(repository.findCategory(subsub.label, subId)).isGreaterThan(0L)
    }

    @Test
    fun transformMainToSub() {
        val cat1 = Category(label = "Main1")
        val catId1 = repository.saveCategory(cat1)!!
        val cat2 = Category(label = "Main2")
        val catId2 = repository.saveCategory(cat2)!!
        repository.moveCategory(catId2, catId1)
        with(repository.loadCategory(catId2)!!) {
            assertThat(parentId).isEqualTo(catId1)
            assertThat(color).isNull()
        }
    }

    @Test
    fun transformSubToMain() {
        val cat1 = Category(label = "Main1")
        val catId1 = repository.saveCategory(cat1)!!
        val cat2 = Category(label = "Main2", parentId = catId1)
        val catId2 = repository.saveCategory(cat2)!!
        repository.moveCategory(catId2, null)
        with(repository.loadCategory(catId2)!!) {
            assertThat(parentId).isNull()
            assertThat(color).isNotNull()
        }
    }

    @Test
    fun saveCategoryData() {
        val category = Category(label = "Main", icon = "food", color = Color.RED)
        val id = repository.saveCategory(category)!!
        with(repository.loadCategory(id)!!) {
            assertThat(label).isEqualTo("Main")
            assertThat(icon).isEqualTo("food")
            assertThat(color).isEqualTo(Color.RED)
        }
        val sub = Category(label = "Sub", icon = "bread", parentId = id)
        val subId = repository.saveCategory(sub)!!
        with(repository.loadCategory(subId)!!) {
            assertThat(label).isEqualTo("Sub")
            assertThat(icon).isEqualTo("bread")
            assertThat(color).isNull()
        }
    }

    @Test
    fun ensureCategoryNew() {
        val categoryInfo =
            CategoryInfo(label = "Main", icon = "food", uuid = "uuid", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo, null)
        assertThat(created).isTrue()
        with(repository.loadCategory(id)!!) {
            assertThat(label).isEqualTo("Main")
            assertThat(icon).isEqualTo("food")
            assertThat(uuid).isEqualTo("uuid")
            assertThat(color).isEqualTo(Color.RED)
        }
    }

    @Test
    fun ensureCategoryExisting() {
        val category = Category(label = "Main", icon = "food", uuid = "uuid")
        val existing = repository.saveCategory(category)!!
        val categoryInfo =
            CategoryInfo(label = "Main", icon = "food", uuid = "uuid", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo, null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
    }

    @Test
    fun ensureCategoryUpdate() {
        val category = Category(label = "Main", icon = "food", uuid = "uuid")
        val existing = repository.saveCategory(category)!!
        val categoryInfo =
            CategoryInfo(label = "New", icon = "apple", uuid = "uuid", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo, null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(label).isEqualTo("New")
            assertThat(icon).isEqualTo("apple")
            assertThat(uuid).isEqualTo("uuid")
            assertThat(color).isEqualTo(Color.RED)
        }
    }

    @Test
    fun ensureCategoryAppendUuid() {
        val category = Category(label = "Main", icon = "food", uuid = "uuid1")
        val existing = repository.saveCategory(category)!!
        val categoryInfo =
            CategoryInfo(label = "Main", icon = "food", uuid = "uuid2", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo, null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid!!.split(UUID_SEPARATOR)).containsExactly("uuid1", "uuid2")
        }
    }

    @Test
    fun ensureCategoryMultipleUuidsInDatabase() {
        val category = Category(label = "Main", icon = "food", uuid = "uuid1:uuid2")
        val existing = repository.saveCategory(category)!!
        val categoryInfo =
            CategoryInfo(label = "Main", icon = "food", uuid = "uuid1", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo, null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid!!.split(UUID_SEPARATOR)).containsExactly("uuid1", "uuid2")
        }
    }

    @Test
    fun ensureCategoryMultipleUuidsInUpdate() {
        val category = Category(label = "Main", icon = "food", uuid = "uuid1")
        val existing = repository.saveCategory(category)!!
        val categoryInfo =
            CategoryInfo(label = "Main", icon = "food", uuid = "uuid1:uuid2", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo, null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid).isEqualTo("uuid1")
        }
    }

    @Test
    fun ensureCategoryMultipleUuidsInDatabaseAndUpdate() {
        val category = Category(label = "Main", icon = "food", uuid = "uuid2:uuid1")
        val existing = repository.saveCategory(category)!!
        val categoryInfo =
            CategoryInfo(label = "Main", icon = "food", uuid = "uuid1:uuid2", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo, null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid!!.split(UUID_SEPARATOR)).containsExactly("uuid1", "uuid2")
        }
    }

    /**
     * We simulate the following scenario:
     * On devices A and B we have an identical category Test1, on A in addition we have Test2
     * User renames Test1 to Test2 on device B, and syncs
     * Expected outcome: On device A, Test1 is deleted, and Test2 has both uuids
     */
    @Test
    fun labelOfRenamedCategoryAlreadyExistsOnTarget() {
        val category1 = Category(label = "Test1", icon = null, uuid = "uuid1")
        val existing1 = repository.saveCategory(category1)!!
        val category2 = Category(label = "Test2", icon = null, uuid = "uuid2")
        val existing2 = repository.saveCategory(category2)!!
        val categoryInfo =
            CategoryInfo(label = "Test2", icon = null, uuid = "uuid1", color = null)
        repository.ensureCategory(categoryInfo, null)
        assertThat(repository.loadCategory(existing1)).isNull()
        with(repository.loadCategory(existing2)!!) {
            assertThat(uuid!!.split(UUID_SEPARATOR)).containsExactly("uuid1", "uuid2")
        }
    }
}