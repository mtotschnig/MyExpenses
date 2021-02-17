package org.totschnig.myexpenses.model2

import androidx.annotation.Keep
import org.threeten.bp.LocalDate

@Keep
data class Transaction(
        val account: Long, val amount: Float, val date: LocalDate, val payee: String, val category: Long,
        val tags: Array<Long>, val comment: String, val method: Long, val number: String)