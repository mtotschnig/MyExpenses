package org.totschnig.webui

import androidx.annotation.Keep
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.toEpoch
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Keep
data class TransactionDTO(
    val id: Long?,
    val account: Long,
    val amount: Float,
    val amountFormatted: String,
    val date: LocalDate,
    val time: LocalTime?,
    val dateFormatted: String,
    val valueDate: LocalDate,
    val party: Long?,
    val partyName: String,
    val category: Long?,
    val categoryPath: String?,
    val tags: List<Long>,
    val tagLabels: List<String>,
    val comment: String,
    val method: Long?,
    val number: String,
    val transferPeer: Long? = null
) {
    fun toEntity(currencyUnit: CurrencyUnit)  = org.totschnig.myexpenses.db2.entities.Transaction(
        id = id ?: 0,
        comment = comment,
        date =  time?.let {
            LocalDateTime.of(date, time).toEpoch()
        } ?: date.toEpoch(),
        valueDate = valueDate.toEpoch(),
        amount = Money(currencyUnit, BigDecimal(amount.toString())).amountMinor,
        categoryId = category,
        accountId = account,
        payeeId = party,
        methodId = method,
        crStatus = CrStatus.UNRECONCILED,
        referenceNumber = number
    )

    companion object {
        fun fromEntity(
            entity: org.totschnig.myexpenses.db2.entities.Transaction,
            currencyUnit: CurrencyUnit,
            currencyFormatter: ICurrencyFormatter,
            dateFormat: DateFormat,
            payeeMap: Map<Long, String>,
            tagMap: Map<Long, String>
        ): TransactionDTO {
            val money = Money(currencyUnit, entity.amount)
            val dateTime = epoch2ZonedDateTime(entity.date)
            return TransactionDTO(
                id = entity.id,
                account = entity.accountId,
                amount = money.amountMajor.toFloat(),
                amountFormatted = currencyFormatter.formatMoney(money),
                date = dateTime.toLocalDate(),
                time = dateTime.toLocalTime(),
                dateFormatted = Utils.convDateTime(entity.date, dateFormat),
                valueDate = epoch2LocalDate(entity.valueDate),
                party = entity.payeeId,
                partyName = payeeMap[entity.payeeId] ?: "",
                category = entity.categoryId,
                tags = entity.tagList,
                tagLabels = entity.tagList.map { tagMap[it] ?: "" },
                comment = entity.comment ?: "",
                method = entity.methodId,
                number = entity.referenceNumber ?: "",
                categoryPath = entity.categoryPath
            )
        }
    }
}