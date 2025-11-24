package org.totschnig.shared_test

import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadTransaction

fun Repository.findCategoryPath(vararg path: String) =
    path.fold(null as Long?) { parentId, segment ->
        if (parentId == -1L) -1L else findCategory(segment, parentId)
    }.takeIf { it != -1L }

fun Repository.assertTransaction(
    id: Long,
    expectedTransaction: TransactionData
) {
    val (expectedAccount, expectedAmount, expectedSplitParts, expectedCategory, expectedParty, expectedTags, expectedAttachments, expectedDebt, expectedMethod, expectedComment) =
        expectedTransaction

    val transaction = loadTransaction(id)
    val attachments = loadAttachments(id)

    with(transaction.data) {
        assertThat(amount).isEqualTo(expectedAmount)
        assertThat(accountId).isEqualTo(expectedAccount)
        assertThat(categoryId).isEqualTo(expectedCategory)
        assertThat(payeeId).isEqualTo(expectedParty)
        assertThat(debtId).isEqualTo(expectedDebt)
        assertThat(methodId).isEqualTo(expectedMethod)
        assertThat(comment).isEqualTo(expectedComment)
    }
    assertThat(transaction.data.tagList).containsExactlyElementsIn(expectedTags)
    assertThat(attachments).containsExactlyElementsIn(expectedAttachments)

    if (expectedSplitParts == null) {
        assertThat(transaction.splitParts).isNull()
    } else {
        val parts = transaction.splitParts!!
        assertThat(parts.size).isEqualTo(expectedSplitParts.size)
        // 2. Map the actual split parts into the same data structure as the expected parts.
        val actualSplitPartsAsInfo = parts.map { actualPart ->
            TransactionData(
                accountId = actualPart.data.accountId,
                amount = actualPart.data.amount,
                category = actualPart.data.categoryId,
                tags = actualPart.data.tagList,
                debtId = actualPart.data.debtId
            )
        }
        assertThat(actualSplitPartsAsInfo).containsExactlyElementsIn(expectedSplitParts)
    }
}