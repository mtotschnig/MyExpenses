package org.totschnig.myexpenses.db2

import android.content.ContentUris
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.provider.AccountInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Tag

@RunWith(AndroidJUnit4::class)
class RepositoryTagsTest : BaseTestWithRepository() {

    @Test
    fun transactionStoreAndRemoveTags() {
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0, "USD")
        val testAccountId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                testAccount.contentValues
            )!!
        )
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