package org.totschnig.shared_test

import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.util.toEpoch

fun Repository.findCategoryPath(vararg path: String) =
    path.fold(null as Long?) { parentId, segment ->
        if (parentId == -1L) -1L else findCategory(segment, parentId)
    }.takeIf { it != -1L }

fun Repository.assertTransaction(
    id: Long,
    expected: TransactionData,
) {

    val transaction = loadTransaction(id)

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
        if (expected.date != null) {
            assertThat(date).isEqualTo(expected.date.toEpoch())
        }
        if (expected.valueDate != null) {
            assertThat(valueDate).isEqualTo(expected.valueDate.toEpoch())
        }

    }
    val attachments = loadAttachments(id)
    assertThat(attachments).containsExactlyElementsIn(expected.attachments)
    if (expected.transferAccount != null) {
        with(transaction.transferPeer!!) {
            assertThat(accountId).isEqualTo(expected.transferAccount)
            assertThat(this.id).isEqualTo(expected.transferPeer)
            assertThat(transferAccountId).isEqualTo(expected.accountId)
            assertThat(transferPeerId).isEqualTo(id)
            assertThat(amount).isEqualTo(expected.transferAmount)
        }
        val attachments = loadAttachments(expected.transferPeer!!)
        assertThat(attachments).containsExactlyElementsIn(expected.attachments)
    }
    assertThat(transaction.data.tagList).containsExactlyElementsIn(expected.tags)


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
                transferAccount = actualPart.data.transferAccountId,
                comment = actualPart.data.comment,
            )
        }
        assertThat(actualSplitPartsAsInfo).containsExactlyElementsIn(expected.splitParts)
    }
}