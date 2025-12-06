package org.totschnig.shared_test

import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadTransaction
import kotlin.math.exp

fun Repository.findCategoryPath(vararg path: String) =
    path.fold(null as Long?) { parentId, segment ->
        if (parentId == -1L) -1L else findCategory(segment, parentId)
    }.takeIf { it != -1L }

fun Repository.assertTransaction(
    id: Long,
    expected: TransactionData
) {

    val transaction = loadTransaction(id)
    val attachments = loadAttachments(id)

    with(transaction.data) {
        assertThat(amount).isEqualTo(expected.amount)
        assertThat(accountId).isEqualTo(expected.accountId)
        assertThat(categoryId).isEqualTo(expected.category)
        assertThat(payeeId).isEqualTo(expected.party)
        assertThat(debtId).isEqualTo(expected.debtId)
        assertThat(methodId).isEqualTo(expected.methodId)
        assertThat(comment).isEqualTo(expected.comment)
        assertThat(transferAccountId).isEqualTo(expected.transferAccount)
        assertThat(transferPeerId).isEqualTo(expected.transferPeer)
    }
    assertThat(transaction.data.tagList).containsExactlyElementsIn(expected.tags)
    assertThat(attachments).containsExactlyElementsIn(expected.attachments)

    if (expected.splitParts == null) {
        assertThat(transaction.splitParts).isNull()
    } else {
        val parts = transaction.splitParts!!
        assertThat(parts.size).isEqualTo(expected.splitParts.size)
        val actualSplitPartsAsInfo = parts.map { actualPart ->
            TransactionData(
                accountId = actualPart.data.accountId,
                amount = actualPart.data.amount,
                category = actualPart.data.categoryId,
                tags = actualPart.data.tagList,
                debtId = actualPart.data.debtId,
                transferPeer = actualPart.data.transferPeerId,
                transferAccount = actualPart.data.transferAccountId
            )
        }
        assertThat(actualSplitPartsAsInfo).containsExactlyElementsIn(expected.splitParts)
    }
}