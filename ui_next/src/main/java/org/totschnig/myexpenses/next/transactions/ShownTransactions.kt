package org.totschnig.myexpenses.next.transactions

import org.totschnig.myexpenses.next.core.model.ledger.TagInfo
import org.totschnig.myexpenses.next.toInfo
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction

/** The transaction as the features of the new UI know it */
fun Transaction2.toShown() = ShownTransaction(
    id = id,
    amount = displayAmount.amountMinor,
    currency = displayAmount.currencyUnit.toInfo(),
    categoryId = catId,
    categoryPath = categoryPath,
    payeeName = party?.name,
    tags = tagList.map { TagInfo(it.id, it.label, it.color) },
    isSplit = isSplit,
    isSplitPart = isSplitPart,
    isTransfer = isTransfer,
)
