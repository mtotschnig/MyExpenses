package org.totschnig.myexpenses.model

import java.math.BigDecimal
import java.time.ZonedDateTime

data class TransactionDTO(
    val uuid: String,
    val date: ZonedDateTime,
    val payee: String?,
    val amount: BigDecimal,
    val catId: Long?,
    val transferAccount: String?,
    val comment: String?,
    val methodLabel: String?,
    val status: CrStatus?,
    val referenceNumber: String?,
    val pictureFileName: String?,
    val tagList: List<String>?,
    val splits: List<TransactionDTO>?
) {

    fun fullLabel(categoryPaths: Map<Long, List<String>>) =
        transferAccount?.let { "[$it]" } ?: categoryPath(categoryPaths)

    fun categoryPath(categoryPaths: Map<Long, List<String>>) = catId?.let { cat ->
        categoryPaths[cat]?.joinToString(":") { label ->
            label.replace("/","\\u002F").replace(":","\\u003A")
        }
    }
}