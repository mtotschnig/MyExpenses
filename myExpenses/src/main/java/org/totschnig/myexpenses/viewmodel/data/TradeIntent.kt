package org.totschnig.myexpenses.viewmodel.data

import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal
import java.time.LocalDateTime

enum class TradeType(@param:StringRes val label: Int) {
    BUY(R.string.trade_buy),
    SELL(R.string.trade_sell)
}

data class TradeIntent(
    val type: TradeType = TradeType.BUY,
    val date: LocalDateTime = LocalDateTime.now(),

    // The Asset being acquired or disposed of (e.g., AAPL, BTC)
    val targetAsset: Currency? = null,
    val quantity: BigDecimal = BigDecimal.ZERO,

    // The Price per unit in the Funding Currency (or Source Asset if Swap)
    val price: BigDecimal = BigDecimal.ZERO,

    // Where the value comes from or goes to
    val fundingAccountId: Long? = null, // Used for BUY/SELL
    val sourceAsset: Currency? = null, // Used for SWAP

    val fee: BigDecimal = BigDecimal.ZERO,
    val feeAsset: CurrencyUnit? = null, // Usually the Portfolio's reporting currency

    val comment: String = "",
    val payeeId: Long? = null,
)