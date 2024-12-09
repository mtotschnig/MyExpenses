package org.totschnig.myexpenses.db2

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.viewmodel.data.Tag

@RunWith(AndroidJUnit4::class)
class RepositoryTagsTest : BaseTestWithRepository() {

    @Test
    fun transactionStoreAndRemoveTags() {
        val testAccountId = insertAccount("Test account")
        val transactionId = insertTransaction(testAccountId, 100).first
        val tagId = repository.writeTag("Good Tag")
        val controlTag = Tag(tagId, "Good Tag")
        repository.saveTagsForTransaction(listOf(controlTag), transactionId)
        assertThat(contentResolver.loadTagsForTransaction(transactionId))
            .containsExactly(controlTag)
        repository.saveTagsForTransaction(emptyList(), transactionId)
        assertThat(contentResolver.loadTagsForTransaction(transactionId)).isEmpty()
    }

}