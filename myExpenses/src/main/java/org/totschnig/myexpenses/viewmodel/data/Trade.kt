package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.model.Money
import java.math.BigDecimal
import java.time.ZonedDateTime

data class Trade(
    val id: Long,
    val type: TradeType,
    val date: ZonedDateTime,
    val quantity: Money,
    val principal: Money,
    val fee: Money?,
    val assetSymbol: String,
    val comment: String?,
    val price: BigDecimal,
    val fundingAccount: Pair<Long, String>?,
    val currency: String // Portfolio currency
)
