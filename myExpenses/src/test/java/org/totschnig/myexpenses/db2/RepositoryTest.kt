package org.totschnig.myexpenses.db2

import android.content.ContentUris
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.viewmodel.data.Category

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val repository: Repository
        get() = Repository(
            ApplicationProvider.getApplicationContext<MyApplication>(),
            Mockito.mock(CurrencyContext::class.java)
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
    fun saveCategory() {
        val parent = Category(label = "Main")
        val parentId = ContentUris.parseId(repository.saveCategory(parent)!!)
        val sub = Category(label = "Sub", parentId = parentId)
        val subId = ContentUris.parseId(repository.saveCategory(sub)!!)
        val subsub = Category(label = "SubSub", parentId = subId)
        val subSubId = ContentUris.parseId(repository.saveCategory(subsub)!!)
        assertThat(repository.findCategory(parent.label)).isGreaterThan(0)
        assertThat(repository.findCategory(sub.label, parentId)).isGreaterThan(0)
        assertThat(repository.findCategory(subsub.label, subId)).isGreaterThan(0)
    }
}