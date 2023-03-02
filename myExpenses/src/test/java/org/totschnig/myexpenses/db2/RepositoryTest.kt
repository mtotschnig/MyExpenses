package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.sync.json.CategoryInfo
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.viewmodel.data.Category

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val repository: Repository
        get() = Repository(
            ApplicationProvider.getApplicationContext<MyApplication>(),
            Mockito.mock(CurrencyContext::class.java),
            Mockito.mock(CurrencyFormatter::class.java),
            Mockito.mock(PrefHandler::class.java)
        )

    @Test
    fun findCategory() {
        val labelUnderTest = " Main "
        val labelUnderTestStripped = "Main"
        assertThat(repository.findCategory(labelUnderTest, null)).isEqualTo(-1)
        assertThat(repository.findCategory(labelUnderTestStripped, null)).isEqualTo(-1)
        assertThat(repository.saveCategory(Category(label = labelUnderTest))).isNotNull()

        val testedId = repository.findCategory(labelUnderTest)
        val testedStrippedId = repository.findCategory(labelUnderTestStripped)
        assertThat(testedId).isGreaterThan(0)
        assertThat(testedStrippedId).isGreaterThan(0)
        assertThat(testedId).isEqualTo(testedStrippedId)
    }

    @Test
    fun saveCategoryHierarchy() {
        val parent = Category(label = "Main")
        val parentId = ContentUris.parseId(repository.saveCategory(parent)!!)
        val sub = Category(label = "Sub", parentId = parentId)
        val subId = ContentUris.parseId(repository.saveCategory(sub)!!)
        val subsub = Category(label = "SubSub", parentId = subId)
        repository.saveCategory(subsub)
        assertThat(repository.findCategory(parent.label)).isGreaterThan(0)
        assertThat(repository.findCategory(sub.label, parentId)).isGreaterThan(0)
        assertThat(repository.findCategory(subsub.label, subId)).isGreaterThan(0)
    }

    @Test
    fun transformMainToSub() {
        val cat1 = Category(label = "Main1")
        val catId1 = ContentUris.parseId(repository.saveCategory(cat1)!!)
        val cat2 = Category(label = "Main2")
        val catId2 = ContentUris.parseId(repository.saveCategory(cat2)!!)
        repository.moveCategory(catId2, catId1)
        with(repository.loadCategory(catId2)!!) {
            assertThat(parentId).isEqualTo(catId1)
            assertThat(color).isNull()
        }
    }

    @Test
    fun transformSubToMain() {
        val cat1 = Category(label = "Main1")
        val catId1 = ContentUris.parseId(repository.saveCategory(cat1)!!)
        val cat2 = Category(label = "Main2", parentId = catId1)
        val catId2 = ContentUris.parseId(repository.saveCategory(cat2)!!)
        repository.moveCategory(catId2, null)
        with(repository.loadCategory(catId2)!!) {
            assertThat(parentId).isNull()
            assertThat(color).isNotNull()
        }
    }

    @Test
    fun saveCategoryData() {
        val category = Category(label ="Main", icon = "food", color = Color.RED)
        val id = ContentUris.parseId(repository.saveCategory(category)!!)
        with(repository.loadCategory(id)!!) {
            assertThat(label).isEqualTo("Main")
            assertThat(icon).isEqualTo("food")
            assertThat(color).isEqualTo(Color.RED)
        }
        val sub = Category(label ="Sub", icon = "bread", parentId = id)
        val subId = ContentUris.parseId(repository.saveCategory(sub)!!)
        with(repository.loadCategory(subId)!!) {
            assertThat(label).isEqualTo("Sub")
            assertThat(icon).isEqualTo("bread")
            assertThat(color).isNull()
        }
    }

    @Test
    fun ensureCategoryNew() {
        val categoryInfo = CategoryInfo(label ="Main", icon = "food", uuid = "uuid", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo,null)
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
        val category = Category(label ="Main", icon = "food", uuid = "uuid")
        val existing = ContentUris.parseId(repository.saveCategory(category)!!)
        val categoryInfo = CategoryInfo(label ="Main", icon = "food", uuid = "uuid", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo,null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
    }

    @Test
    fun ensureCategoryUpdate() {
        val category = Category(label ="Main", icon = "food", uuid = "uuid")
        val existing = ContentUris.parseId(repository.saveCategory(category)!!)
        val categoryInfo = CategoryInfo(label ="New", icon = "apple", uuid = "uuid", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo,null)
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
        val category = Category(label ="Main", icon = "food", uuid = "uuid1")
        val existing = ContentUris.parseId(repository.saveCategory(category)!!)
        val categoryInfo = CategoryInfo(label ="Main", icon = "food", uuid = "uuid2", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo,null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid).isEqualTo("uuid1:uuid2")
        }
    }

    @Test
    fun ensureCategoryMultipleUuidsInDatabase() {
        val category = Category(label ="Main", icon = "food", uuid = "uuid1:uuid2")
        val existing = ContentUris.parseId(repository.saveCategory(category)!!)
        val categoryInfo = CategoryInfo(label ="Main", icon = "food", uuid = "uuid1", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo,null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid).isEqualTo("uuid1:uuid2")
        }
    }

    @Test
    fun ensureCategoryMultipleUuidsInUpdate() {
        val category = Category(label ="Main", icon = "food", uuid = "uuid1")
        val existing = ContentUris.parseId(repository.saveCategory(category)!!)
        val categoryInfo = CategoryInfo(label ="Main", icon = "food", uuid = "uuid1:uuid2", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo,null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid).isEqualTo("uuid1")
        }
    }
    @Test
    fun ensureCategoryMultipleUuidsInDatabaseAndUpdate() {
        val category = Category(label ="Main", icon = "food", uuid = "uuid2:uuid1")
        val existing = ContentUris.parseId(repository.saveCategory(category)!!)
        val categoryInfo = CategoryInfo(label ="Main", icon = "food", uuid = "uuid1:uuid2", color = Color.RED)
        val (id, created) = repository.ensureCategory(categoryInfo,null)
        assertThat(created).isFalse()
        assertThat(id).isEqualTo(existing)
        with(repository.loadCategory(existing)!!) {
            assertThat(uuid).isEqualTo("uuid2:uuid1")
        }
    }
}