package org.totschnig.shared_test

import android.net.Uri
import org.totschnig.myexpenses.provider.SPLIT_CATID

data class TransactionData(
    val accountId: Long,
    val amount: Long,
    val splitParts: List<TransactionData>? = null,
    val category: Long? = if (splitParts != null) SPLIT_CATID else null,
    val party: Long? = null,
    val tags: List<Long> = emptyList(),
    val attachments: List<Uri> = emptyList(),
    val debtId: Long? = null,
    val methodId: Long? = null,
    val comment: String? = null,
    val transferAccount: Long? = null,
    val transferPeer: Long? = null
) {
    init {
        if (splitParts != null) {
            require(category == SPLIT_CATID)
        }
    }
}