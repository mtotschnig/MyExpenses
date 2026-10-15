package org.totschnig.shared_test

import android.net.Uri
import org.totschnig.myexpenses.provider.SPLIT_CATID
import java.time.LocalDate
import java.time.LocalDateTime

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
    val transferPeer: Long? = null,
    val _transferAmount: Long? = null,
    val date: LocalDateTime? = null,
    val valueDate: LocalDate? = null,
) {
    val transferAmount = _transferAmount ?: if (transferAccount != null) - amount else null
    init {
        if (splitParts != null) {
            require(category == SPLIT_CATID)
        }
    }
}