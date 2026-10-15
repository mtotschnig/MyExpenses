package org.totschnig.myexpenses.model

import java.math.BigDecimal
import java.time.ZonedDateTime

data class TransactionDTO(
    val uuid: String,
    val date: ZonedDateTime,
    val payee: String?,
    val amount: BigDecimal,
    val currency: String,
    val catId: Long?,
    val transferAccount: String?,
    val comment: String?,
    val methodLabel: String?,
    val status: CrStatus?,
    val referenceNumber: String?,
    val attachmentFileNames: List<String>?,
    val tagList: List<String>?,
    val splits: List<TransactionDTO>?,
    val equivalentAmount: BigDecimal? = null,
    val originalCurrency: String? = null,
    val originalAmount: BigDecimal? = null
)