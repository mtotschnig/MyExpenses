package org.totschnig.myexpenses.provider.filter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.KEY_ROWID

/**
 * Restricts a transaction list to a fixed set of transactions, e.g. the debits a contract was
 * detected from. Not offered in the filter dialog, it is only set programmatically.
 */
@Parcelize
@Serializable
@SerialName(KEY_ROWID)
data class TransactionIdCriterion(
    override val label: String,
    override val values: List<Long>
) : IdCriterion() {

    @IgnoredOnParcel
    override val id = R.id.FILTER_TRANSACTION_ID_COMMAND
    @IgnoredOnParcel
    override val column = KEY_ROWID

    override val displayInfo: DisplayInfo
        get() = TransactionIdCriterion

    override val shouldApplyToSplitTransactions get() = false

    override val shouldApplyToArchive get() = false

    companion object : DisplayInfo {
        override val title = R.string.transaction
        override val extendedTitle = R.string.transaction
        override val icon = Icons.AutoMirrored.Filled.ReceiptLong
        override val clazz = TransactionIdCriterion::class
    }
}
