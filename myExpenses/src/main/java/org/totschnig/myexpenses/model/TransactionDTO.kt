package org.totschnig.myexpenses.model

import com.google.gson.Gson
import java.math.BigDecimal

data class TransactionDTO(
    val id: String?,
    val isSplit: Boolean?,
    val dateStr: String?,
    val payee: String?,
    val amount: BigDecimal?,
    val labelMain: String?,
    val labelSub: String?,
    val fullLabel: String?,
    val comment: String?,
    val methodLabel: String?,
    val status: CrStatus?,
    val referenceNumber: String?,
    val pictureFileName: String?,
    val tagList: String?
) {

    fun toJson(gson: Gson): String = gson.toJson(this)
}