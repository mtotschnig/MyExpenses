package org.totschnig.myexpenses.next.core.model.transactions

import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.model.ledger.TagInfo

/**
 * A transaction as a list of the app shows it, e.g. in its details, where features add their
 * sections. Unlike a [org.totschnig.myexpenses.next.ledger.LedgerTransaction] it is not part of an
 * analysis, but what the user sees right now.
 *
 * @param amount in minor units of [currency], debits are negative
 */
data class ShownTransaction(
    val id: Long,
    val amount: Long,
    val currency: CurrencyInfo,
    val categoryId: Long? = null,
    val categoryPath: String? = null,
    val payeeName: String? = null,
    val tags: List<TagInfo> = emptyList(),
    val isSplit: Boolean = false,
    val isSplitPart: Boolean = false,
    val isTransfer: Boolean = false,
) {
    /** A debit that is no transfer and no part of a split transaction, so a receipt can belong to it */
    val canHaveReceipt: Boolean
        get() = !isTransfer && !isSplitPart && amount < 0
}
